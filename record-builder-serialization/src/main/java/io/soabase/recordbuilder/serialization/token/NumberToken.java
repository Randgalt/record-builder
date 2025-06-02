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

public sealed interface NumberToken extends ValueToken {
    Number value();

    record ByteToken(byte b) implements NumberToken {
        public static ByteToken of(byte b) {
            return new ByteToken(b);
        }

        @Override
        public Number value() {
            return b;
        }
    }

    record ShortToken(short s) implements NumberToken {
        public static ShortToken of(short s) {
            return new ShortToken(s);
        }

        @Override
        public Number value() {
            return s;
        }
    }

    record IntToken(int i) implements NumberToken {
        public static IntToken of(int i) {
            return new IntToken(i);
        }

        @Override
        public Number value() {
            return i;
        }
    }

    record LongToken(long l) implements NumberToken {
        public static LongToken of(long l) {
            return new LongToken(l);
        }

        @Override
        public Number value() {
            return l;
        }
    }

    record FloatToken(float f) implements NumberToken {
        public static FloatToken of(float f) {
            return new FloatToken(f);
        }

        @Override
        public Number value() {
            return f;
        }
    }

    record DoubleToken(double d) implements NumberToken {
        public static final DoubleToken NAN = new DoubleToken(Double.NaN);

        public static DoubleToken of(double d) {
            return new DoubleToken(d);
        }

        @Override
        public Number value() {
            return d;
        }
    }

    record NumberValueToken(Number value) implements NumberToken {
        public static final NumberValueToken NULL = new NumberValueToken(null);

        public static NumberValueToken of(Number n) {
            return new NumberValueToken(n);
        }
    }
}
