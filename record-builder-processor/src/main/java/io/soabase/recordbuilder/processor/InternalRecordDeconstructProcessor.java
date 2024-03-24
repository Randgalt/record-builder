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

import com.squareup.javapoet.*;
import io.soabase.recordbuilder.core.RecordBuilderDeconstruct;
import io.soabase.recordbuilder.wrappers.Option;
import io.soabase.recordbuilder.wrappers.Wrappers;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;
import static io.soabase.recordbuilder.processor.ProcessorCommon.addVisibility;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.*;

class InternalRecordDeconstructProcessor {
    private final String packageName;
    private final ClassType helperClassType;
    private final TypeSpec helperType;
    private final List<RecordClassType> recordComponents;
    private final TypeSpec.Builder builder;
    private final ClassType recordClassType;
    private final List<TypeElement> mapperClasses;
    private final ProcessingEnvironment processingEnv;

    private static final TypeName optionalType = TypeName.get(Optional.class);
    private static final ClassName optionClass = ClassName.get(Option.class);

    InternalRecordDeconstructProcessor(ProcessingEnvironment processingEnv, TypeElement record,
            RecordBuilderDeconstruct.Options metaData, Optional<String> packageNameOpt) {
        this.processingEnv = processingEnv;
        var recordActualPackage = ElementUtils.getPackageName(record);
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = packageNameOpt.orElse(recordActualPackage);
        helperClassType = ElementUtils.getClassType(packageName,
                getBuilderName(record, recordClassType, metaData.suffix(), metaData.prefixEnclosingClassNames()),
                record.getTypeParameters());
        List<TypeVariableName> typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get)
                .collect(Collectors.toList());
        recordComponents = ProcessorCommon.buildRecordComponents(processingEnv, record);

        Class<?>[] mapperClasses = (metaData.mapperClasses().length > 0) ? metaData.mapperClasses()
                : new Class[] { Wrappers.class };
        this.mapperClasses = Stream.of(mapperClasses)
                .map(mapperClass -> processingEnv.getElementUtils().getTypeElement(mapperClass.getName())).toList();

        builder = TypeSpec.recordBuilder(helperClassType.name()).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(metaData.builderClassModifiers()).addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            builder.addAnnotation(recordBuilderGeneratedAnnotation);
        }
        addVisibility(builder, recordActualPackage.equals(packageName), record.getModifiers());

        processComponents(metaData.fromMethodName());

        helperType = builder.build();
    }

    String packageName() {
        return packageName;
    }

    ClassType builderClassType() {
        return helperClassType;
    }

    TypeSpec builderType() {
        return helperType;
    }

    private void processComponents(String methodName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addJavadoc("Return a new deconstructor record from a source instance\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedRecordBuilderDeconstructAnnotation).returns(helperClassType.typeName());

        methodBuilder.addParameter(recordClassType.typeName(), methodName);

        CodeBlock.Builder codeBlock = CodeBlock.builder();

        codeBlock.add("return new $T(", helperClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            RecordClassType recordComponent = recordComponents.get(index);

            Optional<ExecutableElement> mapper = findMapper(recordComponent);

            if (index > 0) {
                codeBlock.add(", ");
            }

            TypeName typeName;
            if (mapper.isPresent()) {
                ExecutableElement mapperMethod = mapper.get();

                typeName = ClassName.get(mapperMethod.getReturnType());
                codeBlock.add("$T.$L($L.$L())", mapperMethod.getReturnType(), mapperMethod.getSimpleName(), methodName,
                        recordComponents.get(index).name());
            } else {
                typeName = recordComponent.typeName();
                codeBlock.add("$L.$L()", methodName, recordComponents.get(index).name());
            }

            builder.addField(typeName, recordComponent.name());
        });
        codeBlock.addStatement(")");

        methodBuilder.addCode(codeBlock.build());

        builder.addMethod(methodBuilder.build());
    }

    private Optional<ExecutableElement> findMapper(RecordClassType recordComponent) {
        return mapperClasses.stream().flatMap(mapperClass -> mapperClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD).map(element -> (ExecutableElement) element)
                .filter(executableElement -> executableElement.getModifiers().contains(Modifier.PUBLIC)
                        && executableElement.getModifiers().contains(Modifier.STATIC))
                .filter(executableElement -> executableElement.getParameters().size() == 1)
                .flatMap(executableElement -> {
                    TypeMirror parameterType = executableElement.getParameters().get(0).asType();
                    if (processingEnv.getTypeUtils().contains(recordComponent.recordComponent().asType(),
                            parameterType)) {
                        return Stream.of(executableElement);
                    }
                    return Stream.empty();
                })).findFirst();
    }
}
