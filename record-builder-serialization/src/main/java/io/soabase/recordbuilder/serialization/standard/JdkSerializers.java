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
import io.soabase.recordbuilder.serialization.spi.Deserializer;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.Serializer;
import io.soabase.recordbuilder.serialization.token.MetaToken.ArrayStartToken;
import io.soabase.recordbuilder.serialization.token.MetaToken.ObjectStartToken;
import io.soabase.recordbuilder.serialization.token.NumberToken;
import io.soabase.recordbuilder.serialization.token.NumberToken.*;
import io.soabase.recordbuilder.serialization.token.ValueToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.BooleanToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.NullToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.StringToken;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.soabase.recordbuilder.serialization.spi.SerializationRegistry.isAssignablePredicate;

public class JdkSerializers {
    private JdkSerializers() {
    }

    public static void register(SerializationRegistry registry) {
        registry.registerSerializer("String", String.class, _ -> (_, obj, sink) -> sink.stringValue((String) obj));
        registry.registerDeserializer("String", String.class,
                _ -> (_, stream) -> stream.current().as(StringToken.class).value());

        registry.registerSerializer("Number", isAssignablePredicate(Number.class),
                _ -> (_, obj, sink) -> sink.numberValue((Number) obj));
        registry.registerDeserializer("Number", isAssignablePredicate(Number.class),
                _ -> (_, stream) -> stream.current().as(NumberToken.class).value());

        registry.registerSerializer("Generic<T>", type -> type instanceof TypeVariable<?>, _ -> (innerRegistry, obj,
                sink) -> innerRegistry.requiredSerializer(obj.getClass()).serialize(innerRegistry, obj, sink));

        registry.registerSerializer("Object", Object.class, _ -> (innerRegistry, obj, sink) -> innerRegistry
                .requiredSerializer(obj.getClass()).serialize(innerRegistry, obj, sink));
        registry.registerDeserializer("Object", Object.class, objectDeserializer);

        registry.registerDeserializer("WildcardType", type -> type instanceof WildcardType, wildcardDeserializer);

        registry.registerSerializer("Enums", type -> (type instanceof Class<?> clazz) && clazz.isEnum(), enumSerializer);
        registry.registerDeserializer("Enums", type -> (type instanceof Class<?> clazz) && clazz.isEnum(), enumDeserializer);
    }

    public static final Function<Type, Deserializer> objectDeserializer = _ -> (registry,
            stream) -> switch (stream.current()) {
            case NullToken _ -> null;
            case BooleanToken booleanToken -> booleanToken == BooleanToken.TRUE;
            case ByteToken(var value) -> value;
            case ShortToken(var value) -> value;
            case IntToken(var value) -> value;
            case LongToken(var value) -> value;
            case FloatToken(var value) -> value;
            case DoubleToken(var value) -> value;
            case ValueToken valueToken -> valueToken.value();
            case ObjectStartToken _ -> registry.requiredDeserializer(new TypeLiteral<Map<String, Object>>() {
            }.getType()).deserialize(registry, stream);
            case ArrayStartToken _ -> registry.requiredDeserializer(new TypeLiteral<List<Object>>() {
            }.getType()).deserialize(registry, stream);
            default -> throw new IllegalArgumentException(
                    "Cannot deserialize " + stream.current().getClass().getName());
            };

    public static final Function<Type, Deserializer> wildcardDeserializer = type -> (registry, stream) -> {
        WildcardType wildcardType = (WildcardType) type;
        return registry.requiredDeserializer(wildcardType.getUpperBounds()[0]).deserialize(registry, stream);
    };

    public static final Function<Type, Serializer> enumSerializer = _ -> (_, obj, sink) -> {
        if (obj == null) {
            sink.nullValue();
        } else {
            sink.stringValue(((Enum<?>) obj).name());
        }
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Function<Type, Deserializer> enumDeserializer = type -> (_, stream) -> {
        if (stream.current() instanceof NullToken) {
            return null;
        }
        String name = stream.current().as(StringToken.class).value();
        return Enum.valueOf((Class<? extends Enum>) type, name);
    };
}
