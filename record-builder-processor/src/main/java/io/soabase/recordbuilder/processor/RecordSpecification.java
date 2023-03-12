/*
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

import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;

public record RecordSpecification(ClassType recordClassType, String builderName, String recordActualPackage, List<? extends TypeParameterElement> typeParameters, List<RecordClassType> recordComponents, Set<Modifier> modifiers) {
    public static RecordSpecification fromRecordTypeElement(ProcessingEnvironment processingEnv, RecordBuilder.Options metaData, TypeElement record) {
        var recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        var builderName = getBuilderName(record, metaData, recordClassType, metaData.suffix());
        var recordActualPackage = ElementUtils.getPackageName(record);
        var recordComponents = buildRecordComponents(processingEnv, record);
        return new RecordSpecification(recordClassType, builderName, recordActualPackage, record.getTypeParameters(), recordComponents, record.getModifiers());
    }

    public static RecordSpecification fromInterfaceProcessor(ProcessingEnvironment processingEnv, RecordBuilder.Options metaData, List<InternalRecordInterfaceProcessor.Component> recordComponents, ClassType recordClassType, TypeElement iface, String packageName) {
        List<RecordClassType> mappedRecordComponents = recordComponents.stream()
                .map(recordComponent -> {
                    var accessorAnnotations = recordComponent.element().getAnnotationMirrors();
                    return ElementUtils.getRecordClassType(processingEnv, recordComponent.element().getReturnType(), recordComponent.element().getSimpleName().toString(), accessorAnnotations, List.of());
                })
                .toList();
        var builderName = getBuilderName(recordClassType.name(), iface, metaData) + metaData.suffix();
        return new RecordSpecification(recordClassType, builderName, packageName, iface.getTypeParameters(), mappedRecordComponents, iface.getModifiers());
    }

    private static List<RecordClassType> buildRecordComponents(ProcessingEnvironment processingEnv, TypeElement record) {
        var accessorAnnotations = record.getRecordComponents().stream().map(e -> e.getAccessor().getAnnotationMirrors()).toList();
        var canonicalConstructorAnnotations = ElementUtils.findCanonicalConstructor(record).map(constructor -> ((ExecutableElement) constructor).getParameters().stream().map(Element::getAnnotationMirrors).collect(Collectors.toList())).orElse(List.of());
        var recordComponents = record.getRecordComponents();
        return IntStream.range(0, recordComponents.size())
                .mapToObj(index -> {
                    var thisAccessorAnnotations = (accessorAnnotations.size() > index) ? accessorAnnotations.get(index) : List.<AnnotationMirror>of();
                    var thisCanonicalConstructorAnnotations = (canonicalConstructorAnnotations.size() > index) ? canonicalConstructorAnnotations.get(index) : List.<AnnotationMirror>of();
                    RecordComponentElement recordComponentElement = recordComponents.get(index);
                    return ElementUtils.getRecordClassType(processingEnv, recordComponentElement.asType(), recordComponentElement.getSimpleName().toString(), thisAccessorAnnotations, thisCanonicalConstructorAnnotations);
                })
                .collect(Collectors.toList());
    }
}
