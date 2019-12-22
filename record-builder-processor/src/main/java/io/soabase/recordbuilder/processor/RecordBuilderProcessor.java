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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.soabase.recordbuilder.core.RecordBuilderMetaData;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.NAME;

@SupportedAnnotationTypes(NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class RecordBuilderProcessor extends AbstractProcessor {
    public static final String NAME = "io.soabase.recordbuilder.core.RecordBuilder";

    private static final AnnotationSpec generatedAnnotation = AnnotationSpec.builder(Generated.class).addMember("value", "$S", NAME).build();

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
        var internalProcessor = new InternalProcessor(record, metaData);
        writeJavaFile(record, internalProcessor.packageName(), internalProcessor.builderClassType(), internalProcessor.builderType(), metaData);
    }

    private void writeJavaFile(TypeElement record, String packageName, ClassType builderClassType, TypeSpec builderType, RecordBuilderMetaData metaData) {
        // produces the Java file
        var javaFileBuilder = JavaFile.builder(packageName, builderType)
                .skipJavaLangImports(true)
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
