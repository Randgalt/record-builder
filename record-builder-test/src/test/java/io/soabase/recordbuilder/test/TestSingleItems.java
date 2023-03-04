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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestSingleItems {
    @Test
    public void testInternalCollections()
    {
        var now = Instant.now();
        var item = SingleItemsBuilder.<String>builder()
                .add1Map(now, "now")
                .add1Map(Instant.MIN, "before")
                .add1Sets(Arrays.asList("1", "2"))
                .add1Sets(List.of("3"))
                .add1Strings("a")
                .add1Strings("b")
                .add1Strings("c")
                .build();
        Assertions.assertEquals(item.map(), Map.of(now, "now", Instant.MIN, "before"));
        Assertions.assertEquals(item.sets(), Set.of(List.of("1", "2"), List.of("3")));
        Assertions.assertEquals(item.strings(), List.of("a", "b", "c"));

        var copy = item.with()
                .add1Strings("new")
                .add1Map(Instant.MAX, "after")
                .add1Sets(List.of("10", "20", "30"))
                .build();
        Assertions.assertNotEquals(item, copy);
        Assertions.assertEquals(copy.map(), Map.of(now, "now", Instant.MIN, "before", Instant.MAX, "after"));
        Assertions.assertEquals(copy.sets(), Set.of(List.of("1", "2"), List.of("3"), List.of("10", "20", "30")));
        Assertions.assertEquals(copy.strings(), List.of("a", "b", "c", "new"));

        var stringsToAdd = Arrays.asList("x", "y", "z");
        var listToAdd = Arrays.asList(List.of("aa", "bb"), List.of("cc"));
        var mapToAdd = Map.of(now.plusMillis(1), "now+1", now.plusMillis(2), "now+2");
        var streamed = SingleItemsBuilder.builder(item)
                .add1Strings(stringsToAdd.stream())
                .add1Sets(listToAdd.stream())
                .add1Map(mapToAdd.entrySet().stream())
                .build();
        Assertions.assertEquals(streamed.map(), Map.of(now, "now", Instant.MIN, "before", now.plusMillis(1), "now+1", now.plusMillis(2), "now+2"));
        Assertions.assertEquals(streamed.sets(), Set.of(List.of("1", "2"), List.of("3"), List.of("aa", "bb"), List.of("cc")));
        Assertions.assertEquals(streamed.strings(), Arrays.asList("a", "b", "c", "x", "y", "z"));

        var nulls = SingleItemsBuilder.builder(item)
                .strings(null)
                .sets(null)
                .map(null)
                .build();
        Assertions.assertEquals(nulls.map(), Map.of());
        Assertions.assertEquals(nulls.sets(), Set.of());
        Assertions.assertEquals(nulls.strings(), List.of());
    }
}
