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

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

public interface CharSupplier {
    char next();

    default boolean hasNext() {
        return false;
    }

    static CharSupplier of(String string) {
        return new CharSupplier() {
            private int index = 0;

            @Override
            public char next() {
                if (index >= string.length()) {
                    throw new IndexOutOfBoundsException("No more characters in the string");
                }
                return string.charAt(index++);
            }

            @Override
            public boolean hasNext() {
                return index < string.length();
            }
        };
    }

    static CharSupplier of(Reader reader) {
        return new CharSupplier() {
            private char next;
            private boolean isDone;

            {
                advance();
            }

            @Override
            public boolean hasNext() {
                return !isDone;
            }

            @Override
            public char next() {
                if (isDone) {
                    throw new IndexOutOfBoundsException("No more characters to read");
                }
                char result = next;
                advance();
                return result;
            }

            private void advance() {
                if (isDone) {
                    return;
                }
                try {
                    int b = reader.read();
                    if (b < 0) {
                        isDone = true;
                    } else {
                        next = (char) b;
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}
