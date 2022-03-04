/**
 * Copyright 2019 Jordan Zimmerman
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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.soabase.recordbuilder.processor.CollectionBuilderUtils.SingleItemsMetaDataMode.*;
import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;
import static io.soabase.recordbuilder.processor.ElementUtils.getWithMethodName;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;

class InternalRecordBuilderProcessor {
    private final RecordBuilder.Options metaData;
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
    private static final TypeName optionalType = TypeName.get(Optional.class);
    private static final TypeName optionalIntType = TypeName.get(OptionalInt.class);
    private static final TypeName optionalLongType = TypeName.get(OptionalLong.class);
    private static final TypeName optionalDoubleType = TypeName.get(OptionalDouble.class);
    private static final TypeName validatorTypeName = ClassName.get("io.soabase.recordbuilder.validator", "RecordBuilderValidator");
    private static final TypeVariableName rType = TypeVariableName.get("R");
    private final ProcessingEnvironment processingEnv;

    InternalRecordBuilderProcessor(ProcessingEnvironment processingEnv, TypeElement record, RecordBuilder.Options metaData, Optional<String> packageNameOpt) {
        this.processingEnv = processingEnv;
        var recordActualPackage = ElementUtils.getPackageName(record);
        this.metaData = metaData;
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = packageNameOpt.orElse(recordActualPackage);
        builderClassType = ElementUtils.getClassType(packageName, getBuilderName(record, metaData, recordClassType, metaData.suffix()), record.getTypeParameters());
        typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        recordComponents = buildRecordComponents(record);
        uniqueVarName = getUniqueVarName();
        notNullPattern = Pattern.compile(metaData.interpretNotNullsPattern());
        collectionBuilderUtils = new CollectionBuilderUtils(recordComponents, this.metaData);

        builder = TypeSpec.classBuilder(builderClassType.name())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables);
        addVisibility(recordActualPackage.equals(packageName), record.getModifiers());
        addWithNestedClass();
        if (!metaData.beanClassName().isEmpty()) {
            addBeanNestedClass();
        }
        addDefaultConstructor();
        addStaticBuilder();
        if (recordComponents.size() > 0) {
            addAllArgsConstructor();
        }
        addStaticDefaultBuilderMethod();
        addStaticCopyBuilderMethod();
        addStaticFromWithMethod();
        addStaticComponentsMethod();
        addBuildMethod();
        addToStringMethod();
        addHashCodeMethod();
        addEqualsMethod();
        recordComponents.forEach(component -> {
            add1Field(component);
            add1SetterMethod(component);
            add1GetterMethod(component);
            var collectionMetaData = collectionBuilderUtils.singleItemsMetaData(component, EXCLUDE_WILDCARD_TYPES);
            collectionMetaData.ifPresent(meta -> add1CollectionBuilders(meta, component));
        });
        collectionBuilderUtils.addShims(builder);
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
            if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.PROTECTED)) {
                builder.addModifiers(Modifier.PUBLIC);  // builders are top level classes - can only be public or package-private
            }
            // is package-private
        } else {
            builder.addModifiers(Modifier.PUBLIC);
        }
    }

    private List<RecordClassType> buildRecordComponents(TypeElement record) {
        var accessorAnnotations = record.getRecordComponents().stream().map(e -> e.getAccessor().getAnnotationMirrors()).collect(Collectors.toList());
        var canonicalConstructorAnnotations = ElementUtils.findCanonicalConstructor(record).map(constructor -> ((ExecutableElement) constructor).getParameters().stream().map(Element::getAnnotationMirrors).collect(Collectors.toList())).orElse(List.of());
        var recordComponents = record.getRecordComponents();
        return IntStream.range(0, recordComponents.size())
                .mapToObj(index -> {
                    var thisAccessorAnnotations = (accessorAnnotations.size() > index) ? accessorAnnotations.get(index) : List.<AnnotationMirror>of();
                    var thisCanonicalConstructorAnnotations = (canonicalConstructorAnnotations.size() > index) ? canonicalConstructorAnnotations.get(index) : List.<AnnotationMirror>of();
                    return ElementUtils.getRecordClassType(processingEnv, recordComponents.get(index), thisAccessorAnnotations, thisCanonicalConstructorAnnotations);
                })
                .collect(Collectors.toList());
    }

    private void addWithNestedClass() {
        /*
            Adds a nested interface that adds withers similar to:

            public class MyRecordBuilder {
                public interface With {
                    // with methods
                }
            }
         */
        var classBuilder = TypeSpec.interfaceBuilder(metaData.withClassName())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add withers to {@code $L}\n", recordClassType.name())
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariables(typeVariables);
        recordComponents.forEach(component -> addNestedGetterMethod(classBuilder, component, prefixedName(component, true)));
        addWithBuilderMethod(classBuilder);
        addWithSuppliedBuilderMethod(classBuilder);
        IntStream.range(0, recordComponents.size()).forEach(index -> add1WithMethod(classBuilder, recordComponents.get(index), index));
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
            Adds a nested interface that adds getters similar to:

            public class MyRecordBuilder {
                public interface Bean {
                    // getter methods
                }
            }
         */
        var classBuilder = TypeSpec.interfaceBuilder(metaData.beanClassName())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add getters to {@code $L}\n", recordClassType.name())
                .addModifiers(Modifier.PUBLIC)
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
            Adds a method that returns a pre-filled copy builder similar to:

            default MyRecord with(Consumer<MyRecordBuilder> consumer) {
                MyRecordBuilder builder = with();
                consumer.accept(builder);
                return builder.build();
            }
         */
        var codeBlockBuilder = CodeBlock.builder()
                .add("$T builder = with();\n", builderClassType.typeName())
                .add("consumer.accept(builder);\n")
                .add("return builder.build();\n");
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), builderClassType.typeName());
        var parameter = ParameterSpec.builder(consumerType, "consumer").build();
        var methodSpec = MethodSpec.methodBuilder(metaData.withClassMethodPrefix())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new record built from the builder passed to the given consumer")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(parameter)
                .returns(recordClassType.typeName())
                .addCode(codeBlockBuilder.build())
                .build();
        classBuilder.addMethod(methodSpec);
    }

    private void addWithBuilderMethod(TypeSpec.Builder classBuilder) {
        /*
            Adds a method that returns a pre-filled copy builder similar to:

            default MyRecordBuilder with() {
                return MyRecordBuilder.builder(r);
            }
         */
        var codeBlockBuilder = CodeBlock.builder()
                .add("return new $L(", builderClassType.name());
        addComponentCallsAsArguments(-1, codeBlockBuilder);
        codeBlockBuilder.add(");");
        var methodSpec = MethodSpec.methodBuilder(metaData.withClassMethodPrefix())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new record builder using the current values")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(builderClassType.typeName())
                .addCode(codeBlockBuilder.build())
                .build();
        classBuilder.addMethod(methodSpec);
    }

    private String getUniqueVarName() {
        return getUniqueVarName("");
    }

    private String getUniqueVarName(String prefix) {
        var name = prefix + "r";
        var alreadyExists = recordComponents.stream()
                .map(ClassType::name)
                .anyMatch(n -> n.equals(name));
        return alreadyExists ? getUniqueVarName(prefix + "_") : name;
    }

    private void add1WithMethod(TypeSpec.Builder classBuilder, RecordClassType component, int index) {
        /*
            Adds a with method for the component similar to:

            default MyRecord withName(String name) {
                return new MyRecord(name, r.age());
            }
         */
        var codeBlockBuilder = CodeBlock.builder();
        addNullCheckCodeBlock(codeBlockBuilder, index);
        codeBlockBuilder.add("$[return ");
        if (metaData.useValidationApi()) {
            codeBlockBuilder.add("$T.validate(", validatorTypeName);
        }
        codeBlockBuilder.add("new $T(", recordClassType.typeName());
        addComponentCallsAsArguments(index, codeBlockBuilder);
        codeBlockBuilder.add(")");
        if (metaData.useValidationApi()) {
            codeBlockBuilder.add(")");
        }
        codeBlockBuilder.add(";$]");

        var methodName = getWithMethodName(component, metaData.withClassMethodPrefix());
        var parameterSpecBuilder = ParameterSpec.builder(component.typeName(), component.name());
        addConstructorAnnotations(component, parameterSpecBuilder);
        var methodSpec = MethodSpec.methodBuilder(methodName)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new instance of {@code $L} with a new value for {@code $L}\n", recordClassType.name(), component.name())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(parameterSpecBuilder.build())
                .addCode(codeBlockBuilder.build())
                .returns(recordClassType.typeName())
                .build();
        classBuilder.addMethod(methodSpec);
    }

    private void add1PrefixedGetterMethod(TypeSpec.Builder classBuilder, RecordClassType component) {
        /*
            Adds a get method for the component similar to:

            default MyRecord getName() {
                return name();
            }
         */
        var codeBlockBuilder = CodeBlock.builder();
        codeBlockBuilder.add("$[return $L()$];", component.name());

        var methodName = prefixedName(component, true);
        var methodSpec = MethodSpec.methodBuilder(methodName)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Returns the value of {@code $L}\n", component.name())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addCode(codeBlockBuilder.build())
                .returns(component.typeName())
                .build();
        classBuilder.addMethod(methodSpec);
    }

    private void addComponentCallsAsArguments(int index, CodeBlock.Builder codeBlockBuilder) {
        IntStream.range(0, recordComponents.size()).forEach(parameterIndex -> {
            if (parameterIndex > 0) {
                codeBlockBuilder.add(", ");
            }
            RecordClassType parameterComponent = recordComponents.get(parameterIndex);
            if (parameterIndex == index) {
                collectionBuilderUtils.add(codeBlockBuilder, parameterComponent);
            } else {
                codeBlockBuilder.add("$L()", prefixedName(parameterComponent, true));
            }
        });
    }

    private void addDefaultConstructor() {
        /*
            Adds a default constructor similar to:

            private MyRecordBuilder() {
            }
         */
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .build();
        builder.addMethod(constructor);
    }

    private void addStaticBuilder() {
        /*
            Adds an static builder similar to:

            public static MyRecord(int p1, T p2, ...) {
                return new MyRecord(p1, p2, ...);
            }
         */
        CodeBlock codeBlock = buildCodeBlock();
        var builder = MethodSpec.methodBuilder(recordClassType.name())
                .addJavadoc("Static constructor/builder. Can be used instead of new $L(...)\n", recordClassType.name())
                .addTypeVariables(typeVariables)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(recordClassType.typeName())
                .addCode(codeBlock);
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
                    builder.addStatement("$T.requireNonNull($L, $S)", Objects.class, component.name(), component.name() + " is required");
                }
            }
        }
    }

    private boolean isNullAnnotated(RecordClassType component) {
        return component.getCanonicalConstructorAnnotations().stream()
                .anyMatch(annotation -> notNullPattern.matcher(annotation.getAnnotationType().asElement().getSimpleName().toString()).matches());
    }

    private void addAllArgsConstructor() {
        /*
            Adds an all-args constructor similar to:

            private MyRecordBuilder(int p1, T p2, ...) {
                this.p1 = p1;
                this.p2 = p2;
                ...
            }
         */
        var constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(generatedRecordBuilderAnnotation);
        recordComponents.forEach(component -> {
            constructorBuilder.addParameter(component.typeName(), component.name());
            var collectionMetaData = collectionBuilderUtils.singleItemsMetaData(component, STANDARD);
            collectionMetaData.ifPresentOrElse(meta -> constructorBuilder.addStatement("this.$L = new $T<>($L)", component.name(), meta.singleItemCollectionClass(), component.name()),
                    () -> constructorBuilder.addStatement("this.$L = $L", component.name(), component.name()));
        });
        builder.addMethod(constructorBuilder.build());
    }

    private void addToStringMethod() {
        /*
            add a toString() method similar to:

            @Override
            public String toString() {
                return "MyRecord[p1=blah, p2=blah]";
            }
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

        var methodSpec = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addHashCodeMethod() {
        /*
            add a hashCode() method similar to:

            @Override
            public int hashCode() {
                return Objects.hash(p1, p2);
            }
         */
        var codeBuilder = CodeBlock.builder().add("return $T.hash(", Objects.class);
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }
            codeBuilder.add("$L", recordComponents.get(index).name());
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addAnnotation(Override.class)
                .returns(TypeName.INT)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addEqualsMethod() {
        /*
            add an equals() method similar to:

            @Override
            public boolean equals(Object o) {
                if (this == o) || ((o instanceof MyRecordBuilder b)
                    && Objects.equals(p1, b.p1)
                    && Objects.equals(p2, b.p2));
            }
         */
        var codeBuilder = CodeBlock.builder();
        codeBuilder.add("return (this == o) || (");
        codeBuilder.add("(o instanceof $L $L)", builderClassType.name(), uniqueVarName);
        recordComponents.forEach(recordComponent -> {
            String name = recordComponent.name();
            if (recordComponent.typeName().isPrimitive()) {
                codeBuilder.add("\n&& ($L == $L.$L)", name, uniqueVarName, name);
            } else {
                codeBuilder.add("\n&& $T.equals($L, $L.$L)", Objects.class, name, uniqueVarName, name);
            }
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder("equals")
                .addParameter(Object.class, "o")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addBuildMethod() {
        /*
            Adds the build method that generates the record similar to:

            public MyRecord build() {
                return new MyRecord(p1, p2, ...);
            }
         */
        CodeBlock codeBlock = buildCodeBlock();
        var methodSpec = MethodSpec.methodBuilder(metaData.buildMethodName())
                .addJavadoc("Return a new record instance with all fields set to the current values in this builder\n")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(recordClassType.typeName())
                .addCode(codeBlock)
                .build();
        builder.addMethod(methodSpec);
    }

    private CodeBlock buildCodeBlock() {
        /*
            Builds the code block for allocating the record from its parts
        */

        var codeBuilder = CodeBlock.builder();

        IntStream.range(0, recordComponents.size()).forEach(index -> {
            var recordComponent = recordComponents.get(index);
            if (collectionBuilderUtils.isImmutableCollection(recordComponent)) {
                codeBuilder.add("$[$L = ", recordComponent.name());
                collectionBuilderUtils.add(codeBuilder, recordComponents.get(index));
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

    private void addStaticFromWithMethod() {
        /*
            Adds static method that returns a "with"er view of an existing record.

            public static With from(MyRecord from) {
                return new MyRecordBuilder.With() {
                    @Override
                    public String p1() {
                        return from.p1();
                    }

                    @Override
                    public String p2() {
                        return from.p2();
                    }
                };
            }
         */
        var witherClassNameBuilder = CodeBlock.builder()
                .add("$L.$L", builderClassType.name(), metaData.withClassName());
        if (!typeVariables.isEmpty()) {
            witherClassNameBuilder.add("<");
            IntStream.range(0, typeVariables.size()).forEach(index -> {
                if (index > 0) {
                    witherClassNameBuilder.add(", ");
                }
                witherClassNameBuilder.add(typeVariables.get(index).name);
            });
            witherClassNameBuilder.add(">");
        }
        var witherClassName = witherClassNameBuilder.build().toString();
        var codeBuilder = CodeBlock.builder()
                .add("return new $L", witherClassName)
                .add("() {\n").indent();
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            var component = recordComponents.get(index);
            if (index > 0) {
                codeBuilder.add("\n");
            }
            codeBuilder.add("@Override\n")
                    .add("public $T $L() {\n", component.typeName(), prefixedName(component, true))
                    .indent()
                    .addStatement("return from.$L()", component.name())
                    .unindent()
                    .add("}\n");
        });
        codeBuilder.unindent().addStatement("}");

        var withType = ClassName.get("", witherClassName);
        var methodSpec = MethodSpec.methodBuilder("from")//metaData.copyMethodName())
                .addJavadoc("Return a \"with\"er for an existing record instance\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables)
                .addParameter(recordClassType.typeName(), metaData.fromMethodName())
                .returns(withType)
                .addCode(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticCopyBuilderMethod() {
        /*
            Adds a copy builder method that pre-fills the builder with existing values similar to:

            public static MyRecordBuilder builder(MyRecord from) {
                return new MyRecordBuilder(from.p1(), from.p2(), ...);
            }
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
                .addJavadoc("Return a new builder with all fields set to the values taken from the given record instance\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables)
                .addParameter(recordClassType.typeName(), "from")
                .returns(builderClassType.typeName())
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticDefaultBuilderMethod() {
        /*
            Adds a the default builder method similar to:

            public static MyRecordBuilder builder() {
                return new MyRecordBuilder();
            }
         */
        var methodSpec = MethodSpec.methodBuilder(metaData.builderMethodName())
                .addJavadoc("Return a new builder with all fields set to default Java values\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables)
                .returns(builderClassType.typeName())
                .addStatement("return new $T()", builderClassType.typeName())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticComponentsMethod() {
        /*
            Adds a static method that converts a record instance into a stream of its component parts

            public static Stream<Map.Entry<String, Object>> stream(MyRecord record) {
                return Stream.of(new AbstractMap.SimpleImmutableEntry<>("p1", record.p1()),
                         new AbstractMap.SimpleImmutableEntry<>("p2", record.p2()));
            }
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
        var methodSpec = MethodSpec.methodBuilder(metaData.componentsMethodName())
                .addJavadoc("Return a stream of the record components as map entries keyed with the component name and the value as the component value\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(recordClassType.typeName(), "record")
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables)
                .returns(mapEntryType)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void add1Field(ClassType component) {
        /*
            For a single record component, add a field similar to:

            private T p;
         */
        var fieldSpecBuilder = FieldSpec.builder(component.typeName(), component.name(), Modifier.PRIVATE);
        if (metaData.emptyDefaultForOptional()) {
            TypeName thisOptionalType = null;
            if (isOptional(component)) {
                thisOptionalType = optionalType;
            } else if (component.typeName().equals(optionalIntType)) {
                thisOptionalType = optionalIntType;
            } else if (component.typeName().equals(optionalLongType)) {
                thisOptionalType = optionalLongType;
            } else if (component.typeName().equals(optionalDoubleType)) {
                thisOptionalType = optionalDoubleType;
            }
            if (thisOptionalType != null) {
                var codeBlock = CodeBlock.builder().add("$T.empty()", thisOptionalType).build();
                fieldSpecBuilder.initializer(codeBlock);
            }
        }
        builder.addField(fieldSpecBuilder.build());
    }

    private boolean isOptional(ClassType component) {
        if (component.typeName().equals(optionalType)) {
            return true;
        }
        return (component.typeName() instanceof ParameterizedTypeName parameterizedTypeName) && parameterizedTypeName.rawType.equals(optionalType);
    }

    private void addNestedGetterMethod(TypeSpec.Builder classBuilder, RecordClassType component, String methodName) {
        /*
            For a single record component, add a getter similar to:

            T p();
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(methodName)
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName());
        addAccessorAnnotations(component, methodSpecBuilder);
        classBuilder.addMethod(methodSpecBuilder.build());
    }

    private boolean filterOutOverride(AnnotationSpec annotationSpec) {
        return !annotationSpec.type.equals(overrideType);
    }

    private void addConstructorAnnotations(RecordClassType component, ParameterSpec.Builder parameterSpecBuilder) {
        if (metaData.inheritComponentAnnotations()) {
            component.getCanonicalConstructorAnnotations()
                    .stream()
                    .map(AnnotationSpec::get)
                    .filter(this::filterOutOverride)
                    .forEach(parameterSpecBuilder::addAnnotation);
        }
    }

    private void addAccessorAnnotations(RecordClassType component, MethodSpec.Builder methodSpecBuilder) {
        if (metaData.inheritComponentAnnotations()) {
            component.getAccessorAnnotations()
                    .stream()
                    .map(AnnotationSpec::get)
                    .filter(this::filterOutOverride)
                    .forEach(methodSpecBuilder::addAnnotation);
        }
    }

    private String capitalize(String s) {
        return (s.length() < 2) ? s.toUpperCase(Locale.ROOT) : (Character.toUpperCase(s.charAt(0)) + s.substring(1));
    }

    private void add1CollectionBuilders(CollectionBuilderUtils.SingleItemsMetaData meta, RecordClassType component) {
        if (collectionBuilderUtils.isList(component) || collectionBuilderUtils.isSet(component)) {
            add1ListBuilder(meta, component);
        } else if (collectionBuilderUtils.isMap(component)) {
            add1MapBuilder(meta, component);
        }
    }

    private void add1MapBuilder(CollectionBuilderUtils.SingleItemsMetaData meta, RecordClassType component) {
        /*
            For a single map record component, add a methods similar to:

            public T addP(K key, V value) {
                if (this.p == null) {
                    this.p = new HashMap<>();
                }
                this.p.put(key, value);
                return this;
            }

            public T addP(Stream<? extends Map.Entry<K, V> i) {
                if (p == null) {
                    p = new HashMap<>();
                }
                i.forEach(this.p::put);
                return this;
            }

            public T addP(Iterable<? extends Map.Entry<K, V> i) {
                if (p == null) {
                    p = new HashMap<>();
                }
                i.forEach(this.p::put);
                return this;
            }
         */
        for (var i = 0; i < 3; ++i) {
            var codeBlockBuilder = CodeBlock.builder()
                    .beginControlFlow("if (this.$L == null)", component.name())
                    .addStatement("this.$L = new $T<>()", component.name(), HashMap.class)
                    .endControlFlow();
            var methodSpecBuilder = MethodSpec.methodBuilder(metaData.singleItemBuilderPrefix() + capitalize(component.name()))
                    .addJavadoc("Add to the internally allocated {@code HashMap} for {@code $L}\n", component.name())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(generatedRecordBuilderAnnotation)
                    .returns(builderClassType.typeName());
            if (i == 0) {
                methodSpecBuilder.addParameter(meta.typeArguments().get(0), "key");
                methodSpecBuilder.addParameter(meta.typeArguments().get(1), "value");
                codeBlockBuilder.addStatement("this.$L.put(key, value)", component.name());
            } else {
                var parameterClass = ClassName.get((i == 1) ? Stream.class : Iterable.class);
                var entryType = ParameterizedTypeName.get(ClassName.get(Map.Entry.class), WildcardTypeName.subtypeOf(meta.typeArguments().get(0)), WildcardTypeName.subtypeOf(meta.typeArguments().get(1)));
                ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(parameterClass, WildcardTypeName.subtypeOf(entryType));
                methodSpecBuilder.addParameter(parameterizedTypeName, "i");
                codeBlockBuilder.addStatement("i.forEach(entry -> this.$L.put(entry.getKey(), entry.getValue()))", component.name());
            }
            codeBlockBuilder.addStatement("return this");
            methodSpecBuilder.addCode(codeBlockBuilder.build());
            builder.addMethod(methodSpecBuilder.build());
        }
    }

    private void add1ListBuilder(CollectionBuilderUtils.SingleItemsMetaData meta, RecordClassType component) {
        /*
            For a single list or set record component, add methods similar to:

            public T addP(I i) {
                if (this.p == null) {
                    this.p = new ArrayList<>();
                }
                this.p.add(i);
                return this;
            }

            public T addP(Stream<? extends I> i) {
                if (this.p == null) {
                    this.p = new ArrayList<>();
                }
                this.p.addAll(i);
                return this;
            }

            public T addP(Iterable<? extends I> i) {
                if (this.p == null) {
                    this.p = new ArrayList<>();
                }
                this.p.addAll(i);
                return this;
            }
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
                parameter = ParameterizedTypeName.get(parameterClass, WildcardTypeName.subtypeOf(meta.typeArguments().get(0)));
            }
            var codeBlockBuilder = CodeBlock.builder()
                    .beginControlFlow("if (this.$L == null)", component.name())
                    .addStatement("this.$L = new $T<>()", component.name(), meta.singleItemCollectionClass())
                    .endControlFlow()
                    .add(addClockBlock.build())
                    .addStatement("return this");
            var methodSpecBuilder = MethodSpec.methodBuilder(metaData.singleItemBuilderPrefix() + capitalize(component.name()))
                    .addJavadoc("Add to the internally allocated {@code $L} for {@code $L}\n", meta.singleItemCollectionClass().getSimpleName(), component.name())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(generatedRecordBuilderAnnotation)
                    .returns(builderClassType.typeName())
                    .addParameter(parameter, "i")
                    .addCode(codeBlockBuilder.build());
            builder.addMethod(methodSpecBuilder.build());
        }
    }

    private void add1GetterMethod(RecordClassType component) {
        /*
            For a single record component, add a getter similar to:

            public T p() {
                return p;
            }
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(prefixedName(component, true))
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName())
                .addStatement("return $L", component.name());
        addAccessorAnnotations(component, methodSpecBuilder);
        builder.addMethod(methodSpecBuilder.build());
    }

    private void add1SetterMethod(RecordClassType component) {
        /*
            For a single record component, add a setter similar to:

            public MyRecordBuilder p(T p) {
                this.p = p;
                return this;
            }
         */
        var methodSpec = MethodSpec.methodBuilder(prefixedName(component, false))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(builderClassType.typeName());

        var collectionMetaData = collectionBuilderUtils.singleItemsMetaData(component, STANDARD_FOR_SETTER);
        var parameterSpecBuilder = collectionMetaData.map(meta -> {
            CodeBlock.Builder codeSpec = CodeBlock.builder();
            codeSpec.addStatement("this.$L = ($L != null) ? new $T<>($L) : null", component.name(), component.name(), meta.singleItemCollectionClass(), component.name());
            methodSpec.addJavadoc("Re-create the internally allocated {@code $L} for {@code $L} by copying the argument\n", meta.singleItemCollectionClass().getSimpleName(), component.name())
                    .addCode(codeSpec.build());
            return ParameterSpec.builder(meta.wildType(), component.name());
        }).orElseGet(() -> {
            methodSpec.addJavadoc("Set a new value for the {@code $L} record component in the builder\n", component.name())
                    .addStatement("this.$L = $L", component.name(), component.name());
            return ParameterSpec.builder(component.typeName(), component.name());
        });
        addConstructorAnnotations(component, parameterSpecBuilder);
        methodSpec.addStatement("return this").addParameter(parameterSpecBuilder.build());
        builder.addMethod(methodSpec.build());
    }

    private List<TypeVariableName> typeVariablesWithReturn() {
        var variables = new ArrayList<TypeVariableName>();
        variables.add(rType);
        variables.addAll(typeVariables);
        return variables;
    }

    private MethodSpec buildFunctionalHandler(String className, String methodName, boolean isMap) {
        /*
            Build a Functional handler ala:

            default <R> R map(Function<R, T> proc) {
                return proc.apply(p());
            }
        */
        var localTypeVariables = isMap ? typeVariablesWithReturn() : typeVariables;
        var typeName = localTypeVariables.isEmpty() ? ClassName.get("", className) : ParameterizedTypeName.get(ClassName.get("", className), localTypeVariables.toArray(TypeName[]::new));
        var methodBuilder = MethodSpec.methodBuilder(methodName)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(typeName, "proc");
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
        addComponentCallsAsArguments(-1, codeBlockBuilder);
        codeBlockBuilder.add(");");
        methodBuilder.addCode(codeBlockBuilder.build());
        return methodBuilder.build();
    }

    private TypeSpec buildFunctionalInterface(String className, boolean isMap) {
        /*
            Build a Functional interface ala:

            @FunctionalInterface
            interface Function<R, T> {
                R apply(T a);
            }
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
        return TypeSpec.interfaceBuilder(className)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addAnnotation(FunctionalInterface.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(localTypeVariables)
                .addMethod(methodBuilder.build())
                .build();
    }

    private String prefixedName(RecordClassType component, boolean isGetter) {
        BiFunction<String, String, String> prefixer = (p, s) -> p.isEmpty()
            ? s : p + Character.toUpperCase(s.charAt(0)) + s.substring(1);
        boolean isBool = component.typeName().toString().toLowerCase(Locale.ROOT).equals("boolean");
        if (isGetter) {
            if (isBool) {
                return prefixer.apply(metaData.booleanPrefix(), component.name());
            }
            return prefixer.apply(metaData.getterPrefix(), component.name());
        }
        return prefixer.apply(metaData.setterPrefix(), component.name());
    }
}

