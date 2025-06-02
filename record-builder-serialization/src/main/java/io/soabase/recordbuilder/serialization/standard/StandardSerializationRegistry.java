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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

public class StandardSerializationRegistry implements SerializationRegistry {
    private final List<SerializerEntry> serializerEntries = new CopyOnWriteArrayList<>();
    private final List<DeserializerEntry> deserializerEntries = new CopyOnWriteArrayList<>();
    private final Customizations customizations;

    public StandardSerializationRegistry() {
        this(Customizations.builder().build());
    }

    public StandardSerializationRegistry(Customizations customizations) {
        this.customizations = customizations;
    }

    private record SerializerEntry(String name, Predicate<Type> predicate, Function<Type, Serializer> factory) {
    }

    private record DeserializerEntry(String name, Predicate<Type> predicate, Function<Type, Deserializer> factory) {
    }

    @Override
    public void registerSerializer(String name, Type type, Function<Type, Serializer> factory) {
        serializerEntries.add(new SerializerEntry(name, type::equals, factory));
    }

    @Override
    public void registerSerializer(String name, Predicate<Type> predicate, Function<Type, Serializer> factory) {
        serializerEntries.add(new SerializerEntry(name, predicate, factory));
    }

    @Override
    public void registerDeserializer(String name, Type type, Function<Type, Deserializer> factory) {
        deserializerEntries.add(new DeserializerEntry(name, type::equals, factory));
    }

    @Override
    public void registerDeserializer(String name, Predicate<Type> predicate, Function<Type, Deserializer> factory) {
        deserializerEntries.add(new DeserializerEntry(name, predicate, factory));
    }

    @Override
    public Optional<Serializer> serializer(Type type) {
        return serializerEntries.stream().filter(entry -> entry.predicate.test(type))
                .map(entry -> entry.factory.apply(type)).findFirst();
    }

    @Override
    public Serializer requiredSerializer(Type type) {
        return serializer(type).orElseThrow(() -> new NoSuchElementException("No serializer for " + type));
    }

    @Override
    public Optional<Deserializer> deserializer(Type type) {
        return deserializerEntries.stream().filter(entry -> entry.predicate.test(type))
                .map(entry -> entry.factory.apply(type)).findFirst();
    }

    @Override
    public Deserializer requiredDeserializer(Type type) {
        return deserializer(type).orElseThrow(() -> new NoSuchElementException("No deserializer for " + type)); // TODO
    }

    @Override
    public Customizations customizations() {
        return customizations;
    }
}
