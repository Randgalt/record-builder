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

public sealed interface MetaToken extends Token {
    final class ArrayStartToken implements MetaToken {
        public static final ArrayStartToken INSTANCE = new ArrayStartToken();

        private ArrayStartToken() {
        }
    }

    final class ArrayEndToken implements MetaToken {
        public static final ArrayEndToken INSTANCE = new ArrayEndToken();

        private ArrayEndToken() {
        }
    }

    final class ObjectStartToken implements MetaToken {
        public static final ObjectStartToken INSTANCE = new ObjectStartToken();

        private ObjectStartToken() {
        }
    }

    final class ObjectEndToken implements MetaToken {
        public static final ObjectEndToken INSTANCE = new ObjectEndToken();

        private ObjectEndToken() {
        }
    }

    final class SeparatorToken implements MetaToken {
        public static final SeparatorToken INSTANCE = new SeparatorToken();

        private SeparatorToken() {
        }
    }

    final class StreamEndToken implements MetaToken {
        public static final StreamEndToken INSTANCE = new StreamEndToken();

        private StreamEndToken() {
        }
    }

    record FieldNameToken(String fieldName) implements MetaToken {
        public static FieldNameToken of(String fieldName) {
            return new FieldNameToken(fieldName);
        }
    }
}
