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
import io.soabase.recordbuilder.core.DefaultRecordBuilderMetaData;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SupportedAnnotationTypes("io.soabase.recordbuilder.core.RecordBuilder")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class RecordBuilderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation).forEach(element -> process(annotation, element, roundEnv)));
        return true;
    }

    private void process(TypeElement annotation, Element element, RoundEnvironment roundEnv) {
        if (element.getKind() != ElementKind.RECORD) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "RecordBuilder only valid for records.", element);
            return;
        }
        process(annotation, (TypeElement) element, roundEnv, new DefaultRecordBuilderMetaData());   // TODO DefaultRecordBuilderMetaData
    }

    private void process(TypeElement annotation, TypeElement record, RoundEnvironment roundEnv, RecordBuilderMetaData metaData) {
        var recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        var packageName = ElementUtils.getPackageName(record);
        var builderClassType = ElementUtils.getClassType(packageName, recordClassType.name() + metaData.suffix(), record.getTypeParameters());
        var typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        var recordComponents = record.getRecordComponents().stream().map(ElementUtils::getClassType).collect(Collectors.toList());

        var builder = TypeSpec.classBuilder(builderClassType.name()).addModifiers(Modifier.PUBLIC);
        builder.addTypeVariables(typeVariables);

        addDefaultConstructor(builder);
        addAllArgsConstructor(builder, recordComponents);
        addStaticDefaultBuilderMethod(builder, builderClassType, typeVariables, metaData);
        addStaticAllArgsBuilderMethod(builder, builderClassType, recordComponents, typeVariables, metaData);
        addStaticCopyMethod(builder, builderClassType, recordClassType, recordComponents, typeVariables, metaData);
        addBuildMethod(builder, recordClassType, recordComponents, metaData);
        recordComponents.forEach(component -> {
            add1Field(builder, component);
            add1SetterMethod(builder, component, builderClassType);
        });

        writeJavaFile(record, packageName, builderClassType, builder, metaData);
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
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(typeVariables)
                .addParameter(recordClassType.typeName(), "from")
                .returns(builderClassType.typeName())
                .addStatement(codeBuilder.build())
                .build();
        builder.addMethod(methodSpec);
    }

    private void addStaticAllArgsBuilderMethod(TypeSpec.Builder builder, ClassType builderClassType, List<ClassType> recordComponents, List<TypeVariableName> typeVariables, RecordBuilderMetaData metaData) {
        /*
            Adds an all-args builder method that pre-fills the builder with values similar to:

            public static MyRecordBuilder builder(int p1, T p2, ...) {
                return new MyRecordBuilder(p1, p2, ...);
            }
         */
        var methodSpecBuilder = MethodSpec.methodBuilder(metaData.builderMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(typeVariables)
                .returns(builderClassType.typeName());
        var codeBuilder = CodeBlock.builder().add("return new $T(", builderClassType.typeName());
        IntStream.range(0, recordComponents.size()).forEach(index -> {
            ClassType component = recordComponents.get(index);
            methodSpecBuilder.addParameter(component.typeName(), component.name());
            if (index > 0) {
                codeBuilder.add(", ");
            }
            codeBuilder.add("$L", component.name());
        });
        codeBuilder.add(")");
        methodSpecBuilder.addStatement(codeBuilder.build());
        builder.addMethod(methodSpecBuilder.build());
    }

    private void addStaticDefaultBuilderMethod(TypeSpec.Builder builder, ClassType builderClassType, List<TypeVariableName> typeVariables, RecordBuilderMetaData metaData) {
        /*
            Adds a the default builder method similar to:

            public static MyRecordBuilder builder() {
                return new MyRecordBuilder();
            }
         */
        var methodSpec = MethodSpec.methodBuilder(metaData.builderMethodName())
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
        JavaFile javaFile = JavaFile.builder(packageName, builder.build())
                .addFileComment(metaData.fileComment())
                .indent(metaData.fileIndent())
                .build();
        Filer filer = processingEnv.getFiler();
        try
        {
            JavaFileObject sourceFile = filer.createSourceFile(builderClassType.name());
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
