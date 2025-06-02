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

import io.soabase.recordbuilder.core.RecordBuilder.Deconstructor;
import io.soabase.recordbuilder.core.RecordBuilder.DeconstructorTemplate;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.SerializationSink;
import io.soabase.recordbuilder.serialization.spi.Serializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.*;
import java.util.stream.Stream;

import static io.soabase.recordbuilder.serialization.spi.CustomizationKey.DO_NOT_WRITE_NULL_OBJECT_VALUES;

public class DeconstructorSerializer implements Serializer {
    public static final Predicate<Type> PREDICATE = type -> fromDeconstructor(type).size() == 1;

    private static class SeparatorCheck {
        private boolean isFirst = true;

        boolean shouldWriteSeparator() {
            if (isFirst) {
                isFirst = false;
                return false;
            }
            return true;
        }
    }

    private interface ArgBuilder {
        Object build(SerializationRegistry registry, SerializationSink sink, SeparatorCheck separatorCheck);
    }

    private final Serializer serializer;

    public static List<Method> fromDeconstructor(Type type) {
        Class<?> serializerClass;
        if (type instanceof Class<?> clazz) {
            serializerClass = clazz;
        } else if ((type instanceof ParameterizedType parameterizedType)
                && (parameterizedType.getRawType() instanceof Class<?> clazz)) {
            serializerClass = clazz;
        } else {
            return List.of();
        }

        return Stream.of(serializerClass.getDeclaredMethods()).filter(DeconstructorSerializer::isDestructor).toList();
    }

    public DeconstructorSerializer(Type type) {
        Method deconstructor = fromDeconstructor(type).getFirst();

        ArgBuilder[] builders = new ArgBuilder[deconstructor.getParameterTypes().length];
        for (int i = 0; i < deconstructor.getParameterCount(); i++) {
            String name = deconstructor.getParameters()[i].getName();
            Class<?> parameterType = deconstructor.getParameterTypes()[i];
            boolean isLast = (i == deconstructor.getParameterCount() - 1);

            if (IntConsumer.class.isAssignableFrom(parameterType)) {
                builders[i] = (_, sink, separatorCheck) -> (IntConsumer) value -> {
                    if (separatorCheck.shouldWriteSeparator()) {
                        sink.separator();
                    }
                    sink.startField(name);
                    sink.intValue(value);
                };
            } else if (LongConsumer.class.isAssignableFrom(parameterType)) {
                builders[i] = (_, sink, separatorCheck) -> (LongConsumer) value -> {
                    if (separatorCheck.shouldWriteSeparator()) {
                        sink.separator();
                    }
                    sink.startField(name);
                    sink.longValue(value);
                };
            } else if (DoubleConsumer.class.isAssignableFrom(parameterType)) {
                builders[i] = (_, sink, separatorCheck) -> (DoubleConsumer) value -> {
                    if (separatorCheck.shouldWriteSeparator()) {
                        sink.separator();
                    }
                    sink.startField(name);
                    sink.doubleValue(value);
                };
            } else if (Consumer.class.isAssignableFrom(parameterType)) {
                ParameterizedType genericType = (ParameterizedType) deconstructor.getGenericParameterTypes()[i];
                builders[i] = (registry, sink, separatorCheck) -> (Consumer<?>) value -> {
                    if (registry.customizations().isTrue(DO_NOT_WRITE_NULL_OBJECT_VALUES) && (value == null)) {
                        return;
                    }
                    if (separatorCheck.shouldWriteSeparator()) {
                        sink.separator();
                    }
                    sink.startField(name);
                    registry.requiredSerializer(genericType.getActualTypeArguments()[0]).serialize(registry, value,
                            sink);
                };
            } else {
                throw new IllegalArgumentException(
                        "Unexpected parameter type " + parameterType + " for parameter " + name);
            }
        }

        serializer = (registry, obj, sink) -> {
            Object[] args = new Object[deconstructor.getParameterCount()];
            SeparatorCheck separatorCheck = new SeparatorCheck();
            for (int i = 0; i < deconstructor.getParameterCount(); i++) {
                args[i] = builders[i].build(registry, sink, separatorCheck);
            }

            try {
                sink.startObject();
                deconstructor.invoke(obj, args);
                sink.endObject();
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke deconstructor " + deconstructor.getName() + " on class "
                        + obj.getClass().getName(), e);
            }
        };
    }

    public static void register(SerializationRegistry registry) {
        registry.registerSerializer("deconstructor", PREDICATE, DeconstructorSerializer::new);
    }

    @Override
    public void serialize(SerializationRegistry registry, Object obj, SerializationSink sink) {
        serializer.serialize(registry, obj, sink);
    }

    private static boolean isDestructor(Method deconstructor) {
        for (Annotation annotation : deconstructor.getAnnotations()) {
            if ((annotation instanceof Deconstructor deconstructorAnnotation)
                    && deconstructorAnnotation.isSerializer()) {
                return true;
            }
            DeconstructorTemplate templateAnnotation = annotation.annotationType()
                    .getAnnotation(DeconstructorTemplate.class);
            if ((templateAnnotation != null) && templateAnnotation.value().isSerializer()) {
                return true;
            }
        }
        return false;
    }
}
