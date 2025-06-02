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

import java.util.HashSet;
import java.util.Set;

public class PrettySerializationSink implements SerializationSink {
    private final SerializationSink delegate;
    private final SinkWriter sinkWriter;
    private final String newLine;
    private final String indent;
    private final Set<Option> options;
    private int indentLevel;

    public enum Option {
        BRACES_ON_NEW_LINE, NEW_LINE_AFTER_COMMA, NEW_LINE_AFTER_FIELD_NAME,
    }

    public PrettySerializationSink(SerializationSink delegate, SinkWriter sinkWriter) {
        this(delegate, sinkWriter, "\n", "  ", Set.of());
    }

    private PrettySerializationSink(SerializationSink delegate, SinkWriter sinkWriter, String newLine, String indent,
            Set<Option> options) {
        this.delegate = delegate;
        this.sinkWriter = sinkWriter;
        this.newLine = newLine;
        this.indent = indent;
        this.options = Set.copyOf(options);
    }

    public PrettySerializationSink withNewLineValue(String newLine) {
        return new PrettySerializationSink(delegate, sinkWriter, newLine, indent, options);
    }

    public PrettySerializationSink withIndentValue(String indent) {
        return new PrettySerializationSink(delegate, sinkWriter, newLine, indent, options);
    }

    public PrettySerializationSink withOption(Option option) {
        Set<Option> newOptions = new HashSet<>(options);
        newOptions.add(option);
        return new PrettySerializationSink(delegate, sinkWriter, newLine, indent, newOptions);
    }

    @Override
    public void startObject() {
        writeIndent();
        indent();
        delegate.startObject();
    }

    @Override
    public void endObject() {
        unindent();
        writeIndent();
        delegate.endObject();
    }

    @Override
    public void startArray() {
        if (options.contains(Option.BRACES_ON_NEW_LINE)) {
            writeNewline();
            indent();
        }
        delegate.startArray();
        writeNewline();
    }

    @Override
    public void endArray() {
        if (options.contains(Option.BRACES_ON_NEW_LINE)) {
            writeNewline();
            indent();
        }
        delegate.endArray();
        writeNewline();
    }

    @Override
    public void startField(String name) {
        indent();
        if (options.contains(Option.NEW_LINE_AFTER_FIELD_NAME)) {
            writeNewline();
        } else {
            writeSpace();
        }
        delegate.startField(name);
    }

    @Override
    public void separator() {
        delegate.separator();
        if (options.contains(Option.NEW_LINE_AFTER_COMMA)) {
            writeNewline();
        } else {
            writeSpace();
        }
    }

    @Override
    public void nullValue() {
        delegate.nullValue();
    }

    @Override
    public void booleanValue(boolean b) {
        delegate.booleanValue(b);
    }

    @Override
    public void byteValue(byte b) {
        delegate.byteValue(b);
    }

    @Override
    public void shortValue(short s) {
        delegate.shortValue(s);
    }

    @Override
    public void intValue(int i) {
        delegate.intValue(i);
    }

    @Override
    public void longValue(long l) {
        delegate.longValue(l);
    }

    @Override
    public void floatValue(float f) {
        delegate.floatValue(f);
    }

    @Override
    public void doubleValue(double d) {
        delegate.doubleValue(d);
    }

    @Override
    public void numberValue(Number n) {
        delegate.numberValue(n);
    }

    @Override
    public void stringValue(String s) {
        delegate.stringValue(s);
    }

    public void indent() {
        ++indentLevel;
    }

    public void unindent() {
        --indentLevel;
        if (indentLevel < 0) {
            throw new IllegalStateException("Indent level cannot be negative");
        }
    }

    public void writeNewline() {
        sinkWriter.write(newLine);
    }

    public void writeSpace() {
        sinkWriter.write(' ');
    }

    public void writeIndent() {
        for (int i = 0; i < indentLevel; i++) {
            sinkWriter.write(indent);
        }
    }
}
