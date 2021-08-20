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

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.*;

import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;

class CollectionBuilderUtils {
    private final boolean enabled;
    private final String listShimName;
    private final String mapShimName;
    private final String setShimName;
    private final String collectionShimName;

    private boolean needsListShim;
    private boolean needsMapShim;
    private boolean needsSetShim;
    private boolean needsCollectionShim;

    private static final TypeName listType = TypeName.get(List.class);
    private static final TypeName mapType = TypeName.get(Map.class);
    private static final TypeName setType = TypeName.get(Set.class);
    private static final TypeName collectionType = TypeName.get(Collection.class);

    private static final TypeVariableName tType = TypeVariableName.get("T");
    private static final TypeVariableName kType = TypeVariableName.get("K");
    private static final TypeVariableName vType = TypeVariableName.get("V");
    private static final ParameterizedTypeName parameterizedListType = ParameterizedTypeName.get(ClassName.get(List.class), tType);
    private static final ParameterizedTypeName parameterizedMapType = ParameterizedTypeName.get(ClassName.get(Map.class), kType, vType);
    private static final ParameterizedTypeName parameterizedSetType = ParameterizedTypeName.get(ClassName.get(Set.class), tType);
    private static final ParameterizedTypeName parameterizedCollectionType = ParameterizedTypeName.get(ClassName.get(Collection.class), tType);

    CollectionBuilderUtils(List<RecordClassType> recordComponents, boolean enabled) {
        this.enabled = enabled;

        listShimName = adjustShimName(recordComponents, "__list", 0);
        mapShimName = adjustShimName(recordComponents, "__map", 0);
        setShimName = adjustShimName(recordComponents, "__set", 0);
        collectionShimName = adjustShimName(recordComponents, "__collection", 0);
    }

    void add(CodeBlock.Builder builder, RecordClassType component) {
        if (enabled) {
            if (component.rawTypeName().equals(listType)) {
                needsListShim = true;
                builder.add("$L($L)", listShimName, component.name());
            } else if (component.rawTypeName().equals(mapType)) {
                needsMapShim = true;
                builder.add("$L($L)", mapShimName, component.name());
            } else if (component.rawTypeName().equals(setType)) {
                needsSetShim = true;
                builder.add("$L($L)", setShimName, component.name());
            } else if (component.rawTypeName().equals(collectionType)) {
                needsCollectionShim = true;
                builder.add("$L($L)", collectionShimName, component.name());
            } else {
                builder.add("$L", component.name());
            }
        } else {
            builder.add("$L", component.name());
        }
    }

    void addShims(TypeSpec.Builder builder) {
        if (!enabled) {
            return;
        }

        if (needsListShim) {
            builder.addMethod(buildMethod(listShimName, listType, parameterizedListType, tType));
        }
        if (needsSetShim) {
            builder.addMethod(buildMethod(setShimName, setType, parameterizedSetType, tType));
        }
        if (needsMapShim) {
            builder.addMethod(buildMethod(mapShimName, mapType, parameterizedMapType, kType, vType));
        }
        if (needsCollectionShim) {
            builder.addMethod(buildCollectionsMethod());
        }
    }

    private String adjustShimName(List<RecordClassType> recordComponents, String baseName, int index)
    {
        var name = (index == 0) ? baseName : (baseName + index);
        if (recordComponents.stream().anyMatch(component -> component.name().equals(name))) {
            return adjustShimName(recordComponents, baseName, index + 1);
        }
        return name;
    }

    private MethodSpec buildMethod(String name, TypeName mainType, ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        var code = CodeBlock.of("return (o != null) ? $T.copyOf(o) : $T.of()", mainType, mainType);
        return MethodSpec.methodBuilder(name)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariables(Arrays.asList(typeVariables))
                .returns(parameterizedType)
                .addParameter(parameterizedType, "o")
                .addStatement(code)
                .build();
    }

    private MethodSpec buildCollectionsMethod() {
        var code = CodeBlock.builder()
                .add("if (o instanceof Set) {\n")
                .indent()
                .addStatement("return $T.copyOf(o)", setType)
                .unindent()
                .addStatement("}")
                .addStatement("return (o != null) ? $T.copyOf(o) : $T.of()", listType, listType)
                .build();
        return MethodSpec.methodBuilder(collectionShimName)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariable(tType)
                .returns(parameterizedCollectionType)
                .addParameter(parameterizedCollectionType, "o")
                .addCode(code)
                .build();
    }
}
