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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.soabase.recordbuilder.core.RecordBuilderMetaData;
import io.soabase.recordbuilder.core.RecordInterface;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.function.Function;

import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.RECORD_BUILDER;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.RECORD_INTERFACE;

@SupportedAnnotationTypes({RECORD_BUILDER, RECORD_INTERFACE})
public class RecordBuilderProcessor extends AbstractProcessor {
    public static final String RECORD_BUILDER = "io.soabase.recordbuilder.core.RecordBuilder";
    public static final String RECORD_INTERFACE = "io.soabase.recordbuilder.core.RecordInterface";

    static final AnnotationSpec generatedRecordBuilderAnnotation = AnnotationSpec.builder(Generated.class).addMember("value", "$S", RECORD_BUILDER).build();
    static final AnnotationSpec generatedRecordInterfaceAnnotation = AnnotationSpec.builder(Generated.class).addMember("value", "$S", RECORD_INTERFACE).build();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation)
                .forEach(element -> process(annotation, element))
        );
        return true;
    }
    
    @Override
    public SourceVersion getSupportedSourceVersion() {
        // we don't directly return RELEASE_14 as that may 
        // not exist in prior releases
        // if we're running on an older release, returning latest()
        // is fine as we won't encounter any records anyway
        return SourceVersion.latest();
    }

    private void process(TypeElement annotation, Element element) {
        var metaData = new RecordBuilderMetaDataLoader(processingEnv, s -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, s)).getMetaData();

        if (annotation.getQualifiedName().toString().equals(RECORD_BUILDER)) {
            processRecordBuilder((TypeElement) element, metaData);
        } else if (annotation.getQualifiedName().toString().equals(RECORD_INTERFACE)) {
            processRecordInterface((TypeElement) element, element.getAnnotation(RecordInterface.class), metaData);
        } else {
            throw new RuntimeException("Unknown annotation: " + annotation);
        }
    }

    private void processRecordInterface(TypeElement element, RecordInterface recordInterface, RecordBuilderMetaData metaData) {
        if (!element.getKind().isInterface()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "RecordInterface only valid for interfaces.", element);
            return;
        }
        var internalProcessor = new InternalRecordInterfaceProcessor(processingEnv, element, recordInterface, metaData);
        if (!internalProcessor.isValid()) {
            return;
        }
        writeRecordInterfaceJavaFile(element, internalProcessor.packageName(), internalProcessor.recordClassType(), internalProcessor.recordType(), metaData, internalProcessor::toRecord);
    }

    private void processRecordBuilder(TypeElement record, RecordBuilderMetaData metaData) {
        // we use string based name comparison for the element kind,
        // as the ElementKind.RECORD enum doesn't exist on JRE releases
        // older than Java 14, and we don't want to throw unexpected
        // NoSuchFieldErrors
        if (!"RECORD".equals(record.getKind().name())) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "RecordBuilder only valid for records.", record);
            return;
        }
        var internalProcessor = new InternalRecordBuilderProcessor(record, metaData);
        writeRecordBuilderJavaFile(record, internalProcessor.packageName(), internalProcessor.builderClassType(), internalProcessor.builderType(), metaData);
    }

    private void writeRecordBuilderJavaFile(TypeElement record, String packageName, ClassType builderClassType, TypeSpec builderType, RecordBuilderMetaData metaData) {
        // produces the Java file
        JavaFile javaFile = javaFileBuilder(packageName, builderType, metaData);
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
            handleWriteError(record, e);
        }
    }

    private void writeRecordInterfaceJavaFile(TypeElement element, String packageName, ClassType classType, TypeSpec type, RecordBuilderMetaData metaData, Function<String, String> toRecordProc) {
        JavaFile javaFile = javaFileBuilder(packageName, type, metaData);

        String classSourceCode = javaFile.toString();
        int generatedIndex = classSourceCode.indexOf("@Generated");
        String recordSourceCode = toRecordProc.apply(classSourceCode);

        Filer filer = processingEnv.getFiler();
        try
        {
            String fullyQualifiedName = packageName.isEmpty() ? classType.name() : (packageName + "." + classType.name());
            JavaFileObject sourceFile = filer.createSourceFile(fullyQualifiedName);
            try ( Writer writer = sourceFile.openWriter() )
            {
                writer.write(recordSourceCode);
            }
        }
        catch ( IOException e )
        {
            handleWriteError(element, e);
        }
    }

    private JavaFile javaFileBuilder(String packageName, TypeSpec type, RecordBuilderMetaData metaData) {
        var javaFileBuilder = JavaFile.builder(packageName, type)
                .skipJavaLangImports(true)
                .indent(metaData.fileIndent());
        var comment = metaData.fileComment();
        if ((comment != null) && !comment.isEmpty()) {
            javaFileBuilder.addFileComment(comment);
        }
        return javaFileBuilder.build();
    }

    private void handleWriteError(TypeElement element, IOException e) {
        String message = "Could not create source file";
        if (e.getMessage() != null) {
            message = message + ": " + e.getMessage();
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
