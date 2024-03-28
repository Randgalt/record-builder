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
package io.soabase.recordbuilder.test.deconstruct;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTuples {
    @Test
    public void testGeneric() {
        var now = Instant.now();

        assertThat(test(new Generic<>("hey", 12))).isEqualTo("String/Integer:hey:12");
        assertThat(test(new Generic<>(now, true))).isEqualTo("Instant/Boolean:%s:true".formatted(now));
        assertThat(test(new Generic<>(1.0, 2.0))).isEqualTo("dunno");
    }

    private String test(Generic<?, ?> instance) {
        return switch (GenericShim.to(instance)) {
        case GenericShim(String ignore,String s,Integer i) -> "String/Integer:" + s + ":" + i;
        case GenericShim(String ignore,Instant d,Boolean b) -> "Instant/Boolean:" + d + ":" + b;
        default -> "dunno";
        };
    }
}
