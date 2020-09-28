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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.recordbuilder.core.RecordBuilderMetaData;

import javax.lang.model.element.Element;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.List;

public class ElementUtils {
    public static String getPackageName(TypeElement typeElement) {
        while (typeElement.getNestingKind().isNested()) {
            Element enclosingElement = typeElement.getEnclosingElement();
            if (enclosingElement instanceof TypeElement) {
                typeElement = (TypeElement) enclosingElement;
            } else {
                break;
            }
        }
        String name = typeElement.getEnclosingElement().toString();
        return !name.equals("unnamed package") ? name : "";
    }

    public static ClassType getClassType(String packageName, String simpleName, List<? extends TypeParameterElement> typeParameters) {
        return getClassType(ClassName.get(packageName, simpleName), typeParameters);
    }

    public static ClassType getClassType(TypeElement typeElement, List<? extends TypeParameterElement> typeParameters) {
        return getClassType(ClassName.get(typeElement), typeParameters);
    }

    public static ClassType getClassType(ClassName builderClassName, List<? extends TypeParameterElement> typeParameters) {
        if (typeParameters.isEmpty()) {
            return new ClassType(builderClassName, builderClassName.simpleName());
        }
        TypeName[] typeNames = typeParameters.stream().map(TypeVariableName::get).toArray(TypeName[]::new);
        return new ClassType(ParameterizedTypeName.get(builderClassName, typeNames), builderClassName.simpleName());
    }

    public static ClassType getClassType(RecordComponentElement recordComponent) {
        return new ClassType(TypeName.get(recordComponent.asType()), recordComponent.getSimpleName().toString());
    }

    public static String getWithMethodName(ClassType component, String prefix) {
        var name = component.name();
        if (name.length() == 1) {
            return prefix + name.toUpperCase();
        }
        return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static String getBuilderName(TypeElement element, RecordBuilderMetaData metaData, ClassType classType, String suffix) {
        // generate the class name
        var baseName = classType.name() + suffix;
        return metaData.prefixEnclosingClassNames() ? (getBuilderNamePrefix(element.getEnclosingElement()) + baseName) : baseName;
    }

    private static String getBuilderNamePrefix(Element element) {
        // prefix enclosing class names if nested in a class
        if (element instanceof TypeElement) {
            return getBuilderNamePrefix(element.getEnclosingElement()) + element.getSimpleName().toString();
        }
        return "";
    }

    private ElementUtils() {
    }
}
