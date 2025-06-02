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

import io.soabase.recordbuilder.serialization.spi.SerializationSink;
import io.soabase.recordbuilder.serialization.spi.SinkWriter;

import static java.util.Objects.requireNonNull;

public class JsonSerializationSink implements SerializationSink {
    private final SinkWriter writer;

    public JsonSerializationSink(SinkWriter writer) {
        this.writer = requireNonNull(writer, "writer is null");
    }

    @Override
    public void startObject() {
        writer.write('{');
    }

    @Override
    public void endObject() {
        writer.write('}');
    }

    @Override
    public void startArray() {
        writer.write('[');
    }

    @Override
    public void endArray() {
        writer.write(']');
    }

    @Override
    public void startField(String name) {
        writer.write('"');
        writeEscaped(name);
        writer.write("\":");
    }

    @Override
    public void separator() {
        writer.write(',');
    }

    @Override
    public void nullValue() {
        writer.write("null");
    }

    @Override
    public void booleanValue(boolean b) {
        writer.write(b ? "true" : "false");
    }

    @Override
    public void intValue(int i) {
        writer.write(Integer.toString(i));
    }

    @Override
    public void longValue(long l) {
        writer.write(Long.toString(l));
    }

    @Override
    public void doubleValue(double d) {
        writer.write(Double.toString(d));
    }

    @Override
    public void numberValue(Number n) {
        writer.write(n.toString());
    }

    @Override
    public void stringValue(String s) {
        if (s == null) {
            nullValue();
        } else {
            writer.write('"');
            writeEscaped(s);
            writer.write('"');
        }
    }

    private void writeEscaped(String s) {
        // TODO double check the AI
        for (char c : s.toCharArray()) {
            switch (c) {
            case '"' -> writer.write("\\\"");
            case '\\' -> writer.write("\\\\");
            case '/' -> writer.write("\\/");
            case '\b' -> writer.write("\\b");
            case '\f' -> writer.write("\\f");
            case '\n' -> writer.write("\\n");
            case '\r' -> writer.write("\\r");
            case '\t' -> writer.write("\\t");
            default -> {
                if (c < 32) {
                    writer.write(String.format("\\u%04x", (int) c));
                } else {
                    writer.write(c);
                }
            }
            }
        }
    }
}
