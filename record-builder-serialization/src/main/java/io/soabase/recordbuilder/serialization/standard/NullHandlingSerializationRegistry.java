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
import io.soabase.recordbuilder.serialization.token.ValueToken.NullToken;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class NullHandlingSerializationRegistry implements SerializationRegistry {
    private final SerializationRegistry delegate;

    public NullHandlingSerializationRegistry(SerializationRegistry delegate) {
        this.delegate = delegate;
    }

    @Override
    public void registerSerializer(String name, Type type, Function<Type, Serializer> factory) {
        delegate.registerSerializer(name, type, wrapSerializer(factory));
    }

    @Override
    public void registerSerializer(String name, Predicate<Type> predicate, Function<Type, Serializer> factory) {
        delegate.registerSerializer(name, predicate, wrapSerializer(factory));
    }

    @Override
    public void registerDeserializer(String name, Type type, Function<Type, Deserializer> factory) {
        delegate.registerDeserializer(name, type, wrapDeserializer(factory));
    }

    @Override
    public void registerDeserializer(String name, Predicate<Type> predicate, Function<Type, Deserializer> factory) {
        delegate.registerDeserializer(name, predicate, wrapDeserializer(factory));
    }

    @Override
    public Optional<Serializer> serializer(Type type) {
        if (type == null) {
            Serializer nullSerializer = (_, _, sink) -> sink.nullValue();
            return Optional.of(nullSerializer);
        }
        return delegate.serializer(type);
    }

    @Override
    public Serializer requiredSerializer(Type type) {
        return serializer(type).orElseThrow(() -> new NoSuchElementException("No serializer for " + type)); // TODO
    }

    @Override
    public Optional<Deserializer> deserializer(Type type) {
        if (type == null) {
            Deserializer deserializer = (_, stream) -> {
                if (stream.current().equals(NullToken.INSTANCE)) {
                    return null;
                }
                throw new IllegalStateException("Expected null token but found " + stream.current());
            };
            return Optional.of(deserializer);
        }
        return delegate.deserializer(type);
    }

    @Override
    public Deserializer requiredDeserializer(Type type) {
        return deserializer(type).orElseThrow(() -> new NoSuchElementException("No deserializer for " + type)); // TODO
    }

    @Override
    public Customizations customizations() {
        return delegate.customizations();
    }

    private static Function<Type, Deserializer> wrapDeserializer(Function<Type, Deserializer> factory) {
        return type -> {
            Deserializer deserializerInstance = factory.apply(type);
            return (registry, stream) -> {
                if (stream.current().equals(NullToken.INSTANCE)) {
                    return null;
                }
                return deserializerInstance.deserialize(registry, stream);
            };
        };
    }

    private static Function<Type, Serializer> wrapSerializer(Function<Type, Serializer> factory) {
        return type -> (registry, obj, sink) -> {
            if (obj == null) {
                sink.nullValue();
            } else {
                factory.apply(type).serialize(registry, obj, sink);
            }
        };
    }
}
