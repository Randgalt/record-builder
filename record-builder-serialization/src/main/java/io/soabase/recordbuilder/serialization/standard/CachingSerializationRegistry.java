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

import io.soabase.recordbuilder.serialization.spi.Customizations;
import io.soabase.recordbuilder.serialization.spi.Deserializer;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.Serializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class CachingSerializationRegistry implements SerializationRegistry {
    private final SerializationRegistry delegate;
    private final Map<Type, Optional<Serializer>> serializerCache = new ConcurrentHashMap<>();
    private final Map<Type, Optional<Deserializer>> deserializerCache = new ConcurrentHashMap<>();

    public CachingSerializationRegistry(SerializationRegistry delegate) {
        this.delegate = delegate;
    }

    @Override
    public void registerSerializer(String name, Type type, Function<Type, Serializer> factory) {
        delegate.registerSerializer(name, type, factory);
        serializerCache.clear();
    }

    @Override
    public void registerSerializer(String name, Predicate<Type> predicate, Function<Type, Serializer> factory) {
        delegate.registerSerializer(name, predicate, factory);
        serializerCache.clear();
    }

    @Override
    public void registerDeserializer(String name, Type type, Function<Type, Deserializer> factory) {
        delegate.registerDeserializer(name, type, factory);
        deserializerCache.clear();
    }

    @Override
    public void registerDeserializer(String name, Predicate<Type> predicate, Function<Type, Deserializer> factory) {
        delegate.registerDeserializer(name, predicate, factory);
        deserializerCache.clear();
    }

    @Override
    public Optional<Serializer> serializer(Type type) {
        return serializerCache.computeIfAbsent(type, _ -> delegate.serializer(type));
    }

    @Override
    public Serializer requiredSerializer(Type type) {
        return serializer(type).orElseThrow(() -> new NoSuchElementException("No serializer for " + type)); // TODO
    }

    @Override
    public Optional<Deserializer> deserializer(Type type) {
        return deserializerCache.computeIfAbsent(type, _ -> delegate.deserializer(type));
    }

    @Override
    public Deserializer requiredDeserializer(Type type) {
        return deserializer(type).orElseThrow(() -> new NoSuchElementException("No deserializer for " + type)); // TODO
    }

    @Override
    public Customizations customizations() {
        return delegate.customizations();
    }
}
