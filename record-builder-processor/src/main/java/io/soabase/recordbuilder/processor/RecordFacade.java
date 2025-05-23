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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.soabase.recordbuilder.processor.ElementUtils.generateName;

record RecordFacade(Element element, String packageName, ClassType recordClassType, ClassType builderClassType,
        List<TypeVariableName> typeVariables, List<RecordClassType> recordComponents,
        Map<String, CodeBlock> initializers, Set<Modifier> modifiers, boolean builderIsInRecordPackage) {
    public static RecordFacade fromTypeElement(ProcessingEnvironment processingEnv, TypeElement record,
            Optional<String> packageNameOpt, RecordBuilder.Options metaData) {
        String recordActualPackage = ElementUtils.getPackageName(record);
        String packageName = packageNameOpt.orElse(recordActualPackage);
        ClassType recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        ClassType builderClassType = ElementUtils.getClassType(packageName,
                generateName(record, recordClassType, metaData.suffix(), metaData.prefixEnclosingClassNames()),
                record.getTypeParameters());
        List<TypeVariableName> typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get)
                .collect(Collectors.toList());
        List<RecordClassType> recordComponents = buildRecordComponents(processingEnv, record);
        Map<String, CodeBlock> initializers = InitializerUtil.detectInitializers(processingEnv, record);

        return new RecordFacade(record, packageName, recordClassType, builderClassType, typeVariables, recordComponents,
                initializers, record.getModifiers(), recordActualPackage.equals(packageName));
    }

    private static List<RecordClassType> buildRecordComponents(ProcessingEnvironment processingEnv,
            TypeElement record) {
        var accessorAnnotations = record.getRecordComponents().stream().map(e -> (e.getAccessor() != null) ? e.getAccessor().getAnnotationMirrors() : List.<AnnotationMirror>of()).collect(Collectors.toList());
        var canonicalConstructorAnnotations = ElementUtils.findCanonicalConstructor(record)
                .map(constructor -> ((ExecutableElement) constructor).getParameters().stream()
                        .map(Element::getAnnotationMirrors).collect(Collectors.toList()))
                .orElse(List.of());
        var recordComponents = record.getRecordComponents();
        return IntStream.range(0, recordComponents.size()).mapToObj(index -> {
            var thisAccessorAnnotations = (accessorAnnotations.size() > index) ? accessorAnnotations.get(index)
                    : List.<AnnotationMirror> of();
            var thisCanonicalConstructorAnnotations = (canonicalConstructorAnnotations.size() > index)
                    ? canonicalConstructorAnnotations.get(index) : List.<AnnotationMirror> of();
            return ElementUtils.getRecordClassType(processingEnv, recordComponents.get(index), thisAccessorAnnotations,
                    thisCanonicalConstructorAnnotations);
        }).collect(Collectors.toList());
    }
}
