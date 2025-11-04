/*
 * Copyright 2019 The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.recordbuilder.processor;

import com.palantir.javapoet.TypeName;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.stream.Stream;

record NestedBuilder(Optional<RecordBuilder.Options> builderOptions) {
    private static final NestedBuilder NONE = new NestedBuilder(Optional.empty());

    private static final TypeName recordBuilderType = TypeName.get(RecordBuilder.class);
    private static final TypeName recordBuilderTemplateType = TypeName.get(RecordBuilder.Template.class);

    static NestedBuilder build(ProcessingEnvironment processingEnv, RecordBuilder.Options metaData,
            RecordClassType component) {
        if (!metaData.detectNestedRecordBuilders()) {
            return NONE;
        }

        if (component.typeKind() != TypeKind.DECLARED) {
            return NONE;
        }

        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(component.rawTypeName().toString());
        if ((typeElement == null) || (typeElement.asType() == null)) {
            return NONE;
        }

        TypeMirror recordBuilderMirror = processingEnv.getElementUtils().getTypeElement(recordBuilderType.toString())
                .asType();
        TypeMirror recordBuilderTemplateMirror = processingEnv.getElementUtils()
                .getTypeElement(recordBuilderTemplateType.toString()).asType();

        Optional<RecordBuilder.Options> maybeOptions = typeElement.getAnnotationMirrors().stream()
                .flatMap(annotation -> {
                    Optional<? extends AnnotationMirror> annotationMirror = ElementUtils.findAnnotationMirror(
                            processingEnv, annotation.getAnnotationType().asElement(),
                            recordBuilderTemplateType.toString());
                    if (annotationMirror.isPresent()) {
                        RecordBuilder.Options newOptions = ElementUtils.getMetaData(processingEnv,
                                annotation.getAnnotationType().asElement());
                        return Stream.of(newOptions);
                    }
                    return Stream.empty();
                }).findFirst();

        if (maybeOptions.isEmpty()) {
            maybeOptions = processingEnv.getElementUtils().getAllAnnotationMirrors(typeElement).stream()
                    .flatMap(annotationMirror -> {
                        if (processingEnv.getTypeUtils().isSameType(annotationMirror.getAnnotationType(),
                                recordBuilderMirror)) {
                            return Stream.of(ElementUtils.getMetaData(processingEnv, typeElement));
                        }
                        if (processingEnv.getTypeUtils().isSameType(annotationMirror.getAnnotationType(),
                                recordBuilderTemplateMirror)) {
                            RecordBuilder.Options options = recordBuilderTemplateMirror
                                    .getAnnotation(RecordBuilder.Options.class);
                            return Stream.of(options);
                        }
                        return Stream.empty();
                    }).findFirst();
        }
        if (maybeOptions.isEmpty()) {
            return NONE;
        }

        RecordBuilder.Options options = maybeOptions.get();
        return new NestedBuilder(Optional.of(options));
    }
}
