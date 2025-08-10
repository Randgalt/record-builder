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

import com.palantir.javapoet.CodeBlock;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.soabase.recordbuilder.processor.ElementUtils.getAnnotationValue;
import static io.soabase.recordbuilder.processor.ElementUtils.getStringAttribute;

class InitializerUtil {
    static Map<String, CodeBlock> detectInitializers(ProcessingEnvironment processingEnv, TypeElement record) {
        return record.getEnclosedElements().stream().flatMap(element -> {
            var annotation = ElementUtils.findAnnotationMirror(processingEnv, element,
                    RecordBuilder.Initializer.class.getName().replace("$", "."));
            var value = annotation.flatMap(mirror -> getAnnotationValue(mirror.getElementValues(), "value"));
            if (value.isEmpty()) {
                return Stream.of();
            }

            var source = annotation.flatMap(mirror -> getAnnotationValue(mirror.getElementValues(), "source"));

            var name = getStringAttribute(value.get(), "");
            var sourceElement = findSourceElement(processingEnv, record, source);

            Optional<CodeBlock> initializer = sourceElement.getEnclosedElements().stream()
                    .filter(enclosedElement -> enclosedElement.getSimpleName().toString().equals(name))
                    .flatMap(enclosedElement -> {
                        if ((enclosedElement.getKind() == ElementKind.METHOD)
                                && isValid(processingEnv, element, (ExecutableElement) enclosedElement)) {
                            return Stream.of(CodeBlock.builder().add("$T.$L()", sourceElement, name).build());
                        }

                        if ((enclosedElement.getKind() == ElementKind.FIELD)
                                && isValid(processingEnv, element, (VariableElement) enclosedElement)) {
                            return Stream.of(CodeBlock.builder().add("$T.$L", sourceElement, name).build());
                        }

                        return Stream.of();
                    }).findFirst();

            if (initializer.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "No matching public static field or method found for initializer named: " + name, element);
            }

            return initializer.map(codeBlock -> Map.entry(element.getSimpleName().toString(), codeBlock)).stream();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isValid(ProcessingEnvironment processingEnv, Element element,
            ExecutableElement executableElement) {
        if (executableElement.getModifiers().contains(Modifier.PUBLIC)
                && executableElement.getModifiers().contains(Modifier.STATIC)) {
            return processingEnv.getTypeUtils().isSameType(executableElement.getReturnType(), element.asType());
        }
        return false;
    }

    private static boolean isValid(ProcessingEnvironment processingEnv, Element element,
            VariableElement variableElement) {
        if (variableElement.getModifiers().contains(Modifier.PUBLIC)
                && variableElement.getModifiers().contains(Modifier.STATIC)
                && variableElement.getModifiers().contains(Modifier.FINAL)) {
            return processingEnv.getTypeUtils().isSameType(variableElement.asType(), element.asType());
        }
        return false;
    }

    private static TypeElement findSourceElement(ProcessingEnvironment processingEnv, TypeElement record,
            Optional<? extends AnnotationValue> source) {
        return source.flatMap(ElementUtils::getAttributeTypeMirror)
                .map(sourceMirror -> (TypeElement) processingEnv.getTypeUtils().asElement(sourceMirror)).orElse(record);
    }
}
