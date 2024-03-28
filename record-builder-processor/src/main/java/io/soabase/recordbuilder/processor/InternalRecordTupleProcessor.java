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
import io.soabase.recordbuilder.core.RecordBuilder;
import io.soabase.recordbuilder.core.RecordTuple;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.*;

class InternalRecordTupleProcessor {
    private final ProcessingEnvironment processingEnv;
    private final String packageName;
    private final TypeSpec recordType;
    private final List<Component> recordComponents;
    private final ClassType recordClassType;
    private final List<TypeVariableName> typeVariables;

    private record Component(ExecutableElement element, Optional<String> alternateName) {
    }

    InternalRecordTupleProcessor(ProcessingEnvironment processingEnv, TypeElement element,
            RecordBuilder.Options metaData, Optional<String> packageNameOpt, boolean fromTemplate) {
        this.processingEnv = processingEnv;
        packageName = packageNameOpt.orElseGet(() -> ElementUtils.getPackageName(element));
        recordComponents = getRecordComponents(element);

        ClassType ifaceClassType = ElementUtils.getClassType(element, element.getTypeParameters());
        recordClassType = ElementUtils.getClassType(packageName,
                getBuilderName(element, metaData, ifaceClassType, metaData.tupleSuffix()), element.getTypeParameters());
        typeVariables = element.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());

        TypeSpec.Builder builder = TypeSpec.recordBuilder(recordClassType.name()).addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            builder.addAnnotation(generatedRecordTupleAnnotation);
        }

        var actualPackage = ElementUtils.getPackageName(element);
        addVisibility(builder, actualPackage.equals(packageName), element.getModifiers());

        recordComponents.forEach(component -> {
            String name = component.alternateName.orElseGet(() -> component.element.getSimpleName().toString());
            FieldSpec parameterSpec = FieldSpec.builder(ClassName.get(component.element.getReturnType()), name).build();
            builder.addTypeVariables(component.element.getTypeParameters().stream().map(TypeVariableName::get)
                    .collect(Collectors.toList()));
            builder.addField(parameterSpec);
        });

        addFromMethod(builder, element, metaData.fromMethodName());

        recordType = builder.build();
    }

    boolean isValid() {
        return !recordComponents.isEmpty();
    }

    TypeSpec recordType() {
        return recordType;
    }

    String packageName() {
        return packageName;
    }

    ClassType recordClassType() {
        return recordClassType;
    }

    private void addFromMethod(TypeSpec.Builder builder, TypeElement element, String fromName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(fromName)
                .addAnnotation(generatedRecordTupleAnnotation).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(recordClassType.typeName()).addTypeVariables(typeVariables)
                .addParameter(ClassName.get(element.asType()), fromName);

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("return new $T(", recordClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }

            Component component = recordComponents.get(index);
            codeBuilder.add("$L.$L()", fromName, component.element.getSimpleName());
        });
        codeBuilder.addStatement(")");

        methodBuilder.addCode(codeBuilder.build());

        builder.addMethod(methodBuilder.build());
    }

    private void addVisibility(TypeSpec.Builder builder, boolean builderIsInRecordPackage, Set<Modifier> modifiers) {
        if (builderIsInRecordPackage) {
            if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PRIVATE)
                    || modifiers.contains(Modifier.PROTECTED)) {
                builder.addModifiers(Modifier.PUBLIC); // builders are top level classes - can only be public or
                // package-private
            }
            // is package-private
        } else {
            builder.addModifiers(Modifier.PUBLIC);
        }
    }

    private List<Component> getRecordComponents(TypeElement iface) {
        List<Component> components = new ArrayList<>();
        try {
            getRecordComponents(iface, components, new HashSet<>(), new HashSet<>());
            if (components.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Annotated interface has no component methods", iface);
            }
        } catch (IllegalTuple e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), iface);
            components = Collections.emptyList();
        }
        return components;
    }

    private static class IllegalTuple extends RuntimeException {
        public IllegalTuple(String message) {
            super(message);
        }
    }

    private void getRecordComponents(TypeElement iface, Collection<Component> components, Set<String> visitedSet,
            Set<String> usedNames) {
        if (!visitedSet.add(iface.getQualifiedName().toString())) {
            return;
        }

        iface.getEnclosedElements().forEach(element -> {
            RecordTuple.Component component = element.getAnnotation(RecordTuple.Component.class);

            if (component == null) {
                return;
            }

            if (element.getKind() != ElementKind.METHOD || element.getModifiers().contains(Modifier.STATIC)
                    || !element.getModifiers().contains(Modifier.PUBLIC)) {
                throw new IllegalTuple(
                        String.format("RecordTuple.Component must be public non-static methods. Bad method: %s.%s()",
                                iface.getSimpleName(), element.getSimpleName()));
            }

            ExecutableElement executableElement = (ExecutableElement) element;

            if (!executableElement.getParameters().isEmpty()
                    || executableElement.getReturnType().getKind() == TypeKind.VOID) {
                throw new IllegalTuple(String.format(
                        "RecordTuple.Component methods must take no arguments and must return a value. Bad method: %s.%s()",
                        iface.getSimpleName(), executableElement.getSimpleName()));
            }
            if (!executableElement.getTypeParameters().isEmpty()) {
                throw new IllegalTuple(
                        String.format("RecordTuple.Component methods cannot have type parameters. Bad method: %s.%s()",
                                iface.getSimpleName(), element.getSimpleName()));
            }

            if (usedNames.add(element.getSimpleName().toString())) {
                Optional<String> alternateName;
                if (component.value().isEmpty()) {
                    alternateName = ElementUtils.stripBeanPrefix(element.getSimpleName().toString());
                } else {
                    alternateName = Optional.of(component.value());
                }
                components.add(new Component(executableElement, alternateName));
            }
        });

        iface.getInterfaces().forEach(parentIface -> {
            TypeElement parentIfaceElement = (TypeElement) processingEnv.getTypeUtils().asElement(parentIface);
            getRecordComponents(parentIfaceElement, components, visitedSet, usedNames);
        });
    }

}
