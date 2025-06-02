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
package io.soabase.recordbuilder.serialization.json;

import io.soabase.recordbuilder.serialization.spi.CharSupplier;
import io.soabase.recordbuilder.serialization.token.MetaToken.*;
import io.soabase.recordbuilder.serialization.token.NumberToken.DoubleToken;
import io.soabase.recordbuilder.serialization.token.NumberToken.IntToken;
import io.soabase.recordbuilder.serialization.token.NumberToken.LongToken;
import io.soabase.recordbuilder.serialization.token.NumberToken.NumberValueToken;
import io.soabase.recordbuilder.serialization.token.Token;
import io.soabase.recordbuilder.serialization.token.ValueToken.BooleanToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.NullToken;
import io.soabase.recordbuilder.serialization.token.ValueToken.StringToken;

import java.io.EOFException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.function.Supplier;

public class JsonTokenSupplier implements Supplier<Token> {
    private final CharSupplier charSupplier;
    private final boolean allowSingleQuotedStrings;
    private boolean isDone;
    private boolean hasPushbackChar;
    private char pushbackChar;

    public JsonTokenSupplier(CharSupplier charSupplier) {
        this(charSupplier, true);
    }

    private JsonTokenSupplier(CharSupplier charSupplier, boolean allowSingleQuotedStrings) {
        this.charSupplier = charSupplier;
        this.allowSingleQuotedStrings = allowSingleQuotedStrings;
    }

    public JsonTokenSupplier withStrictStrings() {
        return new JsonTokenSupplier(charSupplier, false);
    }

    @Override
    public Token get() {
        char c = next(true);
        if (isDone) {
            return StreamEndToken.INSTANCE;
        }
        return switch (c) {
        case '{' -> ObjectStartToken.INSTANCE;
        case '}' -> ObjectEndToken.INSTANCE;
        case '[' -> ArrayStartToken.INSTANCE;
        case ']' -> ArrayEndToken.INSTANCE;
        case ',' -> SeparatorToken.INSTANCE;
        case '"' -> consumeString('"');
        case '\'' -> allowSingleQuotedStrings ? consumeString('\'') : DoubleToken.NAN;
        case 't' -> {
            requireNextEquals('r');
            requireNextEquals('u');
            requireNextEquals('e');
            yield BooleanToken.TRUE;
        }
        case 'f' -> {
            requireNextEquals('a');
            requireNextEquals('l');
            requireNextEquals('s');
            requireNextEquals('e');
            yield BooleanToken.FALSE;
        }
        case 'n' -> {
            requireNextEquals('u');
            requireNextEquals('l');
            requireNextEquals('l');
            yield NullToken.INSTANCE;
        }
        default -> consumeNumber(c);
        };
    }

    private Token consumeNumber(char c) {
        // TODO
        StringBuilder numberBuilder = new StringBuilder();
        boolean isDone = false;
        while (!isDone) {
            if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                numberBuilder.append(c);
                c = requireNext(false);
            } else {
                pushbackChar(c);
                isDone = true;
            }
        }

        String number = numberBuilder.toString();
        if (number.isBlank()) {
            hasPushbackChar = false;
            return DoubleToken.NAN;
        }
        return parseNumber(number);
    }

    // copied and modified from JDK sandbox
    private static Token parseNumber(String number) {
        // Check if integral (Java literal format)
        boolean integerOnly = true;
        for (int index = 0; index < number.length(); index++) {
            char c = number.charAt(index);
            if (c == '.' || c == 'e' || c == 'E') {
                integerOnly = false;
                break;
            }
        }
        if (integerOnly) {
            try {
                long l = Long.parseLong(number);
                if (Integer.MIN_VALUE <= l && l <= Integer.MAX_VALUE) {
                    return new IntToken((int) l);
                }
                return new LongToken(l);
            } catch (NumberFormatException _) {
                // TODO
                return DoubleToken.NAN;
            }
        } else {
            var db = Double.parseDouble(number);
            if (Double.isInfinite(db)) {
                return new NumberValueToken(new BigDecimal(number));
            } else {
                return new DoubleToken(db);
            }
        }
    }

    private Token consumeString(char endChar) {
        boolean nextIsEscaped = false;
        boolean isFieldName = false;
        boolean isDone = false;
        StringBuilder str = new StringBuilder();

        while (!isDone) {
            char c = requireNext(false);
            if (nextIsEscaped) {
                str.append(c);
                nextIsEscaped = false;
                continue;
            }

            switch (c) {
            case '\\' -> nextIsEscaped = true;
            case '"' -> {
                if (endChar == '"') {
                    char nextChar = next(true);
                    if (nextChar == ':') {
                        isFieldName = true;
                    } else {
                        pushbackChar(nextChar);
                    }
                    isDone = true;
                } else {
                    str.append(c);
                }
            }
            case '\'' -> {
                if (endChar == '\'') {
                    char nextChar = next(true);
                    if (nextChar == ':') {
                        isFieldName = true;
                    } else {
                        pushbackChar(nextChar);
                    }
                    isDone = true;
                } else {
                    str.append(c);
                }
            }
            default -> str.append(c);
            }
        }

        String value = str.toString();
        return isFieldName ? new FieldNameToken(value) : new StringToken(value);
    }

    private void pushbackChar(char c) {
        if (hasPushbackChar) {
            throw new IllegalStateException("Already has a pushback character");
        }
        hasPushbackChar = true;
        pushbackChar = c;
    }

    private void requireNextEquals(char c) {
        if (requireNext(false) != c) {
            throw new IllegalStateException("Expected '" + c + "' but found a different character");
        }
    }

    private char requireNext(boolean skipWhitespace) {
        char c = next(skipWhitespace);
        if (isDone) {
            // TODO
            throw new UncheckedIOException(new EOFException());
        }
        return c;
    }

    private char next(boolean skipWhitespace) {
        if (hasPushbackChar) {
            hasPushbackChar = false;
            return pushbackChar;
        }

        if (isDone) {
            return 0;
        }
        while (charSupplier.hasNext()) {
            char c = charSupplier.next();
            if (skipWhitespace && Character.isWhitespace(c)) {
                continue;
            }
            return c;
        }
        isDone = true;
        return 0;
    }
}
