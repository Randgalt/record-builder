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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.recordbuilder.core.RecordBuilderMetaData;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;
import static io.soabase.recordbuilder.processor.ElementUtils.getWithMethodName;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;

class InternalRecordBuilderProcessor
{
    private final RecordBuilderMetaData metaData;
    private final ClassType recordClassType;
    private final String packageName;
    private final ClassType builderClassType;
    private final List<TypeVariableName> typeVariables;
    private final List<ClassType> recordComponents;
    private final TypeSpec builderType;
    private final TypeSpec.Builder builder;

    InternalRecordBuilderProcessor(TypeElement record, RecordBuilderMetaData metaData, Optional<String> packageNameOpt)
    {
        this.metaData = metaData;
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = packageNameOpt.orElseGet(() -> ElementUtils.getPackageName(record));
        builderClassType = ElementUtils.getClassType(packageName, getBuilderName(record, metaData, recordClassType, metaData.suffix()), record.getTypeParameters());
        typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        recordComponents = record.getRecordComponents().stream().map(ElementUtils::getClassType).collect(Collectors.toList());

        builder = TypeSpec.classBuilder(builderClassType.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addTypeVariables(typeVariables);
        addWithNestedClass();
        addDefaultConstructor();
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
        addStaticDowncastMethod();
        builderType = builder.build();
    }

    String packageName()
    {
        return packageName;
    }

    ClassType builderClassType()
    {
        return builderClassType;
    }

    TypeSpec builderType()
    {
        return builderType;
    }

    private void addWithNestedClass()
    {
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
        addWithBuilderMethod(classBuilder);
        addWithSuppliedBuilderMethod(classBuilder);
        IntStream.range(0, recordComponents.size()).forEach(index -> add1WithMethod(classBuilder, recordComponents.get(index), index));
        builder.addType(classBuilder.build());
    }

    private void addWithSuppliedBuilderMethod(TypeSpec.Builder classBuilder)
    {
        /*
            Adds a method that returns a pre-filled copy builder similar to:

            default MyRecord with(Consumer<MyRecordBuilder> consumer) {
                MyRecord r = (MyRecord)(Object)this;
                MyRecordBuilder builder MyRecordBuilder.builder(r);
                consumer.accept(builder);
                return builder.build();
            }
         */
        var codeBlockBuilder = CodeBlock.builder()
                .add("$T r = $L(this);\n", recordClassType.typeName(), metaData.downCastMethodName())
                .add("$T builder = $L.$L(r);\n", builderClassType.typeName(), builderClassType.name(), metaData.copyMethodName())
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

    private void addWithBuilderMethod(TypeSpec.Builder classBuilder)
    {
        /*
            Adds a method that returns a pre-filled copy builder similar to:

            default MyRecordBuilder with() {
                MyRecord r = (MyRecord)(Object)this;
                return MyRecordBuilder.builder(r);
            }
         */
        var codeBlockBuilder = CodeBlock.builder()
                .add("$T r = $L(this);\n", recordClassType.typeName(), metaData.downCastMethodName())
                .add("return $L.$L(r);", builderClassType.name(), metaData.copyMethodName());
        var methodSpec = MethodSpec.methodBuilder(metaData.withClassMethodPrefix())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new record builder using the current values")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(builderClassType.typeName())
                .addCode(codeBlockBuilder.build())
                .build();
        classBuilder.addMethod(methodSpec);
    }

    private void add1WithMethod(TypeSpec.Builder classBuilder, ClassType component, int index)
    {
        /*
            Adds a with method for the component similar to:

            default MyRecord withName(String name) {
                MyRecord r = (MyRecord)(Object)this;
                return new MyRecord(name, r.age());
            }
         */
        var codeBlockBuilder = CodeBlock.builder()
                .add("$T r = $L(this);\n", recordClassType.typeName(), metaData.downCastMethodName())
                .add("return new $T(", recordClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(parameterIndex -> {
            if (parameterIndex > 0) {
                codeBlockBuilder.add(", ");
            }
            ClassType parameterComponent = recordComponents.get(parameterIndex);
            if (parameterIndex == index) {
                codeBlockBuilder.add(parameterComponent.name());
            }
            else {
                codeBlockBuilder.add("r.$L()", parameterComponent.name());
            }
        });
        codeBlockBuilder.add(");");

        var methodName = getWithMethodName(component, metaData.withClassMethodPrefix());
        var parameterSpec = ParameterSpec.builder(component.typeName(), component.name()).build();
        var methodSpec = MethodSpec.methodBuilder(methodName)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Return a new instance of {@code $L} with a new value for {@code $L}\n", recordClassType.name(), component.name())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(parameterSpec)
                .addCode(codeBlockBuilder.build())
                .returns(recordClassType.typeName())
                .build();
        classBuilder.addMethod(methodSpec);
    }

    private void addDefaultConstructor()
    {
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

    private void addAllArgsConstructor()
    {
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

    private void addToStringMethod()
    {
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

    private void addHashCodeMethod()
    {
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

    private void addEqualsMethod()
    {
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
        codeBuilder.add("(o instanceof $L b)", builderClassType.name());
        recordComponents.forEach(recordComponent -> {
            String name = recordComponent.name();
            if (recordComponent.typeName().isPrimitive()) {
                codeBuilder.add("\n&& ($L == b.$L)", name, name);
            }
            else {
                codeBuilder.add("\n&& $T.equals($L, b.$L)", Objects.class, name, name);
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

    private void addBuildMethod()
    {
        /*
            Adds the build method that generates the record similar to:

            public MyRecord build() {
                return new MyRecord(p1, p2, ...);
            }
         */
        var codeBuilder = CodeBlock.builder().add("return new $T(", recordClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            if (index > 0) {
                codeBuilder.add(", ");
            }
            codeBuilder.add("$L", recordComponents.get(index).name());
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder(metaData.buildMethodName())
                .addJavadoc("Return a new record instance with all fields set to the current values in this builder\n")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(recordClassType.typeName())
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticCopyBuilderMethod()
    {
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

    private void addStaticDefaultBuilderMethod()
    {
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

    private void addStaticComponentsMethod()
    {
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

    private void addStaticDowncastMethod()
    {
        /*
            Adds a method that downcasts to the record type

            private static MyRecord _downcast(Object this) {
                return (MyRecord)this;
            }
         */
        var codeBlockBuilder = CodeBlock.builder()
            .add("try {\n")
            .indent()
            .add("return ($T)obj;\n", recordClassType.typeName())
            .unindent()
            .add("}\n")
            .add("catch (ClassCastException dummy) {\n")
            .indent()
            .add("throw new RuntimeException($S);\n", builderClassType.name() + "." + metaData.withClassName() + " can only be implemented for " + recordClassType.name())
            .unindent()
            .add("}");
        var methodSpec = MethodSpec.methodBuilder(metaData.downCastMethodName())
            .addAnnotation(generatedRecordBuilderAnnotation)
            .addJavadoc("Downcast to {@code $L}\n", recordClassType.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(Object.class, "obj")
            .addTypeVariables(typeVariables)
            .returns(recordClassType.typeName())
            .addCode(codeBlockBuilder.build())
            .build();
        builder.addMethod(methodSpec);
    }

    private void add1Field(ClassType component)
    {
        /*
            For a single record component, add a field similar to:

            private T p;
         */
        var fieldSpec = FieldSpec.builder(component.typeName(), component.name(), Modifier.PRIVATE).build();
        builder.addField(fieldSpec);
    }

    private void add1GetterMethod(ClassType component)
    {
        /*
            For a single record component, add a getter similar to:

            public T p() {
                return p;
            }
         */
        var methodSpec = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName())
                .addStatement("return $L", component.name())
                .build();
        builder.addMethod(methodSpec);
    }

    private void add1SetterMethod(ClassType component)
    {
        /*
            For a single record component, add a setter similar to:

            public MyRecordBuilder p(T p) {
                this.p = p;
                return this;
            }
         */
        var parameterSpec = ParameterSpec.builder(component.typeName(), component.name()).build();
        var methodSpec = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Set a new value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addParameter(parameterSpec)
                .returns(builderClassType.typeName())
                .addStatement("this.$L = $L", component.name(), component.name())
                .addStatement("return this")
                .build();
        builder.addMethod(methodSpec);
    }
}
