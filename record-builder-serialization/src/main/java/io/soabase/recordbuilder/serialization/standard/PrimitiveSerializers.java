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
import io.soabase.recordbuilder.serialization.spi.Serializer;
import io.soabase.recordbuilder.serialization.token.NumberToken;
import io.soabase.recordbuilder.serialization.token.NumberToken.*;
import io.soabase.recordbuilder.serialization.token.Token;
import io.soabase.recordbuilder.serialization.token.ValueToken.BooleanToken;

public class PrimitiveSerializers {
    private PrimitiveSerializers() {
    }

    public static void register(SerializationRegistry registry) {
        registry.registerSerializer("byte", byte.class, _ -> byteSerializer);
        registry.registerDeserializer("byte", byte.class, _ -> byteDeserializer);
        registry.registerSerializer("Byte", Byte.class, _ -> byteSerializer);
        registry.registerDeserializer("Byte", Byte.class, _ -> byteDeserializer);

        registry.registerSerializer("boolean", boolean.class, _ -> booleanSerializer);
        registry.registerDeserializer("boolean", boolean.class, _ -> booleanDeserializer);
        registry.registerSerializer("Boolean", Boolean.class, _ -> booleanSerializer);
        registry.registerDeserializer("Boolean", Boolean.class, _ -> booleanDeserializer);

        registry.registerSerializer("short", short.class, _ -> shortSerializer);
        registry.registerDeserializer("short", short.class, _ -> shortDeserializer);
        registry.registerSerializer("Short", Short.class, _ -> shortSerializer);
        registry.registerDeserializer("Short", Short.class, _ -> shortDeserializer);

        registry.registerSerializer("int", int.class, _ -> intSerializer);
        registry.registerDeserializer("int", int.class, _ -> intDeserializer);
        registry.registerSerializer("Integer", Integer.class, _ -> intSerializer);
        registry.registerDeserializer("Integer", Integer.class, _ -> intDeserializer);

        registry.registerSerializer("long", long.class, _ -> longSerializer);
        registry.registerDeserializer("long", long.class, _ -> longDeserializer);
        registry.registerSerializer("Long", Long.class, _ -> longSerializer);
        registry.registerDeserializer("Long", Long.class, _ -> longDeserializer);

        registry.registerSerializer("float", float.class, _ -> floatSerializer);
        registry.registerDeserializer("float", float.class, _ -> floatDeserializer);
        registry.registerSerializer("Float", Float.class, _ -> floatSerializer);
        registry.registerDeserializer("Float", Float.class, _ -> floatDeserializer);

        registry.registerSerializer("double", double.class, _ -> doubleSerializer);
        registry.registerDeserializer("double", double.class, _ -> doubleDeserializer);
        registry.registerSerializer("Double", Double.class, _ -> doubleSerializer);
        registry.registerDeserializer("Double", Double.class, _ -> doubleDeserializer);

        // TODO - add arrays
    }

    public static Serializer byteSerializer = (_, obj, sink) -> sink.byteValue((byte) obj);
    public static Deserializer byteDeserializer = ((_, stream) -> switch (stream.current()) {
    case ByteToken(byte b) -> b;
    case NumberToken numberToken -> numberToken.value().byteValue();
    default -> throw new IllegalStateException("Unexpected value: " + stream.current()); // TODO
    });
    // add
    // traits/options
    // for
    // type
    // conversions

    public static Serializer booleanSerializer = (_, obj, sink) -> sink.booleanValue((boolean) obj);
    public static Deserializer booleanDeserializer = (_, stream) -> stream.current().as(BooleanToken.class)
            .equals(BooleanToken.TRUE); // TODO
    // -
    // add
    // traits/options
    // for
    // type
    // conversions

    public static Deserializer numberDeserializer = (_, stream) -> {
        Token token = stream.current();
        return switch (token) {
        // TODO
        case IntToken(int i) -> i;
        case LongToken(long l) -> l;
        case NumberValueToken(Number n) -> n.intValue();
        default -> throw new IllegalStateException("Unexpected value: " + token); // TODO
        };
    };

    public static Serializer shortSerializer = (_, obj, sink) -> sink.shortValue((short) obj);
    public static Deserializer shortDeserializer = ((_, stream) -> switch (stream.current()) {
    case ShortToken(short s) -> s;
    case NumberToken numberToken -> numberToken.value().shortValue();
    default -> throw new IllegalStateException("Unexpected value: " + stream.current()); // TODO
    });

    public static Serializer intSerializer = (_, obj, sink) -> sink.intValue((int) obj);
    public static Deserializer intDeserializer = ((_, stream) -> switch (stream.current()) {
    case IntToken(int i) -> i;
    case NumberToken numberToken -> numberToken.value().intValue();
    default -> throw new IllegalStateException("Unexpected value: " + stream.current()); // TODO
    });

    public static Serializer longSerializer = (_, obj, sink) -> sink.longValue((long) obj);
    public static Deserializer longDeserializer = ((_, stream) -> switch (stream.current()) {
    case LongToken(long l) -> l;
    case NumberToken numberToken -> numberToken.value().longValue();
    default -> throw new IllegalStateException("Unexpected value: " + stream.current()); // TODO
    });

    public static Serializer floatSerializer = (_, obj, sink) -> sink.floatValue((float) obj);
    public static Deserializer floatDeserializer = ((_, stream) -> switch (stream.current()) {
    case FloatToken(float f) -> f;
    case NumberToken numberToken -> numberToken.value().floatValue();
    default -> throw new IllegalStateException("Unexpected value: " + stream.current()); // TODO
    });

    public static Serializer doubleSerializer = (_, obj, sink) -> sink.doubleValue((double) obj);
    public static Deserializer doubleDeserializer = ((_, stream) -> switch (stream.current()) {
    case DoubleToken(double d) -> d;
    case NumberToken numberToken -> numberToken.value().doubleValue();
    default -> throw new IllegalStateException("Unexpected value: " + stream.current()); // TODO
    });
}
