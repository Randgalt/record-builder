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

import java.util.*;

import static io.soabase.recordbuilder.test.foo.PointBuilder.Point;
import static org.junit.jupiter.api.Assertions.*;

class TestCollections {
    @Test
    void testRecordBuilderOptionsCopied() {
        try {
            assertNotNull(CollectionInterfaceRecordBuilder.class.getDeclaredMethod("__list", Collection.class));
        } catch (NoSuchMethodException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void testCollectionRecordDefaultValues() {
        var defaultValues = CollectionRecordBuilder.builder().build();
        assertNotNull(defaultValues.l());
        assertNotNull(defaultValues.s());
        assertNotNull(defaultValues.m());
        assertNotNull(defaultValues.c());
    }

    @Test
    void testCollectionRecordImmutable() {
        var list = new ArrayList<String>();
        list.add("one");
        var set = new HashSet<String>();
        set.add("one");
        var map = new HashMap<String, Point>();
        map.put("one", Point(10, 20));
        var collectionAsSet = new HashSet<Point>();
        collectionAsSet.add(Point(30, 40));
        var r = CollectionRecordBuilder.<String, Point>builder()
                .l(list)
                .s(set)
                .m(map)
                .c(collectionAsSet)
                .build();

        assertValues(r, list, set, map, collectionAsSet);
        assertValueChanges(r, list, set, map, collectionAsSet);
        assertImmutable(r);

        var collectionAsList = new ArrayList<Point>();
        var x = CollectionRecordBuilder.<String, Point>builder()
                .l(list)
                .s(set)
                .m(map)
                .c(collectionAsList)
                .build();
        assertTrue(x.c() instanceof List);
    }

    @Test
    void testCollectionRecordImmutableWithers() {
        var r = CollectionRecordBuilder.<String, Point>builder().build();

        var list = new ArrayList<String>();
        list.add("one");
        var set = new HashSet<String>();
        set.add("one");
        var map = new HashMap<String, Point>();
        map.put("one", Point(10, 20));
        var collectionAsSet = new HashSet<Point>();
        collectionAsSet.add(Point(30, 40));
        var x = r.withL(list).withS(set).withM(map).withC(collectionAsSet);

        assertValues(x, list, set, map, collectionAsSet);
        assertValueChanges(x, list, set, map, collectionAsSet);
        assertImmutable(x);
    }

    @Test
    public void testMutableCollectionRecord() {
        var r = MutableCollectionRecordBuilder.builder().build();
        assertNull(r.l());

        var list = new ArrayList<String>();
        list.add("one");
        r = MutableCollectionRecordBuilder.builder().l(list).build();

        assertEquals(r.l(), list);
        list.add("two");
        assertEquals(r.l(), list);
    }

    private void assertImmutable(CollectionRecord<String, Point> r) {
        assertThrows(UnsupportedOperationException.class, () -> r.l().add("hey"));
        assertThrows(UnsupportedOperationException.class, () -> r.s().add("hey"));
        assertThrows(UnsupportedOperationException.class, () -> r.m().put("hey", Point(1, 2)));
        assertThrows(UnsupportedOperationException.class, () -> r.c().add(Point(1, 2)));
    }

    private void assertValueChanges(CollectionRecord<String, Point> r, ArrayList<String> list, HashSet<String> set, HashMap<String, Point> map, HashSet<Point> collectionAsSet) {
        list.add("two");
        set.add("two");
        map.put("two", Point(50, 60));
        collectionAsSet.add(Point(70, 80));

        assertNotEquals(r.l(), list);
        assertNotEquals(r.s(), set);
        assertNotEquals(r.m(), map);
        assertNotEquals(r.c(), collectionAsSet);
    }

    private void assertValues(CollectionRecord<String, Point> r, ArrayList<String> list, HashSet<String> set, HashMap<String, Point> map, HashSet<Point> collectionAsSet) {
        assertEquals(r.l(), list);
        assertEquals(r.s(), set);
        assertEquals(r.m(), map);
        assertEquals(r.c(), collectionAsSet);
        assertTrue(r.c() instanceof Set);
    }
}
