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

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SupportedAnnotationTypes("io.soabase.recordbuilder.core.RecordBuilder")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class RecordBuilderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation).forEach(this::process));
        return true;
    }

    private void process(Element element) {
        var messager = processingEnv.getMessager();
        if (element.getKind() != ElementKind.RECORD) {
            messager.printMessage(Diagnostic.Kind.ERROR, "RecordBuilder only valid for records.", element);
            return;
        }
        var metaData = new RecordBuilderMetaDataLoader(processingEnv.getOptions().get(RecordBuilderMetaData.JAVAC_OPTION_NAME), s -> messager.printMessage(Diagnostic.Kind.NOTE, s)).getMetaData();
        process((TypeElement) element, metaData);
    }

    private void process(TypeElement record, RecordBuilderMetaData metaData) {
        var recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        var packageName = ElementUtils.getPackageName(record);
        var builderClassType = ElementUtils.getClassType(packageName, getBuilderName(record, metaData, recordClassType), record.getTypeParameters());
        var typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        var recordComponents = record.getRecordComponents().stream().map(ElementUtils::getClassType).collect(Collectors.toList());

        var builder = TypeSpec.classBuilder(builderClassType.name()).addModifiers(Modifier.PUBLIC);
        builder.addTypeVariables(typeVariables);

        addDefaultConstructor(builder);
        addAllArgsConstructor(builder, recordComponents);
        addStaticDefaultBuilderMethod(builder, builderClassType, typeVariables, metaData);
        addStaticCopyMethod(builder, builderClassType, recordClassType, recordComponents, typeVariables, metaData);
        addBuildMethod(builder, recordClassType, recordComponents, metaData);
        addToStringMethod(builder, builderClassType, recordComponents);
        addHashCodeMethod(builder, recordComponents);
        addEqualsMethod(builder, builderClassType, recordComponents);
        recordComponents.forEach(component -> {
            add1Field(builder, component);
            add1SetterMethod(builder, component, builderClassType);
        });

        writeJavaFile(record, packageName, builderClassType, builder, metaData);
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

    private void addDefaultConstructor(TypeSpec.Builder builder) {
        /*
            Adds a default constructor similar to:

            private MyRecordBuilder() {
            }
         */
        var constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE);
        builder.addMethod(constructorBuilder.build());
    }

    private void addAllArgsConstructor(TypeSpec.Builder builder, List<ClassType> recordComponents) {
        /*
            Adds an all-args constructor similar to:

            private MyRecordBuilder(int p1, T p2, ...) {
                this.p1 = p1;
                this.p2 = p2;
                ...
            }
         */
        var constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE);
        recordComponents.forEach(component -> {
            constructorBuilder.addParameter(component.typeName(), component.name());
            var codeBuilder = CodeBlock.builder().add("this.$L = $L", component.name(), component.name());
            constructorBuilder.addStatement(codeBuilder.build());
        });
        builder.addMethod(constructorBuilder.build());
    }

    private void addToStringMethod(TypeSpec.Builder builder, ClassType builderClassType, List<ClassType> recordComponents) {
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
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addHashCodeMethod(TypeSpec.Builder builder, List<ClassType> recordComponents) {
        /*
            add an hashCode() method similar to:

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
                .addAnnotation(Override.class)
                .returns(TypeName.INT)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addEqualsMethod(TypeSpec.Builder builder, ClassType builderClassType, List<ClassType> recordComponents) {
        /*
            add an equals() method similar to:

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                return (o instanceof MyRecordBuilder b)
                    && Objects.equals(p1, b.p1)
                    && Objects.equals(p2, b.p2);
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
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addBuildMethod(TypeSpec.Builder builder, ClassType recordClassType, List<ClassType> recordComponents, RecordBuilderMetaData metaData) {
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
                .returns(recordClassType.typeName())
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticCopyMethod(TypeSpec.Builder builder, ClassType builderClassType, ClassType recordClassType, List<ClassType> recordComponents, List<TypeVariableName> typeVariables, RecordBuilderMetaData metaData) {
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
                .addTypeVariables(typeVariables)
                .addParameter(recordClassType.typeName(), "from")
                .returns(builderClassType.typeName())
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticDefaultBuilderMethod(TypeSpec.Builder builder, ClassType builderClassType, List<TypeVariableName> typeVariables, RecordBuilderMetaData metaData) {
        /*
            Adds a the default builder method similar to:

            public static MyRecordBuilder builder() {
                return new MyRecordBuilder();
            }
         */
        var methodSpec = MethodSpec.methodBuilder(metaData.builderMethodName())
                .addJavadoc("Return a new builder with all fields set to default Java values\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(typeVariables)
                .returns(builderClassType.typeName())
                .addStatement("return new $T()", builderClassType.typeName())
                .build();
        builder.addMethod(methodSpec);
    }

    private void add1Field(TypeSpec.Builder builder, ClassType component) {
        /*
            For a single record component, add a field similar to:

            private T p;
         */
        var fieldSpec = FieldSpec.builder(component.typeName(), component.name(), Modifier.PRIVATE).build();
        builder.addField(fieldSpec);
    }

    private void add1SetterMethod(TypeSpec.Builder builder, ClassType component, ClassType builderClassType) {
        /*
            For a single record component, add a setter similar to:

            public MyRecordBuilder p(T p) {
                this.p = p;
                return this;
            }
         */
        var parameterSpec = ParameterSpec.builder(component.typeName(), component.name()).build();
        var methodSpec = MethodSpec.methodBuilder(component.name())
                .addJavadoc("Set a new value for this record component in the builder\n")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterSpec)
                .returns(builderClassType.typeName())
                .addStatement("this.$L = $L", component.name(), component.name())
                .addStatement("return this")
                .build();
        builder.addMethod(methodSpec);
    }

    private void writeJavaFile(TypeElement record, String packageName, ClassType builderClassType, TypeSpec.Builder builder, RecordBuilderMetaData metaData) {
        // produces the Java file
        var javaFileBuilder = JavaFile.builder(packageName, builder.build())
                .indent(metaData.fileIndent());
        var comment = metaData.fileComment();
        if ((comment != null) && !comment.isEmpty()) {
            javaFileBuilder.addFileComment(comment);
        }
        JavaFile javaFile = javaFileBuilder.build();
        Filer filer = processingEnv.getFiler();
        try
        {
            String fullyQualifiedName = packageName.isEmpty() ? builderClassType.name() : (packageName + "." + builderClassType.name());
            JavaFileObject sourceFile = filer.createSourceFile(fullyQualifiedName);
            try ( Writer writer = sourceFile.openWriter() )
            {
                javaFile.writeTo(writer);
            }
        }
        catch ( IOException e )
        {
            String message = "Could not create source file";
            if ( e.getMessage() != null )
            {
                message = message + ": " + e.getMessage();
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, record);
        }
    }
}
