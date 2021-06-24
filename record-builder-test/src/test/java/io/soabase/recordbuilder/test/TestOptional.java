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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class TestOptional {
    @Test
    void testDefaultEmpty() {
        var record = RecordWithOptionalBuilder.builder();
        Assertions.assertEquals(Optional.empty(), record.value());
        Assertions.assertEquals(Optional.empty(), record.raw());
        Assertions.assertEquals(OptionalInt.empty(), record.i());
        Assertions.assertEquals(OptionalLong.empty(), record.l());
        Assertions.assertEquals(OptionalDouble.empty(), record.d());
    }
}
