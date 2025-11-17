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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

class Context {
    private final String sourceAnnotation;
    private final Element sourceElement;
    private final Optional<String> parentClass;
    private final String packageName;
    private final String className;
    private final boolean fromInclude;

    private Context(String sourceAnnotation, Element sourceElement, Optional<String> parentClass, String packageName,
            String className, boolean fromInclude) {
        this.sourceAnnotation = sourceAnnotation;
        this.sourceElement = sourceElement;
        this.parentClass = parentClass;
        this.packageName = packageName;
        this.className = className;
        this.fromInclude = fromInclude;
    }

    static Context forRecordBuilder(Element sourceElement, String packageName, ClassType classType) {
        return new Context("record-builder", sourceElement, Optional.empty(), packageName, classType.name(), false);
    }

    static Context forRecordBuilderInterface(Element sourceElement, String packageName, ClassType classType) {
        return new Context("record-builder-interface", sourceElement, Optional.empty(), packageName, classType.name(),
                false);
    }

    static Context forRecordBuilderDeconstructor(Element sourceElement, Optional<String> parentClass,
            String packageName, ClassType classType) {
        return new Context("record-builder-deconstructor", sourceElement, parentClass, packageName, classType.name(),
                false);
    }

    Context fromInclude(boolean fromInclude) {
        return new Context(sourceAnnotation, sourceElement, parentClass, packageName, className, fromInclude);
    }

    void writeContextFile(ProcessingEnvironment processingEnv) {
        String sourceElementValue = sourceElement(sourceElement);

        try {
            Properties properties = new Properties();
            properties.put("source-annotation", sourceAnnotation);
            properties.put("package", packageName);
            properties.put("source-element", sourceElementValue);
            properties.put("parent-element", parentClass.orElse(sourceElementValue));
            properties.put("processing-source-version", processingEnv.getSourceVersion().toString());
            properties.put("from-include", Boolean.toString(fromInclude));

            String packageSpec = packageName.isEmpty() ? "" : (packageName + "/");
            String fileName = String.join("/", "META-INF", RecordBuilder.class.getName(),
                    packageSpec + className + ".properties");
            FileObject fileObject = processingEnv.getFiler().createResource(CLASS_OUTPUT, "", fileName, sourceElement);
            try (OutputStream writer = fileObject.openOutputStream()) {
                properties.store(writer, "Soabase Record Builder Processor");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Error writing record builder file: " + e.getMessage(), sourceElement);
        }
    }

    private String sourceElement(Element element) {
        if (element.asType().getKind() == TypeKind.DECLARED) {
            return ((QualifiedNameable) element).getQualifiedName().toString();
        }

        Element enclosingElement = element.getEnclosingElement();
        if (enclosingElement != null) {
            return sourceElement(enclosingElement);
        }
        return element.getSimpleName().toString();
    }
}
