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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestImmutableCollections {
    @Test
    public void testImmutableCollectionNotCopiedWhenNotChanged() {
        var item = CollectionCopyingBuilder.<String>builder()
                .addMap(Instant.MAX, "future")
                .addMap(Instant.MIN, "before")
                .addSet(Arrays.asList("1", "2", "3"))
                .addList("a")
                .addList("b")
                .addList("c")
                .collection(List.of("foo", "bar", "baz"))
                .build();
        Assertions.assertEquals(item.map(), Map.of(Instant.MAX, "future", Instant.MIN, "before"));
        Assertions.assertEquals(item.set(), Set.of("1", "2", "3"));
        Assertions.assertEquals(item.list(), List.of("a", "b", "c"));
        Assertions.assertEquals(item.collection(), List.of("foo", "bar", "baz"));

        var oldList = item.list();
        var oldSet = item.set();
        var oldMap = item.map();

        var copy = item.with()
                .count(1)
                .build();

        Assertions.assertSame(oldList, copy.list());
        Assertions.assertSame(oldSet, copy.set());
        Assertions.assertSame(oldMap, copy.map());

        var otherCopy = item.withCount(2);
        Assertions.assertSame(oldList, otherCopy.list());
        Assertions.assertSame(oldSet, otherCopy.set());
        Assertions.assertSame(oldMap, otherCopy.map());
    }
}
