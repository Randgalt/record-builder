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
import java.io.UncheckedIOException;
import java.io.Writer;

public interface SinkWriter {
    void write(char c);

    default void write(char[] chars) {
        for (char c : chars) {
            write(c);
        }
    }

    default void write(String s) {
        write(s.toCharArray());
    }

    static SinkWriter of(StringBuilder stringBuilder) {
        return new SinkWriter() {
            @Override
            public void write(char c) {
                stringBuilder.append(c);
            }

            @Override
            public void write(char[] chars) {
                stringBuilder.append(chars);
            }

            @Override
            public void write(String s) {
                stringBuilder.append(s);
            }
        };
    }

    static SinkWriter of(Writer writer) {
        return new SinkWriter() {
            @Override
            public void write(char c) {
                try {
                    writer.write(c);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void write(char[] chars) {
                try {
                    writer.write(chars);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void write(String s) {
                try {
                    writer.write(s);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}
