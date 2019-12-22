/**
 * Copyright 2016 Jordan Zimmerman
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
import io.soabase.recordbuilder.core.RecordBuilderMetaData;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class InternalProcessor {
    private static final AnnotationSpec generatedAnnotation = AnnotationSpec.builder(Generated.class).addMember("value", "$S", RecordBuilderProcessor.NAME).build();

    private final RecordBuilderMetaData metaData;
    private final ClassType recordClassType;
    private final String packageName;
    private final ClassType builderClassType;
    private final List<TypeVariableName> typeVariables;
    private final List<ClassType> recordComponents;
    private final TypeSpec builderType;
    private final TypeSpec.Builder builder;

    InternalProcessor(TypeElement record, RecordBuilderMetaData metaData) {
        this.metaData = metaData;
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = ElementUtils.getPackageName(record);
        builderClassType = ElementUtils.getClassType(packageName, getBuilderName(record, metaData, recordClassType), record.getTypeParameters());
        typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        recordComponents = record.getRecordComponents().stream().map(ElementUtils::getClassType).collect(Collectors.toList());

        builder = TypeSpec.classBuilder(builderClassType.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedAnnotation)
                .addTypeVariables(typeVariables);
        addDefaultConstructor();
        addAllArgsConstructor();
        addStaticDefaultBuilderMethod();
        addStaticCopyBuilderMethod();
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

    private String getBuilderName(TypeElement record, RecordBuilderMetaData metaData, ClassType recordClassType) {
        // generate the record builder class name
        var baseName = recordClassType.name() + metaData.suffix();
        return metaData.prefixEnclosingClassNames() ? (getBuilderNamePrefix(record.getEnclosingElement()) + baseName) : baseName;
    }

    private String getBuilderNamePrefix(Element element) {
        // prefix enclosing class names if this record is nested in a class
        if (element instanceof TypeElement) {
            return getBuilderNamePrefix(element.getEnclosingElement()) + element.getSimpleName().toString();
        }
        return "";
    }


    private void addDefaultConstructor() {
        /*
            Adds a default constructor similar to:

            private MyRecordBuilder() {
            }
         */
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(generatedAnnotation)
                .build();
        builder.addMethod(constructor);
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
                .addAnnotation(generatedAnnotation);
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
                .addAnnotation(generatedAnnotation)
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
                .addAnnotation(generatedAnnotation)
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
        codeBuilder.add("(o instanceof $L b)", builderClassType.name());
        recordComponents.forEach(recordComponent -> {
            String name = recordComponent.name();
            if (recordComponent.typeName().isPrimitive()) {
                codeBuilder.add("\n&& ($L == b.$L)", name, name);
            } else {
                codeBuilder.add("\n&& $T.equals($L, b.$L)", Objects.class, name, name);
            }
        });
        codeBuilder.add(")");

        var methodSpec = MethodSpec.methodBuilder("equals")
                .addParameter(Object.class, "o")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedAnnotation)
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
                .addAnnotation(generatedAnnotation)
                .returns(recordClassType.typeName())
                .addStatement(codeBuilder.build())
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
                .addAnnotation(generatedAnnotation)
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
                .addAnnotation(generatedAnnotation)
                .addTypeVariables(typeVariables)
                .returns(builderClassType.typeName())
                .addStatement("return new $T()", builderClassType.typeName())
                .build();
        builder.addMethod(methodSpec);
    }

    private void add1Field(ClassType component) {
        /*
            For a single record component, add a field similar to:

            private T p;
         */
        var fieldSpec = FieldSpec.builder(component.typeName(), component.name(), Modifier.PRIVATE).build();
        builder.addField(fieldSpec);
    }

    private void add1GetterMethod(ClassType component) {
        /*
            For a single record component, add a getter similar to:

            public T p() {
                return p;
            }
         */
        var methodSpec = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n", component.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedAnnotation)
                .returns(component.typeName())
                .addStatement("return $L", component.name())
                .build();
        builder.addMethod(methodSpec);
    }

    private void add1SetterMethod(ClassType component) {
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
                .addAnnotation(generatedAnnotation)
                .addParameter(parameterSpec)
                .returns(builderClassType.typeName())
                .addStatement("this.$L = $L", component.name(), component.name())
                .addStatement("return this")
                .build();
        builder.addMethod(methodSpec);
    }
}
