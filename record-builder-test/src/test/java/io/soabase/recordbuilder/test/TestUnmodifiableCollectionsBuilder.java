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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestUnmodifiableCollectionsBuilder {

    @Test
    void shouldWrapCollectionsWithUnmodifiableView() {
        // given
        var list = new ArrayList<Integer>();
        list.add(2);
        list.add(1);
        list.add(0);

        var orderedSet = new LinkedHashSet<String>();
        orderedSet.add("C");
        orderedSet.add("B");
        orderedSet.add("A");

        var orderedMap = new LinkedHashMap<String, Integer>();
        orderedMap.put("C", 2);
        orderedMap.put("B", 1);
        orderedMap.put("A", 0);

        var collection = new HashSet<String>();
        collection.add("C");
        collection.add("B");
        collection.add("A");

        // when
        var record = UnmodifiableCollectionsRecordBuilder.builder().aList(list).orderedSet(orderedSet)
                .orderedMap(orderedMap).aCollection(collection).build();

        // then
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> record.aList().add(9)),
                () -> assertThat(record.aList()).containsExactly(2, 1, 0),
                () -> assertThrows(UnsupportedOperationException.class, () -> record.orderedSet().add("newElement")),
                () -> assertThat(record.orderedSet()).containsExactly("C", "B", "A"),
                () -> assertThrows(UnsupportedOperationException.class, () -> record.orderedMap().put("newElement", 9)),
                () -> assertThat(record.orderedMap()).containsExactly(entry("C", 2), entry("B", 1), entry("A", 0)),
                () -> assertThrows(UnsupportedOperationException.class, () -> record.aCollection().add("newElement")),
                () -> assertThat(record.aCollection()).containsExactlyInAnyOrder("C", "B", "A"));
    }
}
