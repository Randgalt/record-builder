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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class TestParameterizedBuilder {
    @Test
    void testNoSubclass() {
        ParameterizedBuilderBuilder<?> builder = ParameterizedBuilderBuilder.builder();
        builder = builder.foo("foo");
        ParameterizedBuilder record = builder.build();
        Assertions.assertEquals("foo", record.foo());
    }

    static final class SpecialBuilder extends ParameterizedBuilderBuilder<SpecialBuilder> {
        SpecialBuilder doubleSetFoo(String s) {
            foo(s + s);
            return this;
        }
    }

    @Test
    void testWithSubclass() {
        SpecialBuilder builder = new SpecialBuilder();
        builder = builder.foo("bar"); // ensure foo returns SpecialBuilder
        builder = builder.doubleSetFoo("foo");
        ParameterizedBuilder record = builder.build();
        Assertions.assertEquals("foofoo", record.foo());
    }
}
