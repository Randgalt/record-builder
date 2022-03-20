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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TestRecordBuilderFull {
    @Test
    void testNonNull() {
        var record = FullRecordBuilder.builder().justAString("").build();
        Assertions.assertEquals(List.of(), record.numbers());
        Assertions.assertEquals(Map.of(), record.fullRecords());
    }

    @Test
    void testImmutable() {
        var record = FullRecordBuilder.builder()
                .fullRecords(new HashMap<>())
                .numbers(new ArrayList<>())
                .justAString("")
                .build();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> record.fullRecords().put(1, record));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> record.numbers().add(1));
    }
}
