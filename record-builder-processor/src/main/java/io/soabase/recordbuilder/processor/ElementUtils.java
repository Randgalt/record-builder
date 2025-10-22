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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElementUtils {
    public static Optional<? extends AnnotationMirror> findAnnotationMirror(ProcessingEnvironment processingEnv,
            Element element, String annotationClass) {
        return processingEnv.getElementUtils().getAllAnnotationMirrors(element).stream()
                .filter(e -> e.getAnnotationType().toString().equals(annotationClass)).findFirst();
    }

    public static List<? extends AnnotationMirror> getAccessorAnnotations(ProcessingEnvironment processingEnv,
            RecordComponentElement component) {
        var accessorMirrors = component.getAccessor().getAnnotationMirrors();
        var typeMirrors = component.asType().getAnnotationMirrors().stream()
                .filter(typeMirror -> accessorMirrors.stream().noneMatch(accessorMirror -> processingEnv.getTypeUtils()
                        .isSameType(accessorMirror.getAnnotationType(), typeMirror.getAnnotationType())));
        return Stream.concat(accessorMirrors.stream(), typeMirrors).toList();
    }

    public static Optional<? extends AnnotationValue> getAnnotationValue(
            Map<? extends ExecutableElement, ? extends AnnotationValue> values, String name) {
        return values.entrySet().stream().filter(e -> e.getKey().getSimpleName().toString().equals(name))
                .map(Map.Entry::getValue).findFirst();
    }

    @SuppressWarnings("unchecked")
    public static List<TypeMirror> getAttributeTypeMirrorList(AnnotationValue attribute) {
        List<? extends AnnotationValue> values = (attribute != null)
                ? (List<? extends AnnotationValue>) attribute.getValue() : Collections.emptyList();
        return values.stream().map(v -> (TypeMirror) v.getValue()).collect(Collectors.toList());
    }

    public static Optional<TypeMirror> getAttributeTypeMirror(AnnotationValue attribute) {
        if (attribute == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((TypeMirror) attribute.getValue());
    }

    @SuppressWarnings("unchecked")
    public static List<String> getAttributeStringList(AnnotationValue attribute) {
        List<? extends AnnotationValue> values = (attribute != null)
                ? (List<? extends AnnotationValue>) attribute.getValue() : Collections.emptyList();
        return values.stream().map(v -> (String) v.getValue()).collect(Collectors.toList());
    }

    public static boolean getBooleanAttribute(AnnotationValue attribute) {
        Object value = (attribute != null) ? attribute.getValue() : null;
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return false;
    }

    public static String getStringAttribute(AnnotationValue attribute, String defaultValue) {
        Object value = (attribute != null) ? attribute.getValue() : null;
        if (value != null) {
            return String.valueOf(value);
        }
        return defaultValue;
    }

    public static String getPackageName(TypeElement typeElement) {
        while (typeElement.getNestingKind().isNested()) {
            Element enclosingElement = typeElement.getEnclosingElement();
            if (enclosingElement instanceof TypeElement) {
                typeElement = (TypeElement) enclosingElement;
            } else {
                break;
            }
        }
        String name = typeElement.getQualifiedName().toString();
        int index = name.lastIndexOf(".");
        return (index > -1) ? name.substring(0, index) : "";
    }

    public static ClassType getClassType(String packageName, String simpleName,
            List<? extends TypeParameterElement> typeParameters) {
        return getClassType(ClassName.get(packageName, simpleName), typeParameters);
    }

    public static ClassType getClassType(TypeElement typeElement, List<? extends TypeParameterElement> typeParameters) {
        return getClassType(ClassName.get(typeElement), typeParameters);
    }

    public static ClassType getClassType(ClassName builderClassName,
            List<? extends TypeParameterElement> typeParameters) {
        if (typeParameters.isEmpty()) {
            return new ClassType(builderClassName, builderClassName.simpleName());
        }
        TypeName[] typeNames = typeParameters.stream().map(TypeVariableName::get).toArray(TypeName[]::new);
        return new ClassType(ParameterizedTypeName.get(builderClassName, typeNames), builderClassName.simpleName());
    }

    public static ClassType getClassTypeFromNames(ClassName builderClassName,
            List<? extends TypeVariableName> typeVariableNames) {
        if (typeVariableNames.isEmpty()) {
            return new ClassType(builderClassName, builderClassName.simpleName());
        }
        TypeName[] typeNames = typeVariableNames.toArray(TypeName[]::new);
        return new ClassType(ParameterizedTypeName.get(builderClassName, typeNames), builderClassName.simpleName());
    }

    public static RecordClassType getRecordClassType(ProcessingEnvironment processingEnv,
            RecordComponentElement recordComponent, List<? extends AnnotationMirror> accessorAnnotations,
            List<? extends AnnotationMirror> canonicalConstructorAnnotations) {
        var typeName = TypeName.get(recordComponent.asType());
        var rawTypeName = TypeName.get(processingEnv.getTypeUtils().erasure(recordComponent.asType()));
        return new RecordClassType(recordComponent.asType().getKind(), typeName, rawTypeName,
                recordComponent.getSimpleName().toString(), recordComponent.getSimpleName().toString(),
                accessorAnnotations, canonicalConstructorAnnotations);
    }

    public static String getWithMethodName(ClassType component, String prefix) {
        var name = component.name();
        if (name.length() == 1) {
            return prefix + name.toUpperCase();
        }
        return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static String generateName(TypeElement element, ClassType classType, String suffix,
            boolean prefixEnclosingClassNames) {
        // generate the class name
        var baseName = classType.name() + suffix;
        return prefixEnclosingClassNames ? (getNamePrefix(element.getEnclosingElement()) + baseName) : baseName;
    }

    public static Optional<? extends Element> findCanonicalConstructor(TypeElement record) {
        if (record.getKind() != ElementKind.RECORD) {
            return Optional.empty();
        }

        // based on
        // https://github.com/openjdk/jdk/pull/3556/files#diff-a6270f4b50989abe733607c69038b2036306d13f77276af005d023b7fc57f1a2R2368
        var componentList = record.getRecordComponents().stream().map(e -> e.asType().toString())
                .collect(Collectors.toList());
        return record.getEnclosedElements().stream().filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .filter(element -> {
                    var parameters = ((ExecutableElement) element).getParameters();
                    var parametersList = parameters.stream().map(e -> e.asType().toString())
                            .collect(Collectors.toList());
                    return componentList.equals(parametersList);
                }).findFirst();
    }

    private static String getNamePrefix(Element element) {
        // prefix enclosing class names if nested in a class
        if (element instanceof TypeElement) {
            return getNamePrefix(element.getEnclosingElement()) + element.getSimpleName().toString();
        }
        return "";
    }

    public static RecordBuilder.Options getMetaData(ProcessingEnvironment processingEnv, Element element) {
        var recordSpecificMetaData = element.getAnnotation(RecordBuilder.Options.class);
        return (recordSpecificMetaData != null) ? recordSpecificMetaData
                : RecordBuilderOptions.build(processingEnv.getOptions());
    }

    private ElementUtils() {
    }
}
