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

import io.soabase.recordbuilder.wrappers.Option;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDeconstruct {
    @Test
    public void testOptionalToOption()
    {
        assertThat(check(new OptionalToOption("a", Optional.of("there")))).isEqualTo("a-there");
        assertThat(check(new OptionalToOption("b", Optional.of("here")))).isEqualTo("b-here");
        assertThat(check(new OptionalToOption("0", Optional.empty()))).isEqualTo("0-empty");
    }

    private static String check(OptionalToOption o) {
        return switch (OptionalToOptionHelper.from(o)) {
            case OptionalToOptionHelper(var s, Option(var v)) when s.equals("a") && v.equals("there") -> "a-there";
            case OptionalToOptionHelper(var s, Option(var v)) when s.equals("b") && v.equals("here") -> "b-here";
            case OptionalToOptionHelper(var s, Option<?> opt) when s.equals("0") && opt.isEmpty() -> "0-empty";
            default -> "";
        };
    }
}
