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

import io.soabase.recordbuilder.serialization.token.MetaToken.*;
import io.soabase.recordbuilder.serialization.token.NumberToken;
import io.soabase.recordbuilder.serialization.token.NumberToken.*;
import io.soabase.recordbuilder.serialization.token.Token;
import io.soabase.recordbuilder.serialization.token.ValueToken.BooleanToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.NullToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.StringToken;

import java.util.function.Consumer;

public record TokenSerializationSink(SerializationSink sink) implements Consumer<Token> {
    @Override
    public void accept(Token token) {
        switch (token) {
        case NullToken _ -> sink.nullValue();
        case ArrayStartToken _ -> sink.startArray();
        case ArrayEndToken _ -> sink.endArray();
        case ObjectStartToken _ -> sink.startObject();
        case ObjectEndToken _ -> sink.endObject();
        case SeparatorToken _ -> sink.separator();
        case BooleanToken booleanToken -> sink.booleanValue(booleanToken == BooleanToken.TRUE);
        case ByteToken(var value) -> sink.byteValue(value);
        case ShortToken(var value) -> sink.shortValue(value);
        case IntToken(var value) -> sink.intValue(value);
        case LongToken(var value) -> sink.longValue(value);
        case FloatToken(var value) -> sink.floatValue(value);
        case DoubleToken(var value) -> sink.doubleValue(value);
        case NumberToken numberToken -> sink.numberValue(numberToken.value());
        case StringToken(var str) -> sink.stringValue(str);
        case FieldNameToken(var fieldName) -> sink.startField(fieldName);
        case StreamEndToken _ -> {
        }
        }
    }
}
