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

import io.soabase.com.google.inject.TypeLiteral;
import io.soabase.com.google.inject.util.MoreTypes;
import io.soabase.recordbuilder.serialization.spi.Deserializer;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.Serializer;
import io.soabase.recordbuilder.serialization.token.NumberToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.StringToken;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;

public class ArraySerializers {
    private ArraySerializers() {
    }

    public static void register(SerializationRegistry registry) {
        // TODO other primitives, etc.

        registry.registerSerializer("int[]", int[].class, _ -> intArraySerializer);
        registry.registerDeserializer("int[]", int[].class, _ -> intArrayDeserializer);

        registry.registerSerializer("String[]", String[].class, _ -> stringArraySerializer);
        registry.registerDeserializer("String[]", String[].class, _ -> stringArrayDeserializer);

        registry.registerSerializer("isArray", isArray, arraySerializerFactory);
        registry.registerDeserializer("isArray", isArray, arrayDeserializerFactory);
    }

    public static Predicate<Type> isArray = type -> {
        TypeLiteral<?> typeLiteral = TypeLiteral.get(type);
        return typeLiteral.getRawType().isArray();
    };

    public static Serializer intArraySerializer = (_, obj, sink) -> {
        sink.startArray();
        boolean first = true;
        for (int value : (int[]) obj) {
            if (first) {
                first = false;
            } else {
                sink.separator();
            }
            sink.intValue(value);
        }
        sink.endArray();
    };

    public static Deserializer intArrayDeserializer = (_, stream) -> {
        int[] result = new int[10]; // TODO
        int index = 0;
        for (stream.array(); stream.hasMore(); stream.advance()) {
            // TODO handle conversions
            NumberToken number = stream.current().as(NumberToken.class);
            if (number == NumberToken.DoubleToken.NAN) {
                // TODO
                throw new IllegalArgumentException("Cannot deserialize NaN to int array");
            }
            int value = number.value().intValue();
            if (index >= result.length) {
                int[] newResult = new int[result.length * 2];
                System.arraycopy(result, 0, newResult, 0, result.length);
                result = newResult;
            }
            result[index++] = value;
        }

        if (result.length != index) {
            int[] newResult = new int[index];
            System.arraycopy(result, 0, newResult, 0, index);
            return newResult;
        }

        return result;
    };

    public static Serializer stringArraySerializer = (_, obj, sink) -> {
        sink.startArray();
        boolean first = true;
        for (String value : (String[]) obj) {
            if (first) {
                first = false;
            } else {
                sink.separator();
            }
            sink.stringValue(value);
        }
        sink.endArray();
    };

    public static Deserializer stringArrayDeserializer = (_, stream) -> {
        String[] result = new String[10]; // TODO
        int index = 0;
        for (stream.array(); stream.hasMore(); stream.advance()) {
            String value = stream.current().as(StringToken.class).value();
            if (index >= result.length) {
                String[] newResult = new String[result.length * 2];
                System.arraycopy(result, 0, newResult, 0, result.length);
                result = newResult;
            }
            result[index++] = value;
        }

        if (result.length != index) {
            String[] newResult = new String[index];
            System.arraycopy(result, 0, newResult, 0, index);
            return newResult;
        }

        return result;
    };

    public static Function<Type, Serializer> arraySerializerFactory = type -> {
        Type componentType = MoreTypes.componentType(type);
        return (registry, obj, sink) -> {
            sink.startArray();
            boolean first = true;
            for (Object value : (Object[]) obj) {
                if (first) {
                    first = false;
                } else {
                    sink.separator();
                }
                registry.requiredSerializer(componentType).serialize(registry, value, sink);
            }
            sink.endArray();
        };
    };

    public static Function<Type, Deserializer> arrayDeserializerFactory = type -> {
        Type componentType = MoreTypes.componentType(type);
        TypeLiteral<?> typeLiteral = TypeLiteral.get(componentType);
        return (registry, stream) -> {
            Deserializer componentDeserializer = registry.requiredDeserializer(componentType);
            Object[] result = (Object[]) Array.newInstance(typeLiteral.getRawType(), 10);

            int index = 0;
            for (stream.array(); stream.hasMore(); stream.advance(), ++index) {
                Object value = componentDeserializer.deserialize(registry, stream);
                if (index >= result.length) {
                    Object[] newResult = new Object[result.length * 2];
                    System.arraycopy(result, 0, newResult, 0, result.length);
                    result = newResult;
                }
                result[index] = value;
            }

            if (result.length != index) {
                Object[] newResult = (Object[]) Array.newInstance(typeLiteral.getRawType(), index);
                System.arraycopy(result, 0, newResult, 0, index);
                return newResult;
            }

            return result;
        };
    };
}
