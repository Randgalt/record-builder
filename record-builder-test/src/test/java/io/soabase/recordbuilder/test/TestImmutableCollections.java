/*
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
import java.util.*;

public class TestImmutableCollections {
    @Test
    public void testImmutableListNotCopiedWhenNotChanged() {
        var item = CollectionCopyingBuilder.<String> builder().addList("a").addList("b").addList("c").build();
        Assertions.assertEquals(item.list(), List.of("a", "b", "c"));

        var oldList = item.list();

        var copy = item.with().count(1).build();

        Assertions.assertSame(oldList, copy.list());

        var otherCopy = item.with().count(2).build();

        Assertions.assertSame(oldList, otherCopy.list());
    }

    @Test
    public void testImmutableSetNotCopiedWhenNotChanged() {
        var item = CollectionCopyingBuilder.<String> builder().addSet(Arrays.asList("1", "2", "3")).build();
        Assertions.assertEquals(item.set(), Set.of("1", "2", "3"));

        var oldSet = item.set();

        var copy = item.with().count(1).build();

        Assertions.assertSame(oldSet, copy.set());

        var otherCopy = item.with().count(2).build();

        Assertions.assertSame(oldSet, otherCopy.set());
    }

    @Test
    public void testImmutableCollectionNotCopiedWhenNotChanged() {
        var item = CollectionCopyingBuilder.<String> builder().collection(List.of("foo", "bar", "baz")).build();
        Assertions.assertEquals(item.collection(), List.of("foo", "bar", "baz"));

        var oldCollection = item.collection();

        var copy = item.with().count(1).build();

        Assertions.assertSame(oldCollection, copy.collection());

        var otherCopy = item.with().count(2).build();

        Assertions.assertSame(oldCollection, otherCopy.collection());
    }

    @Test
    public void testImmutableMapNotCopiedWhenNotChanged() {
        var item = CollectionCopyingBuilder.<String> builder().addMap(Instant.MAX, "future")
                .addMap(Instant.MIN, "before").build();
        Assertions.assertEquals(item.map(), Map.of(Instant.MAX, "future", Instant.MIN, "before"));

        var oldMap = item.map();

        var copy = item.with().count(1).build();

        Assertions.assertSame(oldMap, copy.map());

        var otherCopy = item.with().count(2).build();

        Assertions.assertSame(oldMap, otherCopy.map());
    }

    @Test
    void testSourceListNotModified() {
        var item = new CollectionCopying<>(new ArrayList<>(), null, null, null, 0);
        var modifiedItem = CollectionCopyingBuilder.builder(item).addList("a").build();

        Assertions.assertEquals(modifiedItem.list(), List.of("a"));
        Assertions.assertTrue(item.list().isEmpty());
    }

    @Test
    void testSourceSetNotModified() {
        var item = new CollectionCopying<>(null, new HashSet<>(), null, null, 0);
        var modifiedItem = CollectionCopyingBuilder.builder(item).addSet("a").build();

        Assertions.assertEquals(modifiedItem.set(), Set.of("a"));
        Assertions.assertTrue(item.set().isEmpty());
    }

    @Test
    void testSourceMapNotModified() {
        var item = new CollectionCopying<>(null, null, new HashMap<>(), null, 0);
        var modifiedItem = CollectionCopyingBuilder.builder(item).addMap(Instant.MIN, "a").build();

        Assertions.assertEquals(modifiedItem.map(), Map.of(Instant.MIN, "a"));
        Assertions.assertTrue(item.map().isEmpty());
    }
}
