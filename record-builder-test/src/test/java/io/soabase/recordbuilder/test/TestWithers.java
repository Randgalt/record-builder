/**
 * Copyright 2019 Jordan Zimmerman
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

class TestWithers {
    @Test
    void testWithers() {
        var r1 = new SimpleGenericRecord<>(10, List.of("1", "2", "3"));
        var r2 = r1.withS(List.of("4", "5"));
        var r3 = r2.withI(20);
        Assertions.assertEquals(10, r1.i());
        Assertions.assertEquals(List.of("1", "2", "3"), r1.s());
        Assertions.assertEquals(10, r2.i());
        Assertions.assertEquals(List.of("4", "5"), r2.s());
        Assertions.assertEquals(20, r3.i());
        Assertions.assertEquals(List.of("4", "5"), r2.s());
    }

    @Test
    void testWitherBuilder() {
        var r1 = new SimpleGenericRecord<>(10, "ten");
        var r2 = r1.with().i(20).s("twenty").build();
        var r3 = r2.with().s("changed");
        Assertions.assertEquals(10, r1.i());
        Assertions.assertEquals("ten", r1.s());
        Assertions.assertEquals(20, r2.i());
        Assertions.assertEquals("twenty", r2.s());
        Assertions.assertEquals(20, r3.i());
        Assertions.assertEquals("changed", r3.s());
    }

    @Test
    void testWitherBuilderConsumer() {
        var r1 = new SimpleGenericRecord<>(10, "ten");
        var r2 = r1.with(r -> r.i(15));
        var r3 = r1.with(r -> r.s("twenty").i(20));
        Assertions.assertEquals(10, r1.i());
        Assertions.assertEquals("ten", r1.s());
        Assertions.assertEquals(15, r2.i());
        Assertions.assertEquals("ten", r2.s());
        Assertions.assertEquals(20, r3.i());
        Assertions.assertEquals("twenty", r3.s());
    }

    private static class BadSubclass implements PersonRecordBuilder.With {}

    @Test
    void testBadWithSubclass() {
        Assertions.assertThrows(RuntimeException.class, () -> new BadSubclass().withAge(10));
    }
}
