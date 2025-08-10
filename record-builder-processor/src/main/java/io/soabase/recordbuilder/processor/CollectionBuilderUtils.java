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

import com.palantir.javapoet.*;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.lang.model.element.Modifier;
import java.io.Serial;
import java.util.*;
import java.util.regex.Pattern;

import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.recordBuilderGeneratedAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.suppressWarningsAnnotation;

class CollectionBuilderUtils {
    private final boolean useImmutableCollections;
    private final boolean useUnmodifiableCollections;
    private final boolean allowNullableCollections;
    private final boolean addSingleItemCollectionBuilders;
    private final boolean addClassRetainedGenerated;

    private final boolean interpretNotNulls;
    private final Pattern notNullPattern;

    private final String listShimName;
    private final String mapShimName;
    private final String setShimName;
    private final String collectionShimName;

    private final String nullableListShimName;
    private final String nullableMapShimName;
    private final String nullableSetShimName;
    private final String nullableCollectionShimName;

    private final String listMakerMethodName;
    private final String mapMakerMethodName;
    private final String setMakerMethodName;

    private boolean needsListShim;
    private boolean needsMapShim;
    private boolean needsSetShim;
    private boolean needsCollectionShim;

    private boolean needsNullableListShim;
    private boolean needsNullableMapShim;
    private boolean needsNullableSetShim;
    private boolean needsNullableCollectionShim;

    private boolean needsListMutableMaker;
    private boolean needsMapMutableMaker;
    private boolean needsSetMutableMaker;

    private static final Class<?> listType = List.class;
    private static final Class<?> mapType = Map.class;
    private static final Class<?> setType = Set.class;
    private static final Class<?> collectionType = Collection.class;
    private static final Class<?> collectionsType = Collections.class;
    private static final TypeName listTypeName = TypeName.get(listType);
    private static final TypeName mapTypeName = TypeName.get(mapType);
    private static final TypeName setTypeName = TypeName.get(setType);
    private static final TypeName collectionTypeName = TypeName.get(collectionType);
    private static final TypeName collectionsTypeName = TypeName.get(collectionsType);

    private static final TypeVariableName tType = TypeVariableName.get("T");
    private static final TypeVariableName kType = TypeVariableName.get("K");
    private static final TypeVariableName vType = TypeVariableName.get("V");
    private static final ParameterizedTypeName parameterizedListType = ParameterizedTypeName
            .get(ClassName.get(List.class), tType);
    private static final ParameterizedTypeName parameterizedMapType = ParameterizedTypeName
            .get(ClassName.get(Map.class), kType, vType);
    private static final ParameterizedTypeName parameterizedSetType = ParameterizedTypeName
            .get(ClassName.get(Set.class), tType);
    private static final ParameterizedTypeName parameterizedCollectionType = ParameterizedTypeName
            .get(ClassName.get(Collection.class), tType);

    private static final Class<?> mutableListType = ArrayList.class;
    private static final Class<?> mutableMapType = HashMap.class;
    private static final Class<?> mutableSetType = HashSet.class;
    private static final ClassName mutableListTypeName = ClassName.get(mutableListType);
    private static final ClassName mutableMapTypeName = ClassName.get(mutableMapType);
    private static final ClassName mutableSetTypeName = ClassName.get(mutableSetType);
    private final TypeSpec mutableListSpec;
    private final TypeSpec mutableSetSpec;
    private final TypeSpec mutableMapSpec;

    CollectionBuilderUtils(List<RecordClassType> recordComponents, RecordBuilder.Options metaData) {
        useImmutableCollections = metaData.useImmutableCollections();
        useUnmodifiableCollections = !useImmutableCollections && metaData.useUnmodifiableCollections();
        allowNullableCollections = metaData.allowNullableCollections();
        addSingleItemCollectionBuilders = metaData.addSingleItemCollectionBuilders();
        addClassRetainedGenerated = metaData.addClassRetainedGenerated();

        interpretNotNulls = metaData.interpretNotNulls();
        notNullPattern = Pattern.compile(metaData.interpretNotNullsPattern());

        listShimName = disambiguateGeneratedMethodName(recordComponents, "__list", 0);
        mapShimName = disambiguateGeneratedMethodName(recordComponents, "__map", 0);
        setShimName = disambiguateGeneratedMethodName(recordComponents, "__set", 0);
        collectionShimName = disambiguateGeneratedMethodName(recordComponents, "__collection", 0);

        nullableListShimName = disambiguateGeneratedMethodName(recordComponents, "__nullableList", 0);
        nullableMapShimName = disambiguateGeneratedMethodName(recordComponents, "__nullableMap", 0);
        nullableSetShimName = disambiguateGeneratedMethodName(recordComponents, "__nullableSet", 0);
        nullableCollectionShimName = disambiguateGeneratedMethodName(recordComponents, "__nullableCollection", 0);

        listMakerMethodName = disambiguateGeneratedMethodName(recordComponents, "__ensureListMutable", 0);
        setMakerMethodName = disambiguateGeneratedMethodName(recordComponents, "__ensureSetMutable", 0);
        mapMakerMethodName = disambiguateGeneratedMethodName(recordComponents, "__ensureMapMutable", 0);

        mutableListSpec = buildMutableCollectionSubType(metaData.mutableListClassName(), mutableListTypeName,
                parameterizedListType, tType);
        mutableSetSpec = buildMutableCollectionSubType(metaData.mutableSetClassName(), mutableSetTypeName,
                parameterizedSetType, tType);
        mutableMapSpec = buildMutableCollectionSubType(metaData.mutableMapClassName(), mutableMapTypeName,
                parameterizedMapType, kType, vType);
    }

    enum SingleItemsMetaDataMode {
        STANDARD, STANDARD_FOR_SETTER, EXCLUDE_WILDCARD_TYPES
    }

    record SingleItemsMetaData(Class<?> singleItemCollectionClass, List<TypeName> typeArguments, TypeName wildType) {
    }

    Optional<SingleItemsMetaData> singleItemsMetaData(RecordClassType component, SingleItemsMetaDataMode mode) {
        if (addSingleItemCollectionBuilders
                && (component.typeName() instanceof ParameterizedTypeName parameterizedTypeName)) {
            Class<?> collectionClass = null;
            ClassName wildcardClass = null;
            int typeArgumentQty = 0;
            if (isList(component)) {
                collectionClass = mutableListType;
                wildcardClass = ClassName.get(Collection.class);
                typeArgumentQty = 1;
            } else if (isSet(component)) {
                collectionClass = mutableSetType;
                wildcardClass = ClassName.get(Collection.class);
                typeArgumentQty = 1;
            } else if (isMap(component)) {
                collectionClass = mutableMapType;
                wildcardClass = (ClassName) component.rawTypeName();
                typeArgumentQty = 2;
            }
            var hasWildcardTypeArguments = hasWildcardTypeArguments(parameterizedTypeName, typeArgumentQty);
            if (collectionClass != null) {
                return switch (mode) {
                case STANDARD -> singleItemsMetaDataWithWildType(parameterizedTypeName, collectionClass, wildcardClass,
                        typeArgumentQty);

                case STANDARD_FOR_SETTER -> {
                    if (hasWildcardTypeArguments) {
                        yield Optional.of(new SingleItemsMetaData(collectionClass,
                                parameterizedTypeName.typeArguments(), component.typeName()));
                    }
                    yield singleItemsMetaDataWithWildType(parameterizedTypeName, collectionClass, wildcardClass,
                            typeArgumentQty);
                }

                case EXCLUDE_WILDCARD_TYPES -> {
                    if (hasWildcardTypeArguments) {
                        yield Optional.empty();
                    }
                    yield singleItemsMetaDataWithWildType(parameterizedTypeName, collectionClass, wildcardClass,
                            typeArgumentQty);
                }
                };
            }
        }
        return Optional.empty();
    }

    boolean isImmutableCollection(RecordClassType component) {
        return (useImmutableCollections || useUnmodifiableCollections)
                && (isList(component) || isMap(component) || isSet(component) || isCollection(component));
    }

    boolean isNullableCollection(RecordClassType component) {
        return allowNullableCollections && (!interpretNotNulls || !isNotNullAnnotated(component))
                && (isList(component) || isMap(component) || isSet(component) || isCollection(component));
    }

    private boolean isNotNullAnnotated(RecordClassType component) {
        return component.getCanonicalConstructorAnnotations().stream().anyMatch(annotation -> notNullPattern
                .matcher(annotation.getAnnotationType().asElement().getSimpleName().toString()).matches());
    }

    boolean isList(RecordClassType component) {
        return component.rawTypeName().equals(listTypeName);
    }

    boolean isMap(RecordClassType component) {
        return component.rawTypeName().equals(mapTypeName);
    }

    boolean isSet(RecordClassType component) {
        return component.rawTypeName().equals(setTypeName);
    }

    private boolean isCollection(RecordClassType component) {
        return component.rawTypeName().equals(collectionTypeName);
    }

    void addShimCall(CodeBlock.Builder builder, RecordClassType component) {
        if (useImmutableCollections || useUnmodifiableCollections) {
            if (isList(component)) {
                if (isNullableCollection(component)) {
                    needsNullableListShim = true;
                    builder.add("$L($L)", nullableListShimName, component.name());
                } else {
                    needsListShim = true;
                    needsListMutableMaker = true;
                    builder.add("$L($L)", listShimName, component.name());
                }
            } else if (isMap(component)) {
                if (isNullableCollection(component)) {
                    needsNullableMapShim = true;
                    builder.add("$L($L)", nullableMapShimName, component.name());
                } else {
                    needsMapShim = true;
                    needsMapMutableMaker = true;
                    builder.add("$L($L)", mapShimName, component.name());
                }
            } else if (isSet(component)) {
                if (isNullableCollection(component)) {
                    needsNullableSetShim = true;
                    builder.add("$L($L)", nullableSetShimName, component.name());
                } else {
                    needsSetShim = true;
                    needsSetMutableMaker = true;
                    builder.add("$L($L)", setShimName, component.name());
                }
            } else if (isCollection(component)) {
                if (isNullableCollection(component)) {
                    needsNullableCollectionShim = true;
                    builder.add("$L($L)", nullableCollectionShimName, component.name());
                } else {
                    needsCollectionShim = true;
                    builder.add("$L($L)", collectionShimName, component.name());
                }
            } else {
                builder.add("$L", component.name());
            }
        } else {
            builder.add("$L", component.name());
        }
    }

    String shimName(RecordClassType component) {
        if (isList(component)) {
            return isNullableCollection(component) ? nullableListShimName : listShimName;
        } else if (isMap(component)) {
            return isNullableCollection(component) ? nullableMapShimName : mapShimName;
        } else if (isSet(component)) {
            return isNullableCollection(component) ? nullableSetShimName : setShimName;
        } else if (isCollection(component)) {
            return isNullableCollection(component) ? nullableCollectionShimName : collectionShimName;
        } else {
            throw new IllegalArgumentException(component + " is not a supported collection type");
        }
    }

    String mutableMakerName(RecordClassType component) {
        if (isList(component)) {
            return listMakerMethodName;
        } else if (isMap(component)) {
            return mapMakerMethodName;
        } else if (isSet(component)) {
            return setMakerMethodName;
        } else {
            throw new IllegalArgumentException(component + " is not a supported collection type");
        }
    }

    void addShims(TypeSpec.Builder builder) {
        if (!useImmutableCollections && !useUnmodifiableCollections) {
            return;
        }

        if (needsListShim) {
            builder.addMethod(
                    buildShimMethod(listShimName, listTypeName, collectionType, parameterizedListType, tType));
        }
        if (needsNullableListShim) {
            builder.addMethod(buildNullableShimMethod(nullableListShimName, listTypeName, collectionType,
                    parameterizedListType, tType));
        }

        if (needsSetShim) {
            builder.addMethod(buildShimMethod(setShimName, setTypeName, collectionType, parameterizedSetType, tType));
        }
        if (needsNullableSetShim) {
            builder.addMethod(buildNullableShimMethod(nullableSetShimName, setTypeName, collectionType,
                    parameterizedSetType, tType));
        }

        if (needsMapShim) {
            builder.addMethod(buildShimMethod(mapShimName, mapTypeName, mapType, parameterizedMapType, kType, vType));
        }
        if (needsNullableMapShim) {
            builder.addMethod(buildNullableShimMethod(nullableMapShimName, mapTypeName, mapType, parameterizedMapType,
                    kType, vType));
        }

        if (needsCollectionShim) {
            builder.addMethod(buildCollectionsShimMethod());
        }
        if (needsNullableCollectionShim) {
            builder.addMethod(buildNullableCollectionsShimMethod());
        }
    }

    void addMutableMakers(TypeSpec.Builder builder) {
        if (!useImmutableCollections && !useUnmodifiableCollections) {
            return;
        }

        if (needsListMutableMaker) {
            builder.addMethod(
                    buildMutableMakerMethod(listMakerMethodName, mutableListSpec.name(), parameterizedListType, tType));
            builder.addType(mutableListSpec);
        }
        if (needsSetMutableMaker) {
            builder.addMethod(
                    buildMutableMakerMethod(setMakerMethodName, mutableSetSpec.name(), parameterizedSetType, tType));
            builder.addType(mutableSetSpec);
        }
        if (needsMapMutableMaker) {
            builder.addMethod(buildMutableMakerMethod(mapMakerMethodName, mutableMapSpec.name(), parameterizedMapType,
                    kType, vType));
            builder.addType(mutableMapSpec);
        }
    }

    private Optional<SingleItemsMetaData> singleItemsMetaDataWithWildType(ParameterizedTypeName parameterizedTypeName,
            Class<?> collectionClass, ClassName wildcardClass, int typeArgumentQty) {
        TypeName wildType;
        if (typeArgumentQty == 1) {
            wildType = ParameterizedTypeName.get(wildcardClass,
                    WildcardTypeName.subtypeOf(parameterizedTypeName.typeArguments().get(0)));
        } else { // if (typeArgumentQty == 2)
            wildType = ParameterizedTypeName.get(wildcardClass,
                    WildcardTypeName.subtypeOf(parameterizedTypeName.typeArguments().get(0)),
                    WildcardTypeName.subtypeOf(parameterizedTypeName.typeArguments().get(1)));
        }
        return Optional.of(new SingleItemsMetaData(collectionClass, parameterizedTypeName.typeArguments(), wildType));
    }

    private boolean hasWildcardTypeArguments(ParameterizedTypeName parameterizedTypeName, int argumentCount) {
        for (int i = 0; i < argumentCount; ++i) {
            if (parameterizedTypeName.typeArguments().size() > i) {
                if (parameterizedTypeName.typeArguments().get(i) instanceof WildcardTypeName) {
                    return true;
                }
            }
        }
        return false;
    }

    private String disambiguateGeneratedMethodName(List<RecordClassType> recordComponents, String baseName, int index) {
        var name = (index == 0) ? baseName : (baseName + index);
        if (recordComponents.stream().anyMatch(component -> component.name().equals(name))) {
            return disambiguateGeneratedMethodName(recordComponents, baseName, index + 1);
        }
        return name;
    }

    private MethodSpec buildShimMethod(String name, TypeName mainType, Class<?> abstractType,
            ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        var code = buildShimMethodBody(mainType, parameterizedType);

        TypeName[] wildCardTypeArguments = parameterizedType.typeArguments().stream().map(WildcardTypeName::subtypeOf)
                .toList().toArray(new TypeName[0]);
        var extendedParameterizedType = ParameterizedTypeName.get(ClassName.get(abstractType), wildCardTypeArguments);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name);
        if (!useImmutableCollections) {
            methodBuilder.addAnnotation(suppressWarningsAnnotation);
        }
        return methodBuilder.addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC).addTypeVariables(Arrays.asList(typeVariables))
                .returns(parameterizedType).addParameter(extendedParameterizedType, "o").addStatement(code).build();
    }

    private CodeBlock buildShimMethodBody(TypeName mainType, ParameterizedTypeName parameterizedType) {
        if (!useUnmodifiableCollections) {
            return CodeBlock.of("return (o != null) ? $T.copyOf(o) : $T.of()", mainType, mainType);
        }

        if (mainType.equals(listTypeName)) {
            return CodeBlock.of("return (o != null) ?  $T.<$T>unmodifiableList(($T) o) : $T.<$T>emptyList()",
                    collectionsTypeName, tType, parameterizedType, collectionsTypeName, tType);
        }

        if (mainType.equals(setTypeName)) {
            return CodeBlock.of("return (o != null) ?  $T.<$T>unmodifiableSet(($T) o) : $T.<$T>emptySet()",
                    collectionsTypeName, tType, parameterizedType, collectionsTypeName, tType);
        }

        if (mainType.equals(mapTypeName)) {
            return CodeBlock.of("return (o != null) ?  $T.<$T, $T>unmodifiableMap(($T) o) : $T.<$T, $T>emptyMap()",
                    collectionsTypeName, kType, vType, parameterizedType, collectionsTypeName, kType, vType);
        }

        throw new IllegalStateException("Cannot build shim method for " + mainType);
    }

    private MethodSpec buildNullableShimMethod(String name, TypeName mainType, Class<?> abstractType,
            ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        var code = buildNullableShimMethodBody(mainType, parameterizedType);

        TypeName[] wildCardTypeArguments = parameterizedType.typeArguments().stream().map(WildcardTypeName::subtypeOf)
                .toList().toArray(new TypeName[0]);
        var extendedParameterizedType = ParameterizedTypeName.get(ClassName.get(abstractType), wildCardTypeArguments);
        return MethodSpec.methodBuilder(name).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC).addTypeVariables(Arrays.asList(typeVariables))
                .returns(parameterizedType).addParameter(extendedParameterizedType, "o").addStatement(code).build();
    }

    private CodeBlock buildNullableShimMethodBody(TypeName mainType, ParameterizedTypeName parameterizedType) {
        if (!useUnmodifiableCollections) {
            return CodeBlock.of("return (o != null) ? $T.copyOf(o) : null", mainType);
        }

        if (mainType.equals(listTypeName)) {
            return CodeBlock.of("return (o != null) ?  $T.<$T>unmodifiableList(($T) o) : null", collectionsTypeName,
                    tType, parameterizedType);
        }

        if (mainType.equals(setTypeName)) {
            return CodeBlock.of("return (o != null) ?  $T.<$T>unmodifiableSet(($T) o) : null", collectionsTypeName,
                    tType, parameterizedType);
        }

        if (mainType.equals(mapTypeName)) {
            return CodeBlock.of("return (o != null) ?  $T.<$T>unmodifiableMap(($T) o) : null", collectionsTypeName,
                    tType, parameterizedType);
        }

        throw new IllegalStateException("Cannot build shim method for " + mainType);
    }

    private MethodSpec buildMutableMakerMethod(String name, String mutableCollectionType,
            ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        var nullCase = CodeBlock.of("if (o == null) return new $L<>()", mutableCollectionType);
        var isMutableCase = CodeBlock.of("if (o instanceof $L) return o", mutableCollectionType);
        var defaultCase = CodeBlock.of("return new $L<>(o)", mutableCollectionType);
        return MethodSpec.methodBuilder(name).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC).addTypeVariables(Arrays.asList(typeVariables))
                .returns(parameterizedType).addParameter(parameterizedType, "o").addStatement(nullCase)
                .addStatement(isMutableCase).addStatement(defaultCase).build();
    }

    private TypeSpec buildMutableCollectionSubType(String className, ClassName mutableCollectionType,
            ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        TypeName[] typeArguments = new TypeName[] {};
        typeArguments = Arrays.stream(typeVariables).toList().toArray(typeArguments);

        TypeSpec.Builder builder = TypeSpec.classBuilder(className).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(ParameterizedTypeName.get(mutableCollectionType, typeArguments))
                .addTypeVariables(Arrays.asList(typeVariables))
                .addField(FieldSpec
                        .builder(TypeName.LONG, "serialVersionUID", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addAnnotation(Serial.class).initializer("1L").build())
                .addMethod(MethodSpec.constructorBuilder().addAnnotation(generatedRecordBuilderAnnotation)
                        .addStatement("super()").build())
                .addMethod(MethodSpec.constructorBuilder().addAnnotation(generatedRecordBuilderAnnotation)
                        .addParameter(parameterizedType, "o").addStatement("super(o)").build());

        if (addClassRetainedGenerated) {
            builder.addAnnotation(recordBuilderGeneratedAnnotation);
        }

        return builder.build();
    }

    private MethodSpec buildCollectionsShimMethod() {
        var code = buildCollectionShimMethodBody();
        return MethodSpec.methodBuilder(collectionShimName).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC).addTypeVariable(tType)
                .returns(parameterizedCollectionType).addParameter(parameterizedCollectionType, "o").addCode(code)
                .build();
    }

    private CodeBlock buildCollectionShimMethodBody() {
        if (!useUnmodifiableCollections) {
            return CodeBlock.builder().add("if (o instanceof Set) {\n").indent()
                    .addStatement("return $T.copyOf(o)", setTypeName).unindent().addStatement("}")
                    .addStatement("return (o != null) ? $T.copyOf(o) : $T.of()", listTypeName, listTypeName).build();
        }

        return CodeBlock.builder().beginControlFlow("if (o instanceof $T)", listType)
                .addStatement("return $T.<$T>unmodifiableList(($T) o)", collectionsTypeName, tType,
                        parameterizedListType)
                .endControlFlow().beginControlFlow("if (o instanceof $T)", setType)
                .addStatement("return $T.<$T>unmodifiableSet(($T) o)", collectionsTypeName, tType, parameterizedSetType)
                .endControlFlow().addStatement("return $T.<$T>emptyList()", collectionsTypeName, tType).build();
    }

    private MethodSpec buildNullableCollectionsShimMethod() {
        var code = buildNullableCollectionShimMethodBody();

        return MethodSpec.methodBuilder(nullableCollectionShimName).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC).addTypeVariable(tType)
                .returns(parameterizedCollectionType).addParameter(parameterizedCollectionType, "o").addCode(code)
                .build();
    }

    private CodeBlock buildNullableCollectionShimMethodBody() {
        if (!useUnmodifiableCollections) {
            return CodeBlock.builder().add("if (o instanceof Set) {\n").indent()
                    .addStatement("return $T.copyOf(o)", setTypeName).unindent().addStatement("}")
                    .addStatement("return (o != null) ? $T.copyOf(o) : null", listTypeName).build();
        }

        return CodeBlock.builder().beginControlFlow("if (o instanceof $T)", listType)
                .addStatement("return $T.<$T>unmodifiableList(($T) o)", collectionsTypeName, tType,
                        parameterizedListType)
                .endControlFlow().beginControlFlow("if (o instanceof $T)", setType)
                .addStatement("return $T.<$T>unmodifiableSet(($T) o)", collectionsTypeName, tType, parameterizedSetType)
                .endControlFlow().addStatement("return null").build();
    }
}
