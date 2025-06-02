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

public interface SerializationSink {
    void startObject();

    void endObject();

    void startArray();

    void endArray();

    void startField(String name);

    void separator();

    void nullValue();

    void booleanValue(boolean b);

    default void byteValue(byte b) {
        numberValue(b);
    }

    default void shortValue(short s) {
        numberValue(s);
    }

    default void intValue(int i) {
        numberValue(i);
    }

    default void longValue(long l) {
        numberValue(l);
    }

    default void floatValue(float f) {
        numberValue(f);
    }

    default void doubleValue(double d) {
        numberValue(d);
    }

    void numberValue(Number n);

    void stringValue(String s);
}
