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
package io.soabase.recordbuilder.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class TestParameterizedGenericBuilder {
    @Test
    void testNoSubclass() {
        ParameterizedGenericBuilderBuilder<Integer, ?> builder = ParameterizedGenericBuilderBuilder.builder();
        builder = builder.bar(1);
        builder = builder.foo("foo");
        ParameterizedGenericBuilder<Integer> record = builder.build();
        Assertions.assertEquals("foo", record.foo());
        Assertions.assertEquals(1, record.bar());
    }

    static final class SpecialBuilder<T extends Number>
            extends ParameterizedGenericBuilderBuilder<T, SpecialBuilder<T>> {
        SpecialBuilder<T> doubleSetFoo(String s) {
            foo(s + s);
            return this;
        }

        SpecialBuilder<T> setIfPositive(T t) {
            if (t.doubleValue() > 0) {
                bar(t);
            }

            return this;
        }
    }

    @Test
    void testWithSubclass() {
        SpecialBuilder<Integer> builder = new SpecialBuilder<>();
        builder = builder.foo("baz"); // ensure foo returns SpecialBuilder
        builder = builder.doubleSetFoo("foo");
        builder = builder.bar(-3);
        builder = builder.setIfPositive(4);
        ParameterizedGenericBuilder<Integer> record = builder.build();
        Assertions.assertEquals("foofoo", record.foo());
        Assertions.assertEquals(4, record.bar());
    }

    @Test
    void testWither() {
        ParameterizedGenericBuilderBuilder<Integer, ?> builder = new ParameterizedGenericBuilder<>("foo", 1).with();
        builder.bar(2);
        ParameterizedGenericBuilder<Integer> record = builder.build();
        Assertions.assertEquals(2, record.bar());

        record = record.withFoo("foofoo");
        Assertions.assertEquals("foofoo", record.foo());

        record = record.with(b -> b.bar(4));
        Assertions.assertEquals(4, record.bar());
    }

    @Test
    void testFrom() {
        ParameterizedGenericBuilderBuilder.With<Integer> wither = ParameterizedGenericBuilderBuilder
                .from(new ParameterizedGenericBuilder<>("foo", 1));
        ParameterizedGenericBuilder<Integer> record = wither.withFoo("foofoo");
        Assertions.assertEquals("foofoo", record.foo());
    }

    @Test
    void testStream() {
        ParameterizedGenericBuilder<Integer> record = new ParameterizedGenericBuilder<>("foo", 1);
        var list = ParameterizedGenericBuilderBuilder.stream(record).map(Map.Entry::getValue).toList();
        Assertions.assertEquals(List.of("foo", 1), list);
    }

    @Test
    void testStaged() {
        ParameterizedGenericBuilderBuilder.FooStage<Number> stage = ParameterizedGenericBuilderBuilder.stagedBuilder();
        ParameterizedGenericBuilder<Number> record = stage.foo("foo").bar(1).build();
        Assertions.assertEquals("foo", record.foo());
    }
}
