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
package io.soabase.recordbuilder.serialization.spi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface SerializationRegistry {
    static Predicate<Type> isAssignablePredicate(Class<?> derivedClass) {
        return type -> {
            if (type instanceof Class<?> clazz) {
                return derivedClass.isAssignableFrom(clazz);
            }
            if ((type instanceof ParameterizedType parameterizedType)
                    && (parameterizedType.getRawType() instanceof Class<?> clazz)) {
                return derivedClass.isAssignableFrom(clazz);
            }
            // TODO more?
            return false;
        };
    }

    void registerSerializer(String name, Type type, Function<Type, Serializer> factory);

    void registerSerializer(String name, Predicate<Type> predicate, Function<Type, Serializer> factory);

    void registerDeserializer(String name, Type type, Function<Type, Deserializer> factory);

    void registerDeserializer(String name, Predicate<Type> predicate, Function<Type, Deserializer> factory);

    Optional<Serializer> serializer(Type type);

    Serializer requiredSerializer(Type type);

    Optional<Deserializer> deserializer(Type type);

    Deserializer requiredDeserializer(Type type);

    Customizations customizations();
}
