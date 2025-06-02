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
package io.soabase.recordbuilder.serialization;

import io.soabase.recordbuilder.serialization.json.JsonSerializationSink;
import io.soabase.recordbuilder.serialization.json.JsonTokenSupplier;
import io.soabase.recordbuilder.serialization.spi.CharSupplier;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.SinkWriter;
import io.soabase.recordbuilder.serialization.token.TokenStreams;

import java.io.Reader;
import java.lang.reflect.Type;

public class RecordBuilderSerializer {
    private final SerializationRegistry registry;

    public RecordBuilderSerializer(SerializationRegistry registry) {
        this.registry = registry;
    }

    public String toJson(Object value) {
        return toJson(value, value.getClass());
    }

    public String toJson(Object value, Type type) {
        StringBuilder stringBuilder = new StringBuilder();
        SinkWriter sinkWriter = SinkWriter.of(stringBuilder);
        registry.requiredSerializer(type).serialize(registry, value, new JsonSerializationSink(sinkWriter));
        return stringBuilder.toString();
    }

    public <T> T fromJson(String json, Class<T> type) {
        CharSupplier charSupplier = CharSupplier.of(json);
        return type.cast(registry.requiredDeserializer(type).deserialize(registry,
                TokenStreams.of(new JsonTokenSupplier(charSupplier))));
    }

    public <T> T fromJson(Reader json, Class<T> type) {
        CharSupplier charSupplier = CharSupplier.of(json);
        return type.cast(registry.requiredDeserializer(type).deserialize(registry,
                TokenStreams.of(new JsonTokenSupplier(charSupplier))));
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json, Type type) {
        CharSupplier charSupplier = CharSupplier.of(json);
        return (T) registry.requiredDeserializer(type).deserialize(registry,
                TokenStreams.of(new JsonTokenSupplier(charSupplier)));
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(Reader json, Type type) {
        CharSupplier charSupplier = CharSupplier.of(json);
        return (T) registry.requiredDeserializer(type).deserialize(registry,
                TokenStreams.of(new JsonTokenSupplier(charSupplier)));
    }
}
