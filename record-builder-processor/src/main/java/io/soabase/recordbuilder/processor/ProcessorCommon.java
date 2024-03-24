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

import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ProcessorCommon {
    private ProcessorCommon() {
    }

    static List<RecordClassType> buildRecordComponents(ProcessingEnvironment processingEnv, TypeElement record) {
        var accessorAnnotations = record.getRecordComponents().stream().map(e -> e.getAccessor().getAnnotationMirrors())
                .toList();
        var canonicalConstructorAnnotations = ElementUtils.findCanonicalConstructor(record)
                .map(constructor -> ((ExecutableElement) constructor).getParameters().stream()
                        .map(Element::getAnnotationMirrors).collect(Collectors.toList()))
                .orElse(List.of());
        var recordComponents = record.getRecordComponents();
        return IntStream.range(0, recordComponents.size()).mapToObj(index -> {
            var thisAccessorAnnotations = (accessorAnnotations.size() > index) ? accessorAnnotations.get(index)
                    : List.<AnnotationMirror> of();
            var thisCanonicalConstructorAnnotations = (canonicalConstructorAnnotations.size() > index)
                    ? canonicalConstructorAnnotations.get(index) : List.<AnnotationMirror> of();
            return ElementUtils.getRecordClassType(processingEnv, recordComponents.get(index), thisAccessorAnnotations,
                    thisCanonicalConstructorAnnotations);
        }).collect(Collectors.toList());
    }

    static void addVisibility(TypeSpec.Builder builder, boolean builderIsInRecordPackage, Set<Modifier> modifiers) {
        if (builderIsInRecordPackage) {
            if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PRIVATE)
                    || modifiers.contains(Modifier.PROTECTED)) {
                builder.addModifiers(Modifier.PUBLIC); // builders are top level classes - can only be public or
                // package-private
            }
            // is package-private
        } else {
            builder.addModifiers(Modifier.PUBLIC);
        }
    }
}
