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
import io.soabase.recordbuilder.core.IgnoreDefaultMethod;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

import static io.soabase.recordbuilder.processor.ElementUtils.generateName;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordInterfaceAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.recordBuilderGeneratedAnnotation;

class InternalRecordInterfaceProcessor {
    private final ProcessingEnvironment processingEnv;
    private final String packageName;
    private final TypeSpec recordType;
    private final List<Component> recordComponents;
    private final ClassType recordClassType;

    private static final Set<String> javaBeanPrefixes = Set.of("get", "is");

    private record Component(ExecutableElement element, Optional<String> alternateName) {
    }

    InternalRecordInterfaceProcessor(ProcessingEnvironment processingEnv, TypeElement iface, boolean addRecordBuilder,
            RecordBuilder.Options metaData, Optional<String> packageNameOpt, boolean fromTemplate) {
        this.processingEnv = processingEnv;
        packageName = packageNameOpt.orElseGet(() -> ElementUtils.getPackageName(iface));
        recordComponents = getRecordComponents(iface);

        ClassType ifaceClassType = ElementUtils.getClassType(iface, iface.getTypeParameters());
        recordClassType = ElementUtils.getClassType(packageName,
                generateName(iface, ifaceClassType, metaData.interfaceSuffix(), metaData.prefixEnclosingClassNames()),
                iface.getTypeParameters());
        List<TypeVariableName> typeVariables = iface.getTypeParameters().stream().map(TypeVariableName::get)
                .collect(Collectors.toList());

        TypeSpec.Builder builder = TypeSpec.recordBuilder(recordClassType.name()).addSuperinterface(iface.asType())
                .addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            builder.addAnnotation(recordBuilderGeneratedAnnotation);
        }

        var actualPackage = ElementUtils.getPackageName(iface);
        addVisibility(builder, actualPackage.equals(packageName), iface.getModifiers());

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        recordComponents.forEach(component -> {
            String name = component.alternateName.orElseGet(() -> component.element.getSimpleName().toString());
            ParameterSpec parameterSpec = ParameterSpec.builder(ClassName.get(component.element.getReturnType()), name)
                    .build();
            constructorBuilder.addParameter(parameterSpec);
            builder.addTypeVariables(component.element.getTypeParameters().stream().map(TypeVariableName::get)
                    .collect(Collectors.toList()));
        });
        builder.recordConstructor(constructorBuilder.build());

        if (addRecordBuilder) {
            ClassType builderClassType = ElementUtils.getClassType(packageName,
                    generateName(iface, recordClassType, metaData.suffix(), metaData.prefixEnclosingClassNames()) + "."
                            + metaData.withClassName(),
                    iface.getTypeParameters());
            builder.addAnnotation(RecordBuilder.class);
            builder.addSuperinterface(builderClassType.typeName());
            if (fromTemplate) {
                builder.addAnnotation(AnnotationSpec.get(metaData));
            } else {
                var options = iface.getAnnotation(RecordBuilder.Options.class);
                if (options != null) {
                    builder.addAnnotation(AnnotationSpec.get(options));
                }
            }
        }

        addAlternateMethods(builder, recordComponents);

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

    private void addAlternateMethods(TypeSpec.Builder builder, List<Component> recordComponents) {
        recordComponents.stream().filter(component -> component.alternateName.isPresent()).forEach(component -> {
            var method = MethodSpec.methodBuilder(component.element.getSimpleName().toString())
                    .addAnnotation(Override.class).returns(ClassName.get(component.element.getReturnType()))
                    .addModifiers(Modifier.PUBLIC).addCode("return $L();", component.alternateName.get()).build();
            builder.addMethod(method);
        });
    }

    private List<Component> getRecordComponents(TypeElement iface) {
        List<Component> components = new ArrayList<>();
        try {
            getRecordComponents(iface, components, new HashSet<>(), new HashSet<>());
            if (components.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Annotated interface has no component methods", iface);
            }
        } catch (IllegalInterface e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), iface);
            components = Collections.emptyList();
        }
        return components;
    }

    private static class IllegalInterface extends RuntimeException {
        public IllegalInterface(String message) {
            super(message);
        }

    }

    private void getRecordComponents(TypeElement iface, Collection<Component> components, Set<String> visitedSet,
            Set<String> usedNames) {
        if (!visitedSet.add(iface.getQualifiedName().toString())) {
            return;
        }

        iface.getEnclosedElements().stream()
                .filter(element -> (element.getKind() == ElementKind.METHOD)
                        && !(element.getModifiers().contains(Modifier.STATIC)))
                .map(element -> ((ExecutableElement) element)).filter(element -> {
                    if (element.isDefault()) {
                        return element.getAnnotation(IgnoreDefaultMethod.class) == null;
                    }
                    return true;
                }).peek(element -> {
                    if (!element.getParameters().isEmpty() || element.getReturnType().getKind() == TypeKind.VOID) {
                        throw new IllegalInterface(String.format(
                                "Non-static, non-default methods must take no arguments and must return a value. Bad method: %s.%s()",
                                iface.getSimpleName(), element.getSimpleName()));
                    }
                    if (!element.getTypeParameters().isEmpty()) {
                        throw new IllegalInterface(
                                String.format("Interface methods cannot have type parameters. Bad method: %s.%s()",
                                        iface.getSimpleName(), element.getSimpleName()));
                    }
                }).filter(element -> usedNames.add(element.getSimpleName().toString()))
                .map(element -> new Component(element, stripBeanPrefix(element.getSimpleName().toString())))
                .collect(Collectors.toCollection(() -> components));
        iface.getInterfaces().forEach(parentIface -> {
            TypeElement parentIfaceElement = (TypeElement) processingEnv.getTypeUtils().asElement(parentIface);
            getRecordComponents(parentIfaceElement, components, visitedSet, usedNames);
        });
    }

    private Optional<String> stripBeanPrefix(String name) {
        return javaBeanPrefixes.stream().filter(prefix -> name.startsWith(prefix) && (name.length() > prefix.length()))
                .findFirst().map(prefix -> {
                    var stripped = name.substring(prefix.length());
                    return Character.toLowerCase(stripped.charAt(0)) + stripped.substring(1);
                });
    }
}
