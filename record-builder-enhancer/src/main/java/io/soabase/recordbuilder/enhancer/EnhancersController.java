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
package io.soabase.recordbuilder.enhancer;

import io.soabase.recordbuilder.enhancer.spi.RecordBuilderEnhancer;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class EnhancersController {
    private final Map<String, Optional<RecordBuilderEnhancer>> enhancers = new ConcurrentHashMap<>();

    record EnhancerAndArgs(RecordBuilderEnhancer enhancer, List<String> arguments) {}

    List<EnhancerAndArgs> getEnhancers(ProcessorImpl processor, TypeElement typeElement) {
        return internalGetEnhancers(processor, typeElement).flatMap(spec -> toEnhancer(processor, spec)).toList();
    }

    private Stream<EnhancerSpec> internalGetEnhancers(ProcessorImpl processor, TypeElement typeElement) {
        Optional<? extends AnnotationMirror> recordBuilderEnhance = getAnnotationMirror(processor, typeElement);
        Optional<? extends AnnotationMirror> recordBuilderEnhanceTemplate = getTemplateAnnotationMirror(processor, typeElement);
        if (recordBuilderEnhance.isPresent() && recordBuilderEnhanceTemplate.isPresent()) {
            processor.logError("RecordBuilderEnhance and RecordBuilderEnhance.Template cannot be combined.");
            return Stream.of();
        }
        return Stream.concat(recordBuilderEnhance.stream().flatMap(this::getEnhancersAnnotationValue), recordBuilderEnhanceTemplate.stream().flatMap(this::getEnhancersAnnotationValue));
    }

    private Map<String, Object> getAnnotationValueMap(AnnotationMirror annotationMirror)
    {
        return annotationMirror.getElementValues().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), entry -> entry.getValue().getValue()));
    }

    private record EnhancerSpec(String enhancerClass, List<String> arguments) {}

    @SuppressWarnings("unchecked")
    private Stream<EnhancerSpec> getEnhancersAnnotationValue(AnnotationMirror annotationMirror)
    {
        Map<String, Object> annotationValueMap = getAnnotationValueMap(annotationMirror);

        List<? extends AnnotationValue> argumentsValue = (List<? extends AnnotationValue>) annotationValueMap.getOrDefault("arguments", List.of()); // list of RecordBuilderEnhanceArguments mirrors
        Map<String, List<String>> argumentsMap = argumentsValue.stream()
                .flatMap(argumentMirror -> {
                    Map<String, Object> argumentMap = getAnnotationValueMap((AnnotationMirror) argumentMirror.getValue());
                    Object enhancer = argumentMap.get("enhancer");
                    Object arguments = argumentMap.get("arguments");
                    if ((enhancer != null) && (arguments != null)) {
                        List<String> argumentList = ((List<? extends AnnotationValue>) arguments).stream().map(value -> value.getValue().toString()).toList();
                        return Stream.of(Map.entry(enhancer.toString(), argumentList));
                    }
                    return Stream.of();
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<? extends AnnotationValue> enhancers = (List<? extends AnnotationValue>) annotationValueMap.get("enhancers");
        if (enhancers != null) {
            return enhancers.stream().map(annotationValue -> {
                TypeMirror typeMirror = (TypeMirror) annotationValue.getValue();
                return new EnhancerSpec(typeMirror.toString(), argumentsMap.getOrDefault(typeMirror.toString(), List.of()));
            });
        }
        return Stream.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ? extends AnnotationValue> getArgumentsAnnotations(AnnotationMirror annotationMirror)
    {
        return annotationMirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().contentEquals("arguments"))
                .flatMap(entry -> ((List<? extends AnnotationValue>) entry.getValue().getValue()).stream())
                .flatMap(annotationValue -> ((AnnotationMirror) annotationValue.getValue()).getElementValues().entrySet().stream()) // now as RecordBuilderEnhanceArguments

                .collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), Map.Entry::getValue));
    }

    private Optional<? extends AnnotationMirror> getAnnotationMirror(ProcessorImpl processor, TypeElement typeElement) {
        return processor.elements().getAllAnnotationMirrors(typeElement).stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(adjustedClassName(RecordBuilderEnhance.class)))
                .findFirst();
    }

    private Optional<? extends AnnotationMirror> getTemplateAnnotationMirror(ProcessorImpl processor, TypeElement typeElement) {
        return processor.elements().getAllAnnotationMirrors(typeElement).stream()
                .flatMap(annotationMirror -> processor.elements().getAllAnnotationMirrors(annotationMirror.getAnnotationType().asElement()).stream())
                .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(adjustedClassName(RecordBuilderEnhance.Template.class)))
                .findFirst();
    }

    private Stream<EnhancerAndArgs> toEnhancer(ProcessorImpl processor, EnhancerSpec spec)
    {
        return enhancers.computeIfAbsent(spec.enhancerClass(), __ -> newEnhancer(processor, spec.enhancerClass()))
                .stream()
                .map(enhancer -> new EnhancerAndArgs(enhancer, spec.arguments()));
    }

    private Optional<RecordBuilderEnhancer> newEnhancer(ProcessorImpl processor, String enhancerClass)
    {
        try {
            Class<?> clazz = Class.forName(enhancerClass, true, RecordBuilderEnhancerPlugin.class.getClassLoader());
            Object enhancer = clazz.getConstructor().newInstance();
            return Optional.of((RecordBuilderEnhancer) enhancer);
        } catch (Exception e) {
            processor.logError("Could not create enhancer instance. type=%s exception=%s message=%s".formatted(enhancerClass, e.getClass().getSimpleName(), e.getMessage()));
            return Optional.empty();
        }
    }

    private static String adjustedClassName(Class<?> clazz) {
        return clazz.getName().replace('$', '.');
    }
}
