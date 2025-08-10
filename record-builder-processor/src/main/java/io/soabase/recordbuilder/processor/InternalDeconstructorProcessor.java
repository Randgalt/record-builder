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

import com.palantir.javapoet.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.soabase.recordbuilder.core.RecordBuilder.BuilderMode;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.soabase.recordbuilder.processor.ElementUtils.generateName;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.recordBuilderGeneratedAnnotation;

class InternalDeconstructorProcessor {
    private final String packageName;
    private final List<TypeVariableName> typeVariables;
    private final List<RecordClassType> recordComponents;
    private final TypeSpec.Builder builder;
    private final ClassType recordClassType;
    private final ProcessingEnvironment processingEnv;
    private final ExecutableElement executableElement;
    private final TypeElement classElement;
    private final RecordBuilder.Options metaData;
    private final RecordBuilder.Deconstructor deconstructor;

    private static final Set<BuilderMode> STANDARD_BUILDER_MODES = Set.of(BuilderMode.STANDARD,
            BuilderMode.STANDARD_AND_STAGED, BuilderMode.STANDARD_AND_STAGED_REQUIRED_ONLY);

    private static final TypeName consumerType = TypeName.get(Consumer.class);
    private static final TypeName intConsumerType = TypeName.get(IntConsumer.class);
    private static final TypeName longConsumerType = TypeName.get(LongConsumer.class);
    private static final TypeName doubleConsumerType = TypeName.get(DoubleConsumer.class);

    private static final TypeName booleanType = TypeName.get(boolean.class);
    private static final TypeName intType = TypeName.get(int.class);
    private static final TypeName longType = TypeName.get(long.class);
    private static final TypeName doubleType = TypeName.get(double.class);

    InternalDeconstructorProcessor(ProcessingEnvironment processingEnv, ExecutableElement executableElement,
            RecordBuilder.Deconstructor deconstructor, RecordBuilder.Options metaData) {
        this.processingEnv = processingEnv;
        this.executableElement = executableElement;
        this.deconstructor = deconstructor;
        classElement = (TypeElement) executableElement.getEnclosingElement();
        this.metaData = metaData;
        packageName = ElementUtils.getPackageName(classElement);
        typeVariables = classElement.getTypeParameters().stream().map(TypeVariableName::get)
                .collect(Collectors.toList());
        recordComponents = buildRecordComponents(executableElement);
        recordClassType = ElementUtils.getClassType(packageName,
                generateName(classElement,
                        new ClassType(TypeName.get(classElement.asType()),
                                deconstructor.prefix().isEmpty() ? classElement.getSimpleName().toString()
                                        : deconstructor.prefix()),
                        deconstructor.suffix(), metaData.prefixEnclosingClassNames()),
                classElement.getTypeParameters());

        builder = TypeSpec.recordBuilder(recordClassType.name()).addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables);

        if (metaData.addClassRetainedGenerated()) {
            builder.addAnnotation(recordBuilderGeneratedAnnotation);
        }

        addVisibility(executableElement.getModifiers());
        addRecordComponents();
        addDeconstructorMethod();

        if (!executableElement.getTypeParameters().isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Deconstructor methods cannot have type parameters", executableElement);
        }
    }

    String packageName() {
        return packageName;
    }

    ClassType recordClassType() {
        return recordClassType;
    }

    List<TypeVariableName> typeVariables() {
        return typeVariables;
    }

    List<RecordClassType> recordComponents() {
        return recordComponents;
    }

    TypeSpec.Builder builder() {
        return builder;
    }

    private void addVisibility(Set<Modifier> modifiers) {
        if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PRIVATE)
                || modifiers.contains(Modifier.PROTECTED)) {
            builder.addModifiers(Modifier.PUBLIC); // builders are top level classes - can only be public or
            // package-private
        }
        // is package-private
    }

    private List<RecordClassType> buildRecordComponents(ExecutableElement executableElement) {
        if (executableElement.getParameters().isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Deconstructor has no parameters",
                    executableElement);
            return List.of();
        }

        return executableElement.getParameters().stream().map(parameter -> {
            ValidatedParameter validatedParameter = validateParameter(parameter.getSimpleName().toString(),
                    parameter.asType());
            return new RecordClassType(validatedParameter.typeName, validatedParameter.rawTypeName,
                    parameter.getSimpleName().toString(), parameter.getAnnotationMirrors(), List.of());
        }).toList();
    }

    private record ValidatedParameter(TypeName typeName, TypeName rawTypeName) {
    }

    private ValidatedParameter validateParameter(String name, TypeMirror typeMirror) {
        TypeName rawTypeName = TypeName.get(processingEnv.getTypeUtils().erasure(typeMirror));

        if (rawTypeName.equals(intConsumerType)) {
            return new ValidatedParameter(intType, intType);
        }

        if (rawTypeName.equals(longConsumerType)) {
            return new ValidatedParameter(longType, longType);
        }

        if (rawTypeName.equals(doubleConsumerType)) {
            return new ValidatedParameter(doubleType, doubleType);
        }

        if (rawTypeName.equals(consumerType)) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeMirror typeParameter = declaredType.getTypeArguments().get(0);
            if (typeParameter.getKind() == TypeKind.WILDCARD) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Deconstructor parameters cannot be wildcards: " + name, executableElement);
                return new ValidatedParameter(booleanType, booleanType); // any default here
            }
            return new ValidatedParameter(TypeName.get(typeParameter),
                    TypeName.get(processingEnv.getTypeUtils().erasure(typeParameter)));
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid deconstructor parameter type: " + name,
                executableElement);
        return new ValidatedParameter(booleanType, booleanType); // any default here
    }

    private void addRecordComponents() {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        recordComponents.forEach(component -> {
            ParameterSpec.Builder componentBuilder = ParameterSpec.builder(component.typeName(), component.name());
            if (deconstructor.inheritAnnotations()) {
                componentBuilder
                        .addAnnotations(component.getAccessorAnnotations().stream().map(AnnotationSpec::get).toList());
            }
            constructorBuilder.addParameter(componentBuilder.build());
        });
        builder.recordConstructor(constructorBuilder.build());
    }

    private void addDeconstructorMethod() {
        boolean canUseBuilder = deconstructor.addRecordBuilder()
                && STANDARD_BUILDER_MODES.contains(metaData.builderMode());
        String parameterName = uniqueName("rhs");
        String variableName = uniqueName(canUseBuilder ? "builder" : "components");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(deconstructor.deconstructorMethodName())
                .addAnnotation(generatedRecordBuilderAnnotation).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(typeVariables).returns(recordClassType.typeName())
                .addParameter(TypeName.get(classElement.asType()), parameterName);

        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();

        if (canUseBuilder) {
            addCodeWithBuilder(codeBlockBuilder, variableName, parameterName);
        } else {
            addCodeWithoutBuilder(codeBlockBuilder, variableName, parameterName);
        }

        methodBuilder.addCode(codeBlockBuilder.build());
        builder.addMethod(methodBuilder.build());
    }

    private void addCodeWithBuilder(CodeBlock.Builder codeBlockBuilder, String variableName, String parameterName) {
        ClassType builderClassType = ElementUtils.getClassType(packageName,
                generateName(classElement, recordClassType, metaData.suffix(), metaData.prefixEnclosingClassNames()),
                classElement.getTypeParameters());

        codeBlockBuilder.add("$T $L = $L.$L();\n", builderClassType.typeName(), variableName, builderClassType.name(),
                metaData.builderMethodName());

        codeBlockBuilder.add("$L.$L(", parameterName, executableElement.getSimpleName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            boolean isLast = index == (recordComponents.size() - 1);
            RecordClassType component = recordComponents.get(index);
            codeBlockBuilder.add("$L::$L", variableName, component.name());
            if (!isLast) {
                codeBlockBuilder.add(", ");
            }
        });
        codeBlockBuilder.add(");\n");

        codeBlockBuilder.add("return $L.$L();", variableName, metaData.buildMethodName());
    }

    private void addCodeWithoutBuilder(CodeBlock.Builder codeBlockBuilder, String variableName, String parameterName) {
        codeBlockBuilder.add("var $L = new Object() {\n", variableName);
        codeBlockBuilder.indent();
        recordComponents.forEach(component -> codeBlockBuilder.add("$T $L;\n", component.typeName(), component.name()));
        codeBlockBuilder.unindent();
        codeBlockBuilder.add("};\n");

        codeBlockBuilder.add("$L.$L(", parameterName, executableElement.getSimpleName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            boolean isLast = index == (recordComponents.size() - 1);
            RecordClassType component = recordComponents.get(index);
            codeBlockBuilder.add("$L -> $L.$L = $L", component.name(), variableName, component.name(),
                    component.name());
            if (!isLast) {
                codeBlockBuilder.add(", ");
            }
        });
        codeBlockBuilder.add(");\n");

        codeBlockBuilder.add("return new $T(", recordClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            boolean isLast = index == (recordComponents.size() - 1);
            RecordClassType component = recordComponents.get(index);
            codeBlockBuilder.add("$L.$L", variableName, component.name());
            if (!isLast) {
                codeBlockBuilder.add(", ");
            }
        });
        codeBlockBuilder.add(");\n");
    }

    private String uniqueName(String base) {
        return uniqueName("", base);
    }

    private String uniqueName(String prefix, String base) {
        var name = prefix + base;
        var alreadyExists = recordComponents.stream().map(ClassType::name).anyMatch(n -> n.equals(name));
        return alreadyExists ? uniqueName(prefix + "_", base) : name;
    }
}
