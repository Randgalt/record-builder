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

import io.soabase.recordbuilder.core.RecordBuilder;
import io.soabase.recordbuilder.core.RecordInterface;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;
import static io.soabase.recordbuilder.processor.ElementUtils.getMetaData;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.RECORD_INTERFACE;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.RECORD_INTERFACE_INCLUDE;

public class RecordBuilderCleaner extends AbstractProcessor {
    private static final Set<String> deletedSet = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<Boolean> results = annotations.stream()
                .flatMap(annotation -> roundEnv.getElementsAnnotatedWith(annotation).stream().map(element -> process(annotation, element)))
                .toList();
        return results.stream().allMatch(b -> b);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private boolean process(TypeElement annotation, Element element) {
        String annotationClass = annotation.getQualifiedName().toString();
        if (annotationClass.equals(RECORD_INTERFACE)) {
            var typeElement = (TypeElement) element;
            return processRecordInterface(typeElement, element.getAnnotation(RecordInterface.class).addRecordBuilder(),
                    getMetaData(processingEnv, typeElement), Optional.empty(), false);
        } else if (annotationClass.equals(RECORD_INTERFACE_INCLUDE)) {
            // processIncludes(element, getMetaData(processingEnv, element), annotationClass); TODO
        } else {
            var recordBuilderTemplate = annotation.getAnnotation(RecordBuilder.Template.class);
            if (recordBuilderTemplate != null) {
                if (recordBuilderTemplate.asRecordInterface()) {
                    return processRecordInterface((TypeElement) element, true, recordBuilderTemplate.options(),
                            Optional.empty(), true);
                }
            }
        }
        return false;
    }

    private boolean processRecordInterface(TypeElement element, boolean addRecordBuilder, RecordBuilder.Options metaData,
            Optional<String> packageName, boolean fromTemplate) {
        ClassType ifaceClassType = ElementUtils.getClassType(element, element.getTypeParameters());
        String actualPackageName = packageName.orElseGet(() -> ElementUtils.getPackageName(element));
        getBuilderName(element, metaData, ifaceClassType, metaData.interfaceSuffix());

        boolean b1 = deletePossibleClassFile(actualPackageName, ifaceClassType.name() + metaData.interfaceSuffix(),
                StandardLocation.SOURCE_OUTPUT);
        boolean b2 = deletePossibleClassFile(actualPackageName,
                ifaceClassType.name() + metaData.interfaceSuffix() + metaData.suffix(), StandardLocation.SOURCE_OUTPUT);
        boolean b3 = deletePossibleClassFile(actualPackageName, ifaceClassType.name() + metaData.interfaceSuffix(), StandardLocation.CLASS_OUTPUT);
        boolean b4 = deletePossibleClassFile(actualPackageName, ifaceClassType.name() + metaData.interfaceSuffix() + metaData.suffix(), StandardLocation.CLASS_OUTPUT);

        return b1 || b2 || b3 || b4;
    }

    private boolean deletePossibleClassFile(String packageName, String className, StandardLocation location) {
        String extension = (location == StandardLocation.CLASS_OUTPUT) ? ".class" : ".java";

        if (!deletedSet.add(packageName + "." + className + extension)) {
            return false;
        }

        try {
            FileObject resource = processingEnv.getFiler().getResource(location, packageName, className + extension);
            File file = new File(resource.toUri());
            System.err.println("XXXX Cleaner: Exists: %s - File %s".formatted(file.exists(), file));
            if (file.exists()) {
                if (!file.delete()) {
                    System.err.println("Could not delete existing: Exists: %s - File %s".formatted(file.exists(), file));
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Could not delete existing class file: %s".formatted(file));
                    return false;
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            // ignore
        }
        return false;
    }
}
