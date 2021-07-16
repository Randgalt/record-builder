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

import javax.lang.model.element.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    private static final TypeName optionalType = TypeName.get(Optional.class);
    private static final TypeName optionalIntType = TypeName.get(OptionalInt.class);
    private static final TypeName optionalLongType = TypeName.get(OptionalLong.class);
    private static final TypeName optionalDoubleType = TypeName.get(OptionalDouble.class);
    private static final TypeName validatorTypeName = ClassName.get("io.soabase.recordbuilder.validator", "RecordBuilderValidator");

    InternalRecordBuilderProcessor(TypeElement record, RecordBuilder.Options metaData, Optional<String> packageNameOpt) {
        this.metaData = getMetaData(record, metaData);
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = packageNameOpt.orElseGet(() -> ElementUtils.getPackageName(record));
        builderClassType = ElementUtils.getClassType(packageName, getBuilderName(record, metaData, recordClassType, metaData.suffix()), record.getTypeParameters());
        typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        recordComponents = buildRecordComponents(record);
        uniqueVarName = getUniqueVarName();
        notNullPattern = Pattern.compile(metaData.interpretNotNullsPattern());

        builder = TypeSpec.classBuilder(builderClassType.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables);
        addWithNestedClass();
        addDefaultConstructor();
        addStaticBuilder();
        if (recordComponents.size() > 0) {
            addAllArgsConstructor();
        }
        addStaticDefaultBuilderMethod();
        addStaticCopyBuilderMethod();
        addStaticComponentsMethod();
        addBuildMethod();
        addToStringMethod();
        addHashCodeMethod();
        addEqualsMethod();
        recordComponents.forEach(component -> {
            add1Field(component);
            add1SetterMethod(component);
            add1GetterMethod(component);
        });
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

    private List<RecordClassType> buildRecordComponents(TypeElement record) {
        var accessorAnnotations = record.getRecordComponents().stream().map(e -> e.getAccessor().getAnnotationMirrors()).collect(Collectors.toList());
        var canonicalConstructorAnnotations = ElementUtils.findCanonicalConstructor(record).map(constructor -> ((ExecutableElement) constructor).getParameters().stream().map(Element::getAnnotationMirrors).collect(Collectors.toList())).orElse(List.of());
        var recordComponents = record.getRecordComponents();
        return IntStream.range(0, recordComponents.size())
                .mapToObj(index -> {
                    var thisAccessorAnnotations = (accessorAnnotations.size() > index) ? accessorAnnotations.get(index) : List.<AnnotationMirror>of();
                    var thisCanonicalConstructorAnnotations = (canonicalConstructorAnnotations.size() > index) ? canonicalConstructorAnnotations.get(index) : List.<AnnotationMirror>of();
                    return ElementUtils.getRecordClassType(recordComponents.get(index), thisAccessorAnnotations, thisCanonicalConstructorAnnotations);
                })
                .collect(Collectors.toList());
    }

    private RecordBuilder.Options getMetaData(TypeElement record, RecordBuilder.Options metaData) {
        var recordSpecificMetaData = record.getAnnotation(RecordBuilder.Options.class);
        return (recordSpecificMetaData != null) ? recordSpecificMetaData : metaData;
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
        recordComponents.forEach(component -> addWithGetterMethod(classBuilder, component));
        addWithBuilderMethod(classBuilder);
        addWithSuppliedBuilderMethod(classBuilder);
        IntStream.range(0, recordComponents.size()).forEach(index -> add1WithMethod(classBuilder, recordComponents.get(index), index));
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
        component.getCanonicalConstructorAnnotations().forEach(annotationMirror -> parameterSpecBuilder.addAnnotation(AnnotationSpec.get(annotationMirror)));
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

    private void addComponentCallsAsArguments(int index, CodeBlock.Builder codeBlockBuilder) {
        IntStream.range(0, recordComponents.size()).forEach(parameterIndex -> {
            if (parameterIndex > 0) {
                codeBlockBuilder.add(", ");
            }
            ClassType parameterComponent = recordComponents.get(parameterIndex);
            if (parameterIndex == index) {
                codeBlockBuilder.add(parameterComponent.name());
            } else {
                codeBlockBuilder.add("$L()", parameterComponent.name());
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
            component.getCanonicalConstructorAnnotations().forEach(annotationMirror -> parameterSpecBuilder.addAnnotation(AnnotationSpec.get(annotationMirror)));
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
            if (!component.typeName().isPrimitive() && isNullAnnotated(component)) {
                builder.addStatement("$T.requireNonNull($L, $S)", Objects.class, component.name(), component.name() + " is required");
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
            var codeBuilder = CodeBlock.builder().add("this.$L = $L", component.name(), component.name());
            constructorBuilder.addStatement(codeBuilder.build());
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
                return Stream.of(
                    new AbstractMap.SimpleEntry<>("p1", record.p1()),
                    new AbstractMap.SimpleEntry<>("p2", record.p2())
                );
            }
         */
        var codeBuilder = CodeBlock.builder().add("return $T.of(", Stream.class);
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(",\n ");
            }
            var name = recordComponents.get(index).name();
            codeBuilder.add("new $T<>($S, record.$L())", AbstractMap.SimpleEntry.class, name, name);
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

    private void addWithGetterMethod(TypeSpec.Builder classBuilder, RecordClassType component) {
        /*
            For a single record component, add a getter similar to:

            T p();
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName());
        component.getAccessorAnnotations().forEach(annotationMirror -> methodSpecBuilder.addAnnotation(AnnotationSpec.get(annotationMirror)));
        classBuilder.addMethod(methodSpecBuilder.build());
    }

    private void add1GetterMethod(RecordClassType component) {
        /*
            For a single record component, add a getter similar to:

            public T p() {
                return p;
            }
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName())
                .addStatement("return $L", component.name());
        component.getAccessorAnnotations().forEach(annotationMirror -> methodSpecBuilder.addAnnotation(AnnotationSpec.get(annotationMirror)));
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
        var parameterSpecBuilder = ParameterSpec.builder(component.typeName(), component.name());
        component.getCanonicalConstructorAnnotations().forEach(annotationMirror -> parameterSpecBuilder.addAnnotation(AnnotationSpec.get(annotationMirror)));

        var methodSpec = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Set a new value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addParameter(parameterSpecBuilder.build())
                .returns(builderClassType.typeName())
                .addStatement("this.$L = $L", component.name(), component.name())
                .addStatement("return this")
                .build();
        builder.addMethod(methodSpec);
    }
}

