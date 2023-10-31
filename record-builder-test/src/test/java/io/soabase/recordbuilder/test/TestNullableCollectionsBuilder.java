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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestNullableCollectionsBuilder {

    @Test
    void testNullableCollectionsBuilder_returnsTheActualValuesIfSet() {
        List<Object> list = List.of("list");
        Set<Object> set = Set.of("set");
        Collection<Point> collection = List.of(new Point(10, 20));
        Map<Object, Point> map = Map.of("one", new Point(10, 20));

        NullableCollectionRecordBuilder<Object, Point> builder = NullableCollectionRecordBuilder.builder().l(list)
                .s(set).c(collection).m(map);

        Assertions.assertEquals(list, builder.l());
        Assertions.assertEquals(set, builder.s());
        Assertions.assertEquals(collection, builder.c());
        Assertions.assertEquals(map, builder.m());

        var record = builder.build();
        Assertions.assertEquals(list, record.l());
        Assertions.assertEquals(set, record.s());
        Assertions.assertEquals(collection, record.c());
        Assertions.assertEquals(map, record.m());
    }

    @Test
    void testNullableCollectionsBuilder_whenNullsAreNotInterpreted_returnsNullForAllComponents_whenNullsAreNotInterpreted() {
        NullableCollectionRecordBuilder<Object, Point> builder = NullableCollectionRecordBuilder.builder();

        Assertions.assertNull(builder.l());
        Assertions.assertNull(builder.m());
        Assertions.assertNull(builder.c());
        Assertions.assertNull(builder.s());

        var record = builder.build();
        Assertions.assertNull(record.l());
        Assertions.assertNull(record.m());
        Assertions.assertNull(record.c());
        Assertions.assertNull(record.s());
    }

    @Test
    void testNullableCollectionBuilder_whenNullsAreInterpreted_returnsNullOrEmptyCollectionBasedOnComponentNullability() {
        // NotNull - list, collection
        // Nullable - set, map
        NullableCollectionRecordInterpretingNotNullsBuilder<Object, Point> builder = NullableCollectionRecordInterpretingNotNullsBuilder
                .builder();

        Assertions.assertNotNull(builder.l());
        Assertions.assertTrue(builder.l().isEmpty());

        Assertions.assertNull(builder.m());
        Assertions.assertNull(builder.s());

        Assertions.assertNotNull(builder.c());
        Assertions.assertTrue(builder.c().isEmpty());

        var record = builder.build();
        Assertions.assertNotNull(record.l());
        Assertions.assertTrue(record.l().isEmpty());

        Assertions.assertNull(record.m());
        Assertions.assertNull(record.s());

        Assertions.assertNotNull(record.c());
        Assertions.assertTrue(record.c().isEmpty());
    }
}
