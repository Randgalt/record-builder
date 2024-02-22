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
import io.soabase.recordbuilder.core.RecordBuilder.BuilderMode;
import io.soabase.recordbuilder.processor.CollectionBuilderUtils.SingleItemsMetaData;
import io.soabase.recordbuilder.processor.options.InternalBuilderOptions;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.soabase.recordbuilder.processor.CollectionBuilderUtils.SingleItemsMetaDataMode.EXCLUDE_WILDCARD_TYPES;
import static io.soabase.recordbuilder.processor.CollectionBuilderUtils.SingleItemsMetaDataMode.STANDARD_FOR_SETTER;
import static io.soabase.recordbuilder.processor.ElementUtils.*;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.recordBuilderGeneratedAnnotation;

class InternalRecordBuilderProcessor {
    private final InternalBuilderOptions metaData;
    private final ClassType recordClassType;
    private final String packageName;
    private final ClassType builderClassType;
    private final List<TypeVariableName> typeVariables;
    private final List<RecordClassType> recordComponents;
    private final TypeSpec builderType;
    private final TypeSpec.Builder builder;
    private final String uniqueVarName;
    private final Pattern notNullPattern;
    private final CollectionBuilderUtils collectionBuilderUtils;

    private static final TypeName overrideType = TypeName.get(Override.class);
    private static final TypeName validType = ClassName.get("javax.validation", "Valid");
    private static final TypeName validatorTypeName = ClassName.get("io.soabase.recordbuilder.validator",
            "RecordBuilderValidator");
    private static final TypeVariableName rType = TypeVariableName.get("R");
    private final ProcessingEnvironment processingEnv;
    private final Modifier constructorVisibilityModifier;
    private final Map<String, CodeBlock> initializers;

    InternalRecordBuilderProcessor(ProcessingEnvironment processingEnv, TypeElement record, InternalBuilderOptions metaData,
            Optional<String> packageNameOpt) {
        this.processingEnv = processingEnv;
        var recordActualPackage = ElementUtils.getPackageName(record);
        this.metaData = metaData;
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = packageNameOpt.orElse(recordActualPackage);
        builderClassType = ElementUtils.getClassType(packageName,
                getBuilderName(record, metaData, recordClassType, metaData.suffix()), record.getTypeParameters());
        typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        recordComponents = buildRecordComponents(record);
        uniqueVarName = getUniqueVarName();
        notNullPattern = Pattern.compile(metaData.interpretNotNullsPattern());
        collectionBuilderUtils = new CollectionBuilderUtils(recordComponents, this.metaData);
        constructorVisibilityModifier = metaData.publicBuilderConstructors() ? Modifier.PUBLIC : Modifier.PRIVATE;
        initializers = InitializerUtil.detectInitializers(processingEnv, record);

        builder = TypeSpec.classBuilder(builderClassType.name()).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(metaData.builderClassModifiers()).addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            builder.addAnnotation(recordBuilderGeneratedAnnotation);
        }
        addVisibility(recordActualPackage.equals(packageName), record.getModifiers());
        if (metaData.enableWither()) {
            addWithNestedClass();
        }
        if (!metaData.beanClassName().isEmpty()) {
            addBeanNestedClass();
        }
        if (metaData.builderMode() != BuilderMode.STANDARD) {
            addStagedBuilderClasses();
            addStaticStagedBuilderMethod((metaData.builderMode() == BuilderMode.STANDARD_AND_STAGED)
                    ? metaData.stagedBuilderMethodName() : metaData.builderMethodName());
        }
        addDefaultConstructor();
        if (metaData.addStaticBuilder()) {
            addStaticBuilder();
        }
        if (recordComponents.size() > 0) {
            addAllArgsConstructor();
        }
        if (metaData.builderMode() != BuilderMode.STAGED) {
            addStaticDefaultBuilderMethod();
        }
        addStaticCopyBuilderMethod();
        if (metaData.enableWither()) {
            addStaticFromWithMethod();
        }
        if (metaData.onceOnlyAssignment()) {
            addOnceOnlySupport();
        }
        addStaticComponentsMethod();
        addBuildMethod();
        addToStringMethod();
        addHashCodeMethod();
        addEqualsMethod();
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            RecordClassType component = recordComponents.get(index);
            add1Field(component);
            add1SetterMethod(component, index);
            if (metaData.enableGetters()) {
                add1GetterMethod(component);
            }
            if (metaData.addConcreteSettersForOptional()) {
                add1ConcreteOptionalSetterMethod(component);
            }
            var collectionMetaData = collectionBuilderUtils.singleItemsMetaData(component, EXCLUDE_WILDCARD_TYPES);
            collectionMetaData.ifPresent(meta -> add1CollectionBuilders(meta, component));
        });
        collectionBuilderUtils.addShims(builder);
        collectionBuilderUtils.addMutableMakers(builder);
        builderType = builder.build();
    }

    String packageName() {
        return packageName;
    }

    ClassType builderClassType() {
        return builderClassType;
    }

    TypeSpec builderType() {
        return builderType;
    }

    private void addVisibility(boolean builderIsInRecordPackage, Set<Modifier> modifiers) {
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

    private List<RecordClassType> buildRecordComponents(TypeElement record) {
        var accessorAnnotations = record.getRecordComponents().stream().map(e -> e.getAccessor().getAnnotationMirrors())
                .collect(Collectors.toList());
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

    private void addOnceOnlySupport() {
        if (recordComponents.isEmpty()) {
            return;
        }

        // per https://www.baeldung.com/java-boolean-array-bitset-performance - simple boolean array is better for a
        // small number of "bits"

        /*
         * Adds an array to support once only assignment similar to:
         *
         * private final boolean[] _onceOnlyCheck = new boolean[<number of record components>];
         */
        FieldSpec onceOnlyField = FieldSpec
                .builder(boolean[].class, metaData.onceOnlyAssignmentName(), Modifier.PRIVATE, Modifier.FINAL)
                .initializer(CodeBlock.of("new boolean[$L]", recordComponents.size())).build();
        builder.addField(onceOnlyField);
    }

    private void addStagedBuilderClasses() {
        if (recordComponents.size() < 2) {
            return;
        }

        IntStream.range(0, recordComponents.size()).forEach(index -> {
            Optional<RecordClassType> nextComponent = ((index + 1) < recordComponents.size())
                    ? Optional.of(recordComponents.get(index + 1)) : Optional.empty();
            add1StagedBuilderClass(recordComponents.get(index), nextComponent);
        });

        /*
         * Adds the final builder stage that has the "build" methods similar to:
         *
         * public class BuilderStage { PersonBuilder builder();
         *
         * default Person build() { return builder().build(); }
         */
        var classBuilder = TypeSpec.interfaceBuilder(stagedBuilderName(builderClassType))
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add final staged builder to {@code $L}\n", recordClassType.name())
                .addModifiers(Modifier.PUBLIC).addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            classBuilder.addAnnotation(recordBuilderGeneratedAnnotation);
        }

        MethodSpec buildMethod = buildMethod().addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addStatement("return builder().build()").build();
        classBuilder.addMethod(buildMethod);

        var builderMethod = MethodSpec.methodBuilder(metaData.builderMethodName())
                .addJavadoc("Return a new builder with all fields set to the current values in this builder\n")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).addAnnotation(generatedRecordBuilderAnnotation)
                .returns(builderClassType.typeName()).build();
        classBuilder.addMethod(builderMethod);

        builder.addType(classBuilder.build());
    }

    private void add1StagedBuilderClass(RecordClassType component, Optional<RecordClassType> nextComponent) {
        /*
         * Adds a nested interface similar to:
         *
         * public class NameStage { AgeStage name(String name); }
         */
        var classBuilder = TypeSpec.interfaceBuilder(stagedBuilderName(component))
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add staged builder to {@code $L} for component {@code $L}\n", recordClassType.name(),
                        component.name())
                .addModifiers(Modifier.PUBLIC).addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            classBuilder.addAnnotation(recordBuilderGeneratedAnnotation);
        }

        var returnType = nextComponent.map(this::stagedBuilderType)
                .orElseGet(() -> stagedBuilderType(builderClassType));
        var methodSpec = MethodSpec.methodBuilder(prefixedName(component, false))
                .addAnnotation(generatedRecordBuilderAnnotation).returns(returnType.typeName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

        methodSpec.addJavadoc("Set a new value for the {@code $L} record component in the builder\n", component.name());
        var parameterSpecBuilder = ParameterSpec.builder(component.typeName(), component.name());
        addConstructorAnnotations(component, parameterSpecBuilder);
        methodSpec.addParameter(parameterSpecBuilder.build());

        classBuilder.addMethod(methodSpec.build());

        builder.addType(classBuilder.build());
    }

    private void addWithNestedClass() {
        /*
         * Adds a nested interface that adds withers similar to:
         *
         * public class MyRecordBuilder { public interface With { // with methods } }
         */
        var classBuilder = TypeSpec.interfaceBuilder(metaData.withClassName())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add withers to {@code $L}\n", recordClassType.name()).addModifiers(Modifier.PUBLIC)
                .addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            classBuilder.addAnnotation(recordBuilderGeneratedAnnotation);
        }
        recordComponents.forEach(component -> addNestedGetterMethod(classBuilder, component, component.name()));
        addWithBuilderMethod(classBuilder);
        addWithSuppliedBuilderMethod(classBuilder);
        IntStream.range(0, recordComponents.size())
                .forEach(index -> add1WithMethod(classBuilder, recordComponents.get(index), index));
        if (metaData.addFunctionalMethodsToWith()) {
            classBuilder.addType(buildFunctionalInterface("Function", true))
                    .addType(buildFunctionalInterface("Consumer", false))
                    .addMethod(buildFunctionalHandler("Function", "map", true))
                    .addMethod(buildFunctionalHandler("Consumer", "accept", false));
        }
        builder.addType(classBuilder.build());
    }

    private void addBeanNestedClass() {
        /*
         * Adds a nested interface that adds getters similar to:
         *
         * public class MyRecordBuilder { public interface Bean { // getter methods } }
         */
        var classBuilder = TypeSpec.interfaceBuilder(metaData.beanClassName())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add getters to {@code $L}\n", recordClassType.name()).addModifiers(Modifier.PUBLIC)
                .addTypeVariables(typeVariables);
        recordComponents.forEach(component -> {
            if (prefixedName(component, true).equals(component.name())) {
                return;
            }
            addNestedGetterMethod(classBuilder, component, component.name());
            add1PrefixedGetterMethod(classBuilder, component);
        });
        builder.addType(classBuilder.build());
    }

    private void addWithSuppliedBuilderMethod(TypeSpec.Builder classBuilder) {
        /*
         * Adds a method that returns a pre-filled copy builder similar to:
         *
         * default MyRecord with(Consumer<MyRecordBuilder> consumer) { MyRecordBuilder builder = with();
         * consumer.accept(builder); return builder.build(); }
         */
        var codeBlockBuilder = CodeBlock.builder().add("$T builder = with();\n", builderClassType.typeName())
                .add("consumer.accept(builder);\n").add("return builder.$L();\n", metaData.buildMethodName());
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), builderClassType.typeName());
        var parameter = ParameterSpec.builder(consumerType, "consumer").build();
        var methodSpec = MethodSpec.methodBuilder(metaData.withClassMethodPrefix())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new record built from the builder passed to the given consumer")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).addParameter(parameter)
                .returns(recordClassType.typeName()).addCode(codeBlockBuilder.build()).build();
        classBuilder.addMethod(methodSpec);
    }

    private void addWithBuilderMethod(TypeSpec.Builder classBuilder) {
        /*
         * Adds a method that returns a pre-filled copy builder similar to:
         *
         * default MyRecordBuilder with() { return MyRecordBuilder.builder(r); }
         */
        var codeBlockBuilder = CodeBlock.builder().add("return new $L$L(", builderClassType.name(),
                typeVariables.isEmpty() ? "" : "<>");
        addComponentCallsAsArguments(-1, codeBlockBuilder, false);
        codeBlockBuilder.add(");");
        var methodSpec = MethodSpec.methodBuilder(metaData.withClassMethodPrefix())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new record builder using the current values")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).returns(builderClassType.typeName())
                .addCode(codeBlockBuilder.build()).build();
        classBuilder.addMethod(methodSpec);
    }

    private String getUniqueVarName() {
        return getUniqueVarName("");
    }

    private String getUniqueVarName(String prefix) {
        var name = prefix + "r";
        var alreadyExists = recordComponents.stream().map(ClassType::name).anyMatch(n -> n.equals(name));
        return alreadyExists ? getUniqueVarName(prefix + "_") : name;
    }

    private void add1WithMethod(TypeSpec.Builder classBuilder, RecordClassType component, int index) {
        /*
         * Adds a with method for the component similar to:
         *
         * default MyRecord withName(String name) { return new MyRecord(name, r.age()); }
         */
        var codeBlockBuilder = CodeBlock.builder();
        addNullCheckCodeBlock(codeBlockBuilder, index);
        codeBlockBuilder.add("$[return ");
        if (metaData.useValidationApi()) {
            codeBlockBuilder.add("$T.validate(", validatorTypeName);
        }
        codeBlockBuilder.add("new $T(", recordClassType.typeName());
        addComponentCallsAsArguments(index, codeBlockBuilder, false);
        codeBlockBuilder.add(")");
        if (metaData.useValidationApi()) {
            codeBlockBuilder.add(")");
        }
        codeBlockBuilder.add(";$]");

        var methodName = getWithMethodName(component, metaData.withClassMethodPrefix());
        var parameterSpecBuilder = ParameterSpec.builder(component.typeName(), component.name());
        addConstructorAnnotations(component, parameterSpecBuilder);
        var methodSpec = MethodSpec.methodBuilder(methodName).addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new instance of {@code $L} with a new value for {@code $L}\n",
                        recordClassType.name(), component.name())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).addParameter(parameterSpecBuilder.build())
                .addCode(codeBlockBuilder.build()).returns(recordClassType.typeName()).build();
        classBuilder.addMethod(methodSpec);
    }

    private void add1PrefixedGetterMethod(TypeSpec.Builder classBuilder, RecordClassType component) {
        /*
         * Adds a get method for the component similar to:
         *
         * default MyRecord getName() { return name(); }
         */
        var codeBlockBuilder = CodeBlock.builder();
        codeBlockBuilder.add("$[return $L()$];", component.name());

        var methodName = prefixedName(component, true);
        var methodSpec = MethodSpec.methodBuilder(methodName).addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Returns the value of {@code $L}\n", component.name())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).addCode(codeBlockBuilder.build())
                .returns(component.typeName()).build();
        classBuilder.addMethod(methodSpec);
    }

    private void addComponentCallsAsArguments(int index, CodeBlock.Builder codeBlockBuilder, boolean usePrefixedName) {
        IntStream.range(0, recordComponents.size()).forEach(parameterIndex -> {
            if (parameterIndex > 0) {
                codeBlockBuilder.add(", ");
            }
            RecordClassType parameterComponent = recordComponents.get(parameterIndex);
            if (parameterIndex == index) {
                collectionBuilderUtils.addShimCall(codeBlockBuilder, parameterComponent);
            } else {
                codeBlockBuilder.add("$L()",
                        usePrefixedName ? prefixedName(parameterComponent, true) : parameterComponent.name());
            }
        });
    }

    private void addDefaultConstructor() {
        /*
         * Adds a default constructor similar to:
         *
         * private MyRecordBuilder() { }
         */
        var constructor = MethodSpec.constructorBuilder().addModifiers(constructorVisibilityModifier)
                .addAnnotation(generatedRecordBuilderAnnotation).build();
        builder.addMethod(constructor);
    }

    private void addStaticBuilder() {
        /*
         * Adds an static builder similar to:
         *
         * public static MyRecord(int p1, T p2, ...) { return new MyRecord(p1, p2, ...); }
         */
        CodeBlock codeBlock = buildCodeBlock();
        var builder = MethodSpec.methodBuilder(recordClassType.name())
                .addJavadoc("Static constructor/builder. Can be used instead of new $L(...)\n", recordClassType.name())
                .addTypeVariables(typeVariables).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedRecordBuilderAnnotation).returns(recordClassType.typeName()).addCode(codeBlock);
        recordComponents.forEach(component -> {
            var parameterSpecBuilder = ParameterSpec.builder(component.typeName(), component.name());
            addConstructorAnnotations(component, parameterSpecBuilder);
            builder.addParameter(parameterSpecBuilder.build());
        });
        this.builder.addMethod(builder.build());
    }

    private void addNullCheckCodeBlock(CodeBlock.Builder builder) {
        if (metaData.interpretNotNulls()) {
            for (int i = 0; i < recordComponents.size(); ++i) {
                addNullCheckCodeBlock(builder, i);
            }
        }
    }

    private void addNullCheckCodeBlock(CodeBlock.Builder builder, int index) {
        if (metaData.interpretNotNulls()) {
            var component = recordComponents.get(index);
            if (!collectionBuilderUtils.isImmutableCollection(component)) {
                if (!component.typeName().isPrimitive() && isNullAnnotated(component)) {
                    builder.addStatement("$T.requireNonNull($L, $S)", Objects.class, component.name(),
                            component.name() + " is required");
                }
            }
        }
    }

    private boolean isNullAnnotated(RecordClassType component) {
        return component.getCanonicalConstructorAnnotations().stream().anyMatch(annotation -> notNullPattern
                .matcher(annotation.getAnnotationType().asElement().getSimpleName().toString()).matches());
    }

    private void addAllArgsConstructor() {
        /*
         * Adds an all-args constructor similar to:
         *
         * private MyRecordBuilder(int p1, T p2, ...) { this.p1 = p1; this.p2 = p2; ... }
         */
        var constructorBuilder = MethodSpec.constructorBuilder().addModifiers(constructorVisibilityModifier)
                .addAnnotation(generatedRecordBuilderAnnotation);
        recordComponents.forEach(component -> {
            constructorBuilder.addParameter(component.typeName(), component.name());
            constructorBuilder.addStatement("this.$L = $L", component.name(), component.name());
        });
        builder.addMethod(constructorBuilder.build());
    }

    private void addToStringMethod() {
        /*
         * add a toString() method similar to:
         *
         * @Override public String toString() { return "MyRecord[p1=blah, p2=blah]"; }
         */
        var codeBuilder = CodeBlock.builder().add("return \"$L[", builderClassType.name());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }
            String name = recordComponents.get(index).name();
            codeBuilder.add("$L=\" + $L + \"", name, name);
        });
        codeBuilder.add("]\"");

        var methodSpec = MethodSpec.methodBuilder("toString").addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation).addAnnotation(Override.class).returns(String.class)
                .addStatement(codeBuilder.build()).build();
        builder.addMethod(methodSpec);
    }

    private void addHashCodeMethod() {
        /*
         * add a hashCode() method similar to:
         *
         * @Override public int hashCode() { return Objects.hash(p1, p2); }
         */
        var codeBuilder = CodeBlock.builder().add("return $T.hash(", Objects.class);
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }
            codeBuilder.add("$L", recordComponents.get(index).name());
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder("hashCode").addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation).addAnnotation(Override.class).returns(TypeName.INT)
                .addStatement(codeBuilder.build()).build();
        builder.addMethod(methodSpec);
    }

    private void addEqualsMethod() {
        /*
         * add an equals() method similar to:
         *
         * @Override public boolean equals(Object o) { if (this == o) || ((o instanceof MyRecordBuilder b) &&
         * Objects.equals(p1, b.p1) && Objects.equals(p2, b.p2)); }
         */
        var codeBuilder = CodeBlock.builder();
        codeBuilder.add("return (this == o) || (");
        if (typeVariables.isEmpty()) {
            codeBuilder.add("(o instanceof $L $L)", builderClassType.name(), uniqueVarName);
        } else {
            String wildcardList = typeVariables.stream().map(__ -> "?").collect(Collectors.joining(","));
            codeBuilder.add("(o instanceof $L<$L> $L)", builderClassType.name(), wildcardList, uniqueVarName);
        }
        recordComponents.forEach(recordComponent -> {
            String name = recordComponent.name();
            if (recordComponent.typeName().isPrimitive()) {
                codeBuilder.add("\n&& ($L == $L.$L)", name, uniqueVarName, name);
            } else {
                codeBuilder.add("\n&& $T.equals($L, $L.$L)", Objects.class, name, uniqueVarName, name);
            }
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder("equals").addParameter(Object.class, "o")
                .addModifiers(Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                .addAnnotation(Override.class).returns(TypeName.BOOLEAN).addStatement(codeBuilder.build()).build();
        builder.addMethod(methodSpec);
    }

    private void addBuildMethod() {
        /*
         * Adds the build method that generates the record similar to:
         *
         * public MyRecord build() { return new MyRecord(p1, p2, ...); }
         */
        CodeBlock codeBlock = buildCodeBlock();
        MethodSpec methodSpec = buildMethod().addCode(codeBlock).build();
        builder.addMethod(methodSpec);
    }

    private MethodSpec.Builder buildMethod() {
        return MethodSpec.methodBuilder(metaData.buildMethodName())
                .addJavadoc("Return a new record instance with all fields set to the current values in this builder\n")
                .addModifiers(Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                .returns(recordClassType.typeName());
    }

    private CodeBlock buildCodeBlock() {
        /*
         * Builds the code block for allocating the record from its parts
         */

        var codeBuilder = CodeBlock.builder();

        IntStream.range(0, recordComponents.size()).forEach(index -> {
            var recordComponent = recordComponents.get(index);
            if (collectionBuilderUtils.isImmutableCollection(recordComponent)) {
                codeBuilder.add("$[$L = ", recordComponent.name());
                collectionBuilderUtils.addShimCall(codeBuilder, recordComponents.get(index));
                codeBuilder.add(";\n$]");
            }
        });

        addNullCheckCodeBlock(codeBuilder);
        codeBuilder.add("$[return ");
        if (metaData.useValidationApi()) {
            codeBuilder.add("$T.validate(", validatorTypeName);
        }
        codeBuilder.add("new $T(", recordClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }
            codeBuilder.add("$L", recordComponents.get(index).name());
        });
        codeBuilder.add(")");
        if (metaData.useValidationApi()) {
            codeBuilder.add(")");
        }
        codeBuilder.add(";$]");
        return codeBuilder.build();
    }

    private TypeName buildWithTypeName() {
        ClassName rawTypeName = ClassName.get(packageName, builderClassType.name() + "." + metaData.withClassName());
        if (typeVariables.isEmpty()) {
            return rawTypeName;
        }
        return ParameterizedTypeName.get(rawTypeName, typeVariables.toArray(new TypeName[] {}));
    }

    private void addFromWithClass() {
        /*
         * Adds static private class that implements/proxies the Wither
         *
         * private static final class _FromWith implements MyRecordBuilder.With { private final MyRecord from;
         *
         * @Override public String p1() { return from.p1(); }
         *
         * @Override public String p2() { return from.p2(); } }
         */

        var fromWithClassBuilder = TypeSpec.classBuilder(metaData.fromWithClassName())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addAnnotation(generatedRecordBuilderAnnotation).addTypeVariables(typeVariables)
                .addSuperinterface(buildWithTypeName());
        if (metaData.addClassRetainedGenerated()) {
            fromWithClassBuilder.addAnnotation(recordBuilderGeneratedAnnotation);
        }

        fromWithClassBuilder.addField(recordClassType.typeName(), "from", Modifier.PRIVATE, Modifier.FINAL);
        MethodSpec constructorSpec = MethodSpec.constructorBuilder().addParameter(recordClassType.typeName(), "from")
                .addStatement("this.from = from").addModifiers(Modifier.PRIVATE)
                .addAnnotation(generatedRecordBuilderAnnotation).build();
        fromWithClassBuilder.addMethod(constructorSpec);

        IntStream.range(0, recordComponents.size()).forEach(index -> {
            var component = recordComponents.get(index);
            MethodSpec methodSpec = MethodSpec.methodBuilder(component.name()).returns(component.typeName())
                    .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                    .addStatement("return from.$L()", component.name()).addAnnotation(generatedRecordBuilderAnnotation)
                    .build();
            fromWithClassBuilder.addMethod(methodSpec);
        });
        this.builder.addType(fromWithClassBuilder.build());
    }

    private void addStaticFromWithMethod() {
        /*
         * Adds static method that returns a "with"er view of an existing record.
         *
         * public static With from(MyRecord from) { return new _FromWith(from); }
         */

        addFromWithClass();

        var methodSpec = MethodSpec.methodBuilder(metaData.fromMethodName())
                .addJavadoc("Return a \"with\"er for an existing record instance\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC).addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables).addParameter(recordClassType.typeName(), metaData.fromMethodName())
                .returns(buildWithTypeName()).addStatement("return new $L$L(from)", metaData.fromWithClassName(),
                        typeVariables.isEmpty() ? "" : "<>")
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticCopyBuilderMethod() {
        /*
         * Adds a copy builder method that pre-fills the builder with existing values similar to:
         *
         * public static MyRecordBuilder builder(MyRecord from) { return new MyRecordBuilder(from.p1(), from.p2(), ...);
         * }
         */
        var codeBuilder = CodeBlock.builder().add("return new $T(", builderClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }
            codeBuilder.add("from.$L()", recordComponents.get(index).name());
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder(metaData.copyMethodName())
                .addJavadoc(
                        "Return a new builder with all fields set to the values taken from the given record instance\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC).addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables).addParameter(recordClassType.typeName(), "from")
                .returns(builderClassType.typeName()).addStatement(codeBuilder.build()).build();
        builder.addMethod(methodSpec);
    }

    private void addStaticDefaultBuilderMethod() {
        /*
         * Adds the default builder method similar to:
         *
         * public static MyRecordBuilder builder() { return new MyRecordBuilder(); }
         */
        var methodSpec = MethodSpec.methodBuilder(metaData.builderMethodName())
                .addJavadoc("Return a new builder with all fields set to default Java values\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC).addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables).returns(builderClassType.typeName())
                .addStatement("return new $T()", builderClassType.typeName()).build();
        builder.addMethod(methodSpec);
    }

    private void addStaticStagedBuilderMethod(String builderMethodName) {
        if (recordComponents.size() < 2) {
            return;
        }

        /*
         * Adds the staged builder method similar to:
         *
         * public static NameStage stagedBuilder() { return name -> age -> () -> new PersonBuilder(name, age).build(); }
         */
        var codeBlock = CodeBlock.builder();
        if (metaData.onceOnlyAssignment()) {
            codeBlock.addStatement("$T $L = new $T()", builderClassType.typeName(), uniqueVarName,
                    builderClassType.typeName());
            codeBlock.add("return ");
            recordComponents.forEach(recordComponent -> {
                codeBlock.add("$L -> {\n", recordComponent.name()).indent()
                        .addStatement("$L.$L($L)", uniqueVarName, recordComponent.name(), recordComponent.name())
                        .add("return ");
            });
            codeBlock.addStatement("() -> $L", uniqueVarName);
            IntStream.range(0, recordComponents.size()).forEach(__ -> codeBlock.unindent().addStatement("}"));
        } else {
            codeBlock.add("return ");
            recordComponents.forEach(recordComponent -> codeBlock.add("$L -> ", recordComponent.name()));
            codeBlock.add("() -> new $T(", builderClassType.typeName());
            IntStream.range(0, recordComponents.size()).forEach(index -> {
                if (index > 0) {
                    codeBlock.add(", ");
                }
                codeBlock.add("$L", recordComponents.get(index).name());
            });
            codeBlock.addStatement(")");
        }

        var methodSpec = MethodSpec.methodBuilder(builderMethodName)
                .addJavadoc("Return the first stage of a staged builder\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC).addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables).returns(stagedBuilderType(recordComponents.get(0)).typeName())
                .addCode(codeBlock.build()).build();
        builder.addMethod(methodSpec);
    }

    private void addStaticComponentsMethod() {
        /*
         * Adds a static method that converts a record instance into a stream of its component parts
         *
         * public static Stream<Map.Entry<String, Object>> stream(MyRecord record) { return Stream.of(new
         * AbstractMap.SimpleImmutableEntry<>("p1", record.p1()), new AbstractMap.SimpleImmutableEntry<>("p2",
         * record.p2())); }
         */
        var codeBuilder = CodeBlock.builder().add("return $T.of(", Stream.class);
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(",\n ");
            }
            var name = recordComponents.get(index).name();
            codeBuilder.add("new $T<>($S, record.$L())", AbstractMap.SimpleImmutableEntry.class, name, name);
        });
        codeBuilder.add(")");
        var mapEntryTypeVariables = ParameterizedTypeName.get(Map.Entry.class, String.class, Object.class);
        var mapEntryType = ParameterizedTypeName.get(ClassName.get(Stream.class), mapEntryTypeVariables);
        var methodSpec = MethodSpec.methodBuilder(metaData.componentsMethodName()).addJavadoc(
                "Return a stream of the record components as map entries keyed with the component name and the value as the component value\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC).addParameter(recordClassType.typeName(), "record")
                .addAnnotation(generatedRecordBuilderAnnotation).addTypeVariables(typeVariables).returns(mapEntryType)
                .addStatement(codeBuilder.build()).build();
        builder.addMethod(methodSpec);
    }

    private void add1Field(ClassType component) {
        /*
         * For a single record component, add a field similar to:
         *
         * private T p;
         */
        var fieldSpecBuilder = FieldSpec.builder(component.typeName(), component.name(), Modifier.PRIVATE);

        CodeBlock initializer = initializers.get(component.name());
        if (initializer != null) {
            fieldSpecBuilder.initializer(initializer);
        } else if (metaData.emptyDefaultForOptional()) {
            Optional<OptionalType> thisOptionalType = OptionalType.fromClassType(component);
            if (thisOptionalType.isPresent()) {
                var codeBlock = CodeBlock.builder().add("$T.empty()", thisOptionalType.get().typeName()).build();
                fieldSpecBuilder.initializer(codeBlock);
            }
        }

        builder.addField(fieldSpecBuilder.build());
    }

    private void addNestedGetterMethod(TypeSpec.Builder classBuilder, RecordClassType component, String methodName) {
        /*
         * For a single record component, add a getter similar to:
         *
         * T p();
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(methodName)
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n",
                        component.name())
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName());
        addAccessorAnnotations(component, methodSpecBuilder, this::filterOutValid);
        classBuilder.addMethod(methodSpecBuilder.build());
    }

    private boolean filterOutOverride(AnnotationSpec annotationSpec) {
        return !annotationSpec.type.equals(overrideType);
    }

    private boolean filterOutValid(AnnotationSpec annotationSpec) {
        return !annotationSpec.type.equals(validType);
    }

    private void addConstructorAnnotations(RecordClassType component, ParameterSpec.Builder parameterSpecBuilder) {
        if (metaData.inheritComponentAnnotations()) {
            component.getCanonicalConstructorAnnotations().stream().map(AnnotationSpec::get)
                    .filter(this::filterOutOverride).forEach(parameterSpecBuilder::addAnnotation);
        }
    }

    private void addAccessorAnnotations(RecordClassType component, MethodSpec.Builder methodSpecBuilder,
            Predicate<AnnotationSpec> additionalFilter) {
        if (metaData.inheritComponentAnnotations()) {
            component.getAccessorAnnotations().stream().map(AnnotationSpec::get).filter(this::filterOutOverride)
                    .filter(additionalFilter).forEach(methodSpecBuilder::addAnnotation);
        }
    }

    private String capitalize(String s) {
        return (s.length() < 2) ? s.toUpperCase(Locale.ROOT) : (Character.toUpperCase(s.charAt(0)) + s.substring(1));
    }

    private void add1CollectionBuilders(SingleItemsMetaData meta, RecordClassType component) {
        if (collectionBuilderUtils.isList(component) || collectionBuilderUtils.isSet(component)) {
            add1ListBuilder(meta, component);
        } else if (collectionBuilderUtils.isMap(component)) {
            add1MapBuilder(meta, component);
        }
    }

    private void add1MapBuilder(SingleItemsMetaData meta, RecordClassType component) {
        /*
         * For a single map record component, add a methods similar to:
         *
         * public T addP(K key, V value) { this.p = __ensureMapMutable(p); this.p.put(key, value); return this; }
         *
         * public T addP(Stream<? extends Map.Entry<K, V> i) { this.p = __ensureMapMutable(p); i.forEach(this.p::put);
         * return this; }
         *
         * public T addP(Iterable<? extends Map.Entry<K, V> i) { this.p = __ensureMapMutable(p); i.forEach(this.p::put);
         * return this; }
         */
        for (var i = 0; i < 3; ++i) {
            var codeBlockBuilder = CodeBlock.builder();
            if (collectionBuilderUtils.isImmutableCollection(component)) {
                codeBlockBuilder.addStatement("this.$L = $L($L)", component.name(),
                        collectionBuilderUtils.mutableMakerName(component), component.name());
            } else {
                codeBlockBuilder.beginControlFlow("if (this.$L == null)", component.name())
                        .addStatement("this.$L = new $T<>()", component.name(), meta.singleItemCollectionClass())
                        .endControlFlow();
            }
            var methodSpecBuilder = MethodSpec
                    .methodBuilder(metaData.singleItemBuilderPrefix() + capitalize(component.name()))
                    .addJavadoc("Add to the internally allocated {@code HashMap} for {@code $L}\n", component.name())
                    .addModifiers(Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                    .returns(builderClassType.typeName());
            if (i == 0) {
                methodSpecBuilder.addParameter(meta.typeArguments().get(0), "key");
                methodSpecBuilder.addParameter(meta.typeArguments().get(1), "value");
                codeBlockBuilder.addStatement("this.$L.put(key, value)", component.name());
            } else {
                var parameterClass = ClassName.get((i == 1) ? Stream.class : Iterable.class);
                var entryType = ParameterizedTypeName.get(ClassName.get(Map.Entry.class),
                        WildcardTypeName.subtypeOf(meta.typeArguments().get(0)),
                        WildcardTypeName.subtypeOf(meta.typeArguments().get(1)));
                ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(parameterClass,
                        WildcardTypeName.subtypeOf(entryType));
                methodSpecBuilder.addParameter(parameterizedTypeName, "i");
                codeBlockBuilder.addStatement("i.forEach(entry -> this.$L.put(entry.getKey(), entry.getValue()))",
                        component.name());
            }
            codeBlockBuilder.addStatement("return this");
            methodSpecBuilder.addCode(codeBlockBuilder.build());
            builder.addMethod(methodSpecBuilder.build());
        }
    }

    private void add1ListBuilder(SingleItemsMetaData meta, RecordClassType component) {
        /*
         * For a single list or set record component, add methods similar to:
         *
         * public T addP(I i) { this.list = __ensureListMutable(list); this.p.add(i); return this; }
         *
         * public T addP(Stream<? extends I> i) { this.list = __ensureListMutable(list); this.p.addAll(i); return this;
         * }
         *
         * public T addP(Iterable<? extends I> i) { this.list = __ensureListMutable(list); this.p.addAll(i); return
         * this; }
         */
        for (var i = 0; i < 3; ++i) {
            var addClockBlock = CodeBlock.builder();
            TypeName parameter;
            if (i == 0) {
                addClockBlock.addStatement("this.$L.add(i)", component.name());
                parameter = meta.typeArguments().get(0);
            } else {
                addClockBlock.addStatement("i.forEach(this.$L::add)", component.name());
                var parameterClass = ClassName.get((i == 1) ? Stream.class : Iterable.class);
                parameter = ParameterizedTypeName.get(parameterClass,
                        WildcardTypeName.subtypeOf(meta.typeArguments().get(0)));
            }
            var codeBlockBuilder = CodeBlock.builder();
            if (collectionBuilderUtils.isImmutableCollection(component)) {
                codeBlockBuilder.addStatement("this.$L = $L($L)", component.name(),
                        collectionBuilderUtils.mutableMakerName(component), component.name());
            } else {
                codeBlockBuilder.beginControlFlow("if (this.$L == null)", component.name())
                        .addStatement("this.$L = new $T<>()", component.name(), meta.singleItemCollectionClass())
                        .endControlFlow();
            }
            codeBlockBuilder.add(addClockBlock.build()).addStatement("return this");
            var methodSpecBuilder = MethodSpec
                    .methodBuilder(metaData.singleItemBuilderPrefix() + capitalize(component.name()))
                    .addJavadoc("Add to the internally allocated {@code $L} for {@code $L}\n",
                            meta.singleItemCollectionClass().getSimpleName(), component.name())
                    .addModifiers(Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                    .returns(builderClassType.typeName()).addParameter(parameter, "i")
                    .addCode(codeBlockBuilder.build());
            builder.addMethod(methodSpecBuilder.build());
        }
    }

    private void add1GetterMethod(RecordClassType component) {
        /*
         * For a single record component, add a getter similar to:
         *
         * public T p() { return p; }
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(prefixedName(component, true))
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n",
                        component.name())
                .addModifiers(Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName()).addCode(checkReturnShim(component));
        addAccessorAnnotations(component, methodSpecBuilder, __ -> true);
        builder.addMethod(methodSpecBuilder.build());
    }

    private CodeBlock checkReturnShim(RecordClassType component) {
        var codeBuilder = CodeBlock.builder();
        if (collectionBuilderUtils.isImmutableCollection(component)) {
            codeBuilder.add("return ");
            collectionBuilderUtils.addShimCall(codeBuilder, component);
            codeBuilder.add(";");
        } else {
            codeBuilder.addStatement("return $L", component.name());
        }
        return codeBuilder.build();
    }

    private void add1SetterMethod(RecordClassType component, int componentIndex) {
        /*
         * For a single record component, add a setter similar to:
         *
         * public MyRecordBuilder p(T p) { this.p = p; return this; }
         */
        var methodSpec = MethodSpec.methodBuilder(prefixedName(component, false)).addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation).returns(builderClassType.typeName());

        if (metaData.onceOnlyAssignment()) {
            var onceOnlyCheck = CodeBlock.builder()
                    .add("if ($L[$L]) {\n", metaData.onceOnlyAssignmentName(), componentIndex).indent()
                    .addStatement("throw new IllegalStateException(\"A value has already been set for: $L\")",
                            component.name())
                    .unindent().add("}\n")
                    .addStatement("$L[$L] = true", metaData.onceOnlyAssignmentName(), componentIndex).build();
            methodSpec.addCode(onceOnlyCheck);
        }

        var collectionMetaData = collectionBuilderUtils.singleItemsMetaData(component, STANDARD_FOR_SETTER);
        var parameterSpecBuilder = collectionMetaData.map(meta -> {
            CodeBlock.Builder codeSpec = CodeBlock.builder();
            codeSpec.addStatement("this.$L = $L($L)", component.name(), collectionBuilderUtils.shimName(component),
                    component.name());
            methodSpec.addJavadoc(
                    "Re-create the internally allocated {@code $T} for {@code $L} by copying the argument\n",
                    component.typeName(), component.name()).addCode(codeSpec.build());
            return ParameterSpec.builder(meta.wildType(), component.name());
        }).orElseGet(() -> {
            methodSpec.addJavadoc("Set a new value for the {@code $L} record component in the builder\n",
                    component.name()).addStatement("this.$L = $L", component.name(), component.name());
            return ParameterSpec.builder(component.typeName(), component.name());
        });

        addConstructorAnnotations(component, parameterSpecBuilder);
        methodSpec.addStatement("return this").addParameter(parameterSpecBuilder.build());

        builder.addMethod(methodSpec.build());
    }

    private void add1ConcreteOptionalSetterMethod(RecordClassType component) {
        /*
         * For a single optional record component, add a concrete setter similar to:
         *
         * public MyRecordBuilder p(T p) { this.p = p; return this; }
         */
        var optionalType = OptionalType.fromClassType(component);
        if (optionalType.isEmpty()) {
            return;
        }
        var type = optionalType.get();
        var methodSpec = MethodSpec.methodBuilder(prefixedName(component, false)).addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation).returns(builderClassType.typeName());

        var parameterSpecBuilder = ParameterSpec.builder(type.valueType(), component.name());
        methodSpec.addJavadoc("Set a new value for the {@code $L} record component in the builder\n", component.name())
                .addStatement(getOptionalStatement(type), component.name(), type.typeName(), component.name());
        addConstructorAnnotations(component, parameterSpecBuilder);
        methodSpec.addStatement("return this").addParameter(parameterSpecBuilder.build());
        builder.addMethod(methodSpec.build());
    }

    private String getOptionalStatement(OptionalType type) {

        if (type.isOptional()) {
            return "this.$L = $T.ofNullable($L)";
        }

        return "this.$L = $T.of($L)";
    }

    private List<TypeVariableName> typeVariablesWithReturn() {
        var variables = new ArrayList<TypeVariableName>();
        variables.add(rType);
        variables.addAll(typeVariables);
        return variables;
    }

    private MethodSpec buildFunctionalHandler(String className, String methodName, boolean isMap) {
        /*
         * Build a Functional handler ala:
         *
         * default <R> R map(Function<R, T> proc) { return proc.apply(p()); }
         */
        var localTypeVariables = isMap ? typeVariablesWithReturn() : typeVariables;
        var typeName = localTypeVariables.isEmpty() ? ClassName.get("", className)
                : ParameterizedTypeName.get(ClassName.get("", className), localTypeVariables.toArray(TypeName[]::new));
        var methodBuilder = MethodSpec.methodBuilder(methodName).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).addParameter(typeName, "proc");
        var codeBlockBuilder = CodeBlock.builder();
        if (isMap) {
            methodBuilder.addJavadoc("Map record components into a new object");
            methodBuilder.addTypeVariable(rType);
            methodBuilder.returns(rType);
            codeBlockBuilder.add("return ");
        } else {
            methodBuilder.addJavadoc("Perform an operation on record components");
        }
        codeBlockBuilder.add("proc.apply(");
        addComponentCallsAsArguments(-1, codeBlockBuilder, true);
        codeBlockBuilder.add(");");
        methodBuilder.addCode(codeBlockBuilder.build());
        return methodBuilder.build();
    }

    private TypeSpec buildFunctionalInterface(String className, boolean isMap) {
        /*
         * Build a Functional interface ala:
         *
         * @FunctionalInterface interface Function<R, T> { R apply(T a); }
         */
        var localTypeVariables = isMap ? typeVariablesWithReturn() : typeVariables;
        var methodBuilder = MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        recordComponents.forEach(component -> {
            var parameterSpecBuilder = ParameterSpec.builder(component.typeName(), component.name());
            addConstructorAnnotations(component, parameterSpecBuilder);
            methodBuilder.addParameter(parameterSpecBuilder.build());
        });
        if (isMap) {
            methodBuilder.returns(rType);
        }
        return TypeSpec.interfaceBuilder(className).addAnnotation(generatedRecordBuilderAnnotation)
                .addAnnotation(FunctionalInterface.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(localTypeVariables).addMethod(methodBuilder.build()).build();
    }

    private String prefixedName(RecordClassType component, boolean isGetter) {
        BiFunction<String, String, String> prefixer = (p, s) -> p.isEmpty() ? s
                : p + Character.toUpperCase(s.charAt(0)) + s.substring(1);
        boolean isBool = component.typeName().toString().toLowerCase(Locale.ROOT).equals("boolean");
        if (isGetter) {
            if (isBool) {
                return prefixer.apply(metaData.booleanPrefix(), component.name());
            }
            return prefixer.apply(metaData.getterPrefix(), component.name());
        }
        return prefixer.apply(metaData.setterPrefix(), component.name());
    }

    private String stagedBuilderName(ClassType component) {
        return capitalize(component.name()) + metaData.stagedBuilderMethodSuffix();
    }

    private ClassType stagedBuilderType(ClassType component) {
        return getClassTypeFromNames(ClassName.get("", stagedBuilderName(component)), typeVariables);
    }
}
