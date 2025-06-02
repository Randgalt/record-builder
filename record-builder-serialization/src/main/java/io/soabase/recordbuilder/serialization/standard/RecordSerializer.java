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

import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.SerializationSink;
import io.soabase.recordbuilder.serialization.spi.Serializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Predicate;

import static io.soabase.recordbuilder.serialization.spi.CustomizationKey.DO_NOT_WRITE_NULL_OBJECT_VALUES;
import static io.soabase.recordbuilder.serialization.spi.CustomizationKey.MAKE_METHODS_ACCESSIBLE;

public class RecordSerializer implements Serializer {
    public static final Predicate<Type> PREDICATE = type -> getRecordClass(type).isPresent();

    private final RecordComponent[] recordComponents;

    public static Optional<Class<?>> getRecordClass(Type type) {
        if ((type instanceof Class<?> clazz) && clazz.isRecord()) {
            return Optional.of(clazz);
        }

        if ((type instanceof ParameterizedType parameterizedType)
                && (parameterizedType.getRawType() instanceof Class<?> clazz) && clazz.isRecord()) {
            return Optional.of(clazz);
        }

        return Optional.empty();
    }

    public static void register(SerializationRegistry registry) {
        registry.registerSerializer("record", PREDICATE, type -> new RecordSerializer(type, registry));
    }

    public RecordSerializer(Type type, SerializationRegistry registry) {
        Class<?> clazz = getRecordClass(type)
                .orElseThrow(() -> new IllegalArgumentException("Type " + type + " is not a record type"));
        recordComponents = clazz.getRecordComponents();
        if (registry.customizations().isTrue(MAKE_METHODS_ACCESSIBLE)) {
            for (RecordComponent recordComponent : recordComponents) {
                recordComponent.getAccessor().setAccessible(true);
            }
        }
    }

    @Override
    public void serialize(SerializationRegistry registry, Object obj, SerializationSink sink) {
        sink.startObject();
        for (int i = 0; i < recordComponents.length; i++) {
            try {
                RecordComponent recordComponent = recordComponents[i];
                Object value = recordComponent.getAccessor().invoke(obj);

                if (registry.customizations().isTrue(DO_NOT_WRITE_NULL_OBJECT_VALUES) && (value == null)) {
                    continue;
                }

                String name = recordComponent.getName();
                Type type = recordComponent.getGenericType();
                boolean isLast = (i + 1) == recordComponents.length;

                sink.startField(name);
                Serializer serializer = registry.serializer(type)
                        .orElseThrow(() -> new IllegalArgumentException("No serializer found for type " + type));
                serializer.serialize(registry, value, sink);
                if (!isLast) {
                    sink.separator();
                }
            } catch (Exception e) {
                // TODO
                throw new RuntimeException(e);
            }
        }
        sink.endObject();
    }
}
