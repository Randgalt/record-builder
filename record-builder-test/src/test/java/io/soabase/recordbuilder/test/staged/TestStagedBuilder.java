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
package io.soabase.recordbuilder.test.staged;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStagedBuilder {
    @Test
    void testSimple() {
        var now = Instant.now();
        var obj = SimpleStagedBuilder.builder().i(1).s("s").instant(now).build();
        assertEquals(new SimpleStaged(1, "s", now), obj);
    }

    @Test
    void testSimpleCombined() {
        var now = Instant.now();
        var obj1 = CombinedSimpleStagedBuilder.builder().i(1).s("s").instant(now).build();
        var obj2 = CombinedSimpleStagedBuilder.stagedBuilder().i(1).s("s").instant(now).build();
        assertEquals(obj1, obj2);
    }

    @Test
    void testGeneric() {
        var now = Instant.now();
        var obj = SimpleStagedBuilder.builder().i(1).s("s").instant(now).build();

        GenericStaged<SimpleStaged, String> generic = GenericStagedBuilder.<SimpleStaged, String> builder().name("name")
                .aT(obj).theUThing("thing").build();

        assertEquals(new GenericStaged<>("name", new SimpleStaged(1, "s", now), "thing"), generic);
    }

    @Test
    void testGenericCombined() {
        var now = Instant.now();
        var builder = SimpleStagedBuilder.builder().i(1).s("s").instant(now).builder();

        var obj1 = CombinedGenericStagedBuilder.builder().name("name")
                .aT(new GenericStaged<>("other", builder.build(), BigInteger.TEN)).theUThing(BigDecimal.ONE).build();
        var obj2 = CombinedGenericStagedBuilder.stagedBuilder().name("name")
                .aT(new GenericStaged<>("other", builder.build(), BigInteger.TEN)).theUThing(BigDecimal.ONE).build();
        assertEquals(obj1, obj2);
    }

    @Test
    void testSingleField() {
        SingleFieldStaged obj = SingleFieldStagedBuilder.builder().i(1).build();
        assertEquals(new SingleFieldStaged(1), obj);
    }

    @Test
    void testNoFields() {
        NoFieldsStaged obj = NoFieldsStagedBuilder.builder().build();
        assertEquals(new NoFieldsStaged(), obj);
    }
}
