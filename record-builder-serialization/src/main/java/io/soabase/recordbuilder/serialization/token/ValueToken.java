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
package io.soabase.recordbuilder.serialization.token;

import io.soabase.recordbuilder.serialization.token.NumberToken.NumberValueToken;

import java.util.NoSuchElementException;

public sealed interface ValueToken
        extends Token permits NumberToken, ValueToken.BooleanToken, ValueToken.NullToken, ValueToken.StringToken {
    Object value();

    final class NullToken implements ValueToken {
        public static final NullToken INSTANCE = new NullToken();

        private NullToken() {
        }

        @Override
        public Object value() {
            return null;
        }

        @Override
        public <T extends Token> T as(Class<T> type) {
            if (type.equals(NullToken.class)) {
                return type.cast(INSTANCE);
            }
            if (type.equals(StringToken.class)) {
                return type.cast(StringToken.NULL);
            }
            if (type.equals(NumberValueToken.class)) {
                return type.cast(NumberValueToken.NULL);
            }
            throw new NoSuchElementException("Cannot cast " + this.getClass().getName() + " to " + type.getName());
        }
    }

    final class BooleanToken implements ValueToken {
        public static final BooleanToken TRUE = new BooleanToken();
        public static final BooleanToken FALSE = new BooleanToken();

        private BooleanToken() {
        }

        @Override
        public Object value() {
            return this == TRUE;
        }
    }

    record StringToken(String value) implements ValueToken {
        public static final StringToken NULL = new StringToken(null);

        public static StringToken of(String s) {
            return new StringToken(s);
        }
    }
}
