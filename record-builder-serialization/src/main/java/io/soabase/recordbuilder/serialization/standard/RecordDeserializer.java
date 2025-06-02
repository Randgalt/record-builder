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

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.soabase.recordbuilder.serialization.spi.CustomizationKey.MAKE_METHODS_ACCESSIBLE;
import static io.soabase.recordbuilder.serialization.standard.RecordSerializer.PREDICATE;
import static io.soabase.recordbuilder.serialization.standard.RecordSerializer.getRecordClass;

public class RecordDeserializer implements Deserializer {
    private final RecordComponent[] recordComponents;
    private final Map<String, Integer> indexMap;
    private final Constructor<?> constructor;

    public static void register(SerializationRegistry registry) {
        registry.registerDeserializer("record", PREDICATE, type -> new RecordDeserializer(type, registry));
    }

    public RecordDeserializer(Type type, SerializationRegistry registry) {
        Class<?> clazz = getRecordClass(type)
                .orElseThrow(() -> new IllegalArgumentException("Type " + type + " is not a record type"));
        recordComponents = clazz.getRecordComponents();

        indexMap = IntStream.range(0, recordComponents.length)
                .mapToObj(index -> Map.entry(recordComponents[index].getName(), index))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        constructor = findConstructor(registry, clazz, recordComponents);
    }

    @Override
    public Object deserialize(SerializationRegistry registry, TokenStream stream) {
        Object[] args = new Object[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            RecordComponent recordComponent = recordComponents[i];
            Class<?> type = recordComponent.getType();
            defaultArgument(type, args, i);
        }

        for (stream.object(); stream.hasMore(); stream.advance()) {
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
            args[index] = registry.requiredDeserializer(recordComponents[index].getGenericType()).deserialize(registry,
                    stream);
        }

        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    public static void defaultArgument(Class<?> type, Object[] args, int i) {
        if (type.isPrimitive()) {
            if (type.isAssignableFrom(boolean.class)) {
                args[i] = false;
            } else if (type.isAssignableFrom(byte.class)) {
                args[i] = (byte) 0;
            } else if (type.isAssignableFrom(short.class)) {
                args[i] = (short) 0;
            } else if (type.isAssignableFrom(int.class)) {
                args[i] = 0;
            } else if (type.isAssignableFrom(long.class)) {
                args[i] = 0L;
            } else if (type.isAssignableFrom(float.class)) {
                args[i] = 0f;
            } else if (type.isAssignableFrom(double.class)) {
                args[i] = 0d;
            } else if (type.isAssignableFrom(char.class)) {
                args[i] = (char) 0;
            }
        }
    }

    private Constructor<?> findConstructor(SerializationRegistry registry, Class<?> clazz,
            RecordComponent[] recordComponents) {
        Class<?>[] parameterTypes = new Class<?>[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            parameterTypes[i] = recordComponents[i].getType();
        }
        try {
            // TODO - do this everywhere
            if (registry.customizations().isTrue(MAKE_METHODS_ACCESSIBLE)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
                constructor.setAccessible(true);
                return constructor;
            }
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            // TODO
            throw new IllegalArgumentException("No suitable constructor found for " + clazz.getName(), e);
        }
    }
}
