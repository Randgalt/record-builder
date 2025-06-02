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
package io.soabase.recordbuilder.serialization.standard;

import io.soabase.recordbuilder.serialization.spi.Deserializer;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.token.MetaToken.FieldNameToken;
import io.soabase.recordbuilder.serialization.token.TokenStream;
import io.soabase.recordbuilder.serialization.token.ValueToken.NullToken;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

import static io.soabase.com.google.inject.util.MoreTypes.unbox;
import static io.soabase.com.google.inject.util.Types.arrayOf;
import static io.soabase.com.google.inject.util.Types.newParameterizedType;
import static io.soabase.recordbuilder.serialization.spi.CustomizationKey.MAKE_METHODS_ACCESSIBLE;
import static io.soabase.recordbuilder.serialization.standard.DeconstructorSerializer.PREDICATE;
import static io.soabase.recordbuilder.serialization.standard.RecordDeserializer.defaultArgument;

public class DeconstructorDeserializer implements Deserializer {
    private final Constructor<?> constructor;
    private final Map<String, Function<SerializationRegistry, Deserializer>> handlers;
    private final Map<String, Integer> indexMap;

    public DeconstructorDeserializer(Type type) {
        Map<TypeVariable<?>, Type> typeVariableMap = new HashMap<>();
        if ((type instanceof ParameterizedType parameterizedType)
                && (parameterizedType.getRawType() instanceof Class<?> rawClass)) {
            for (int i = 0; i < rawClass.getTypeParameters().length; i++) {
                typeVariableMap.put(rawClass.getTypeParameters()[i], parameterizedType.getActualTypeArguments()[i]);
            }
        }

        Method deconstructor = DeconstructorSerializer.fromDeconstructor(type).getFirst();

        Map<String, Function<SerializationRegistry, Deserializer>> handlers = new HashMap<>();
        Map<String, Integer> indexMap = new HashMap<>();

        constructor = findConstructor(type, deconstructor);

        for (int i = 0; i < deconstructor.getParameterCount(); i++) {
            String name = deconstructor.getParameters()[i].getName();
            Class<?> rawParameterType = deconstructor.getParameterTypes()[i];

            indexMap.put(name, i);

            if (IntConsumer.class.isAssignableFrom(rawParameterType)) {
                handlers.put(name, registry -> registry.requiredDeserializer(Integer.class));
            } else if (LongConsumer.class.isAssignableFrom(rawParameterType)) {
                handlers.put(name, registry -> registry.requiredDeserializer(Long.class));
            } else if (DoubleConsumer.class.isAssignableFrom(rawParameterType)) {
                handlers.put(name, registry -> registry.requiredDeserializer(Double.class));
            } else if (Consumer.class.isAssignableFrom(rawParameterType)) {
                ParameterizedType genericParameterType = (ParameterizedType) deconstructor
                        .getGenericParameterTypes()[i];
                Type targetType = unmap(typeVariableMap, genericParameterType.getActualTypeArguments()[0]);
                handlers.put(name, registry -> registry.requiredDeserializer(targetType));
            } else {
                throw new IllegalArgumentException(
                        "Unexpected parameter type " + rawParameterType + " for parameter " + name);
            }
        }

        this.handlers = Map.copyOf(handlers);
        this.indexMap = Map.copyOf(indexMap);
    }

    private Type unmap(Map<TypeVariable<?>, Type> typeVariableMap, Type actualTypeArgument) {
        if (actualTypeArgument instanceof TypeVariable<?> typeVariable) {
            Type type = typeVariableMap.get(typeVariable);
            if (type == null) {
                throw new IllegalArgumentException(
                        "Type variable " + typeVariable + " not found in " + typeVariableMap);
            }
            return type;
        } else if (actualTypeArgument instanceof GenericArrayType genericArrayType) {
            Type type = genericArrayType.getGenericComponentType();
            Type unwrappedType = unmap(typeVariableMap, type);
            if (!unwrappedType.equals(type)) {
                return arrayOf(unwrappedType);
            }
        } else if (actualTypeArgument instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Type[] unwrappedTypeArguments = new Type[actualTypeArguments.length];
            for (int i = 0; i < actualTypeArguments.length; i++) {
                unwrappedTypeArguments[i] = unmap(typeVariableMap, actualTypeArguments[i]);
            }
            return newParameterizedType(parameterizedType.getRawType(), unwrappedTypeArguments);
        } else if (actualTypeArgument instanceof WildcardType wildcardType) {
            Type type = wildcardType.getUpperBounds()[0];
            return unmap(typeVariableMap, type);
        }
        return actualTypeArgument;
    }

    private static Constructor<?> findConstructor(Type type, Method deconstructor) {
        outer: for (Constructor<?> constructor : unwrapType(type).getDeclaredConstructors()) {
            if (constructor.getParameterCount() != deconstructor.getParameterCount()) {
                continue;
            }

            for (int i = 0; i < deconstructor.getParameterCount(); ++i) {
                Class<?> constructorParameterType = constructor.getParameterTypes()[i];
                Class<?> parameterType = deconstructor.getParameterTypes()[i];
                if (IntConsumer.class.isAssignableFrom(parameterType)) {
                    if (!constructorParameterType.equals(int.class)
                            && !constructorParameterType.equals(Integer.class)) {
                        continue outer;
                    }
                } else if (LongConsumer.class.isAssignableFrom(parameterType)) {
                    if (!constructorParameterType.equals(long.class) && !constructorParameterType.equals(Long.class)) {
                        continue outer;
                    }
                } else if (DoubleConsumer.class.isAssignableFrom(parameterType)) {
                    if (!constructorParameterType.equals(double.class)
                            && !constructorParameterType.equals(Double.class)) {
                        continue outer;
                    }
                } else if (Consumer.class.isAssignableFrom(parameterType)) {
                    Type constructorGenericParameterType = constructor.getGenericParameterTypes()[i];
                    Type genericParameterType = ((ParameterizedType) deconstructor.getGenericParameterTypes()[i])
                            .getActualTypeArguments()[0];
                    if (constructorParameterType.isPrimitive()) {
                        if (!constructorParameterType.equals(unbox(genericParameterType))) {
                            continue outer;
                        }
                    } else {
                        if (!constructorGenericParameterType.equals(genericParameterType)) {
                            continue outer;
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unexpected parameter type " + parameterType);
                }

                // it matches - use it
                return constructor;
            }
        }
        throw new IllegalArgumentException("No deconstructor found");
    }

    public static void register(SerializationRegistry registry) {
        registry.registerDeserializer("deconstructor", PREDICATE, DeconstructorDeserializer::new);
    }

    @Override
    public Object deserialize(SerializationRegistry registry, TokenStream stream) {
        Object[] args = new Object[handlers.size()];
        for (int i = 0; i < constructor.getParameterCount(); i++) {
            Class<?> type = constructor.getParameterTypes()[i];
            defaultArgument(type, args, i);
        }

        boolean first = true;
        for (stream.object(); stream.hasMore(); stream.advance(), first = false) {
            // TODO is this still needed?
            if (first && stream.current().equals(NullToken.INSTANCE)) {
                return null;
            }

            FieldNameToken fieldNameToken = stream.current().as(FieldNameToken.class);
            Integer index = indexMap.get(fieldNameToken.fieldName());
            if (index == null) {
                if (registry.customizations().isTrue(MAKE_METHODS_ACCESSIBLE)) {
                    stream.advance();
                    continue;
                }

                // TODO
                throw new IllegalArgumentException("Field " + fieldNameToken.fieldName() + " not found");
            }

            stream.advance();
            args[index] = handlers.get(fieldNameToken.fieldName()).apply(registry).deserialize(registry, stream);
        }

        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    private static Class<?> unwrapType(Type type) {
        // TODO use library for this
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return unwrapType(parameterizedType.getRawType());
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return unwrapType(genericArrayType.getGenericComponentType());
        }
        if (type instanceof WildcardType wildcardType) {
            return unwrapType(wildcardType.getUpperBounds()[0]);
        }

        // TODO
        throw new IllegalArgumentException("Type " + type + " is not a class or parameterized type");
    }
}
