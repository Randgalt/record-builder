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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class IncludeHelper {
    private final boolean isValid;
    private final List<TypeElement> classTypeElements;
    private final Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues;

    IncludeHelper(ProcessingEnvironment processingEnv, Element element, AnnotationMirror annotationMirror,
            boolean packagesSupported) {
        annotationValues = processingEnv.getElementUtils().getElementValuesWithDefaults(annotationMirror);
        var value = ElementUtils.getAnnotationValue(annotationValues, "value");
        var classes = ElementUtils.getAnnotationValue(annotationValues, "classes");
        var packages = ElementUtils.getAnnotationValue(annotationValues, "packages");
        var isValid = true;
        var classTypeElements = new ArrayList<TypeElement>();
        if (value.isPresent() || classes.isPresent() || packages.isPresent()) {
            var valueList = value.map(ElementUtils::getAttributeTypeMirrorList).orElseGet(List::of);
            var classesList = classes.map(ElementUtils::getAttributeTypeMirrorList).orElseGet(List::of);
            var packagesList = packages.map(ElementUtils::getAttributeStringList).orElseGet(List::of);
            if (valueList.isEmpty() && classesList.isEmpty() && packagesList.isEmpty()) {
                if (packagesSupported) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "At least one of \"value\", \"classes\" or \"packages\" required", element);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "At least one of \"value\" or \"classes\" required", element);
                }
                isValid = false;
            }
            isValid = processList(processingEnv, isValid, element, valueList, classTypeElements);
            isValid = processList(processingEnv, isValid, element, classesList, classTypeElements);
            packages.ifPresent(annotationValue -> processPackages(processingEnv, classTypeElements, packagesList));
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not read attribute for annotation",
                    element);
            isValid = false;
        }
        this.isValid = isValid;
        this.classTypeElements = List.copyOf(classTypeElements);
    }

    Map<? extends ExecutableElement, ? extends AnnotationValue> getAnnotationValues() {
        return annotationValues;
    }

    boolean isValid() {
        return isValid;
    }

    List<TypeElement> getClassTypeElements() {
        return classTypeElements;
    }

    private boolean processList(ProcessingEnvironment processingEnv, boolean isValid, Element element,
            List<TypeMirror> list, ArrayList<TypeElement> classTypeElements) {
        for (var typeMirror : list) {
            TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
            if (typeElement == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Could not get element for: " + typeMirror, element);
                isValid = false;
            } else {
                classTypeElements.add(typeElement);
            }
        }
        return isValid;
    }

    private void processPackages(ProcessingEnvironment processingEnv, List<TypeElement> classTypeElements,
            List<String> packagesList) {
        for (var packageName : packagesList) {
            var packageElement = processingEnv.getElementUtils().getPackageElement(packageName);
            for (var child : packageElement.getEnclosedElements()) {
                if (child.getKind() == ElementKind.RECORD) {
                    classTypeElements.add((TypeElement) child);
                }
            }
        }
    }
}
