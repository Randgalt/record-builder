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

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RecordBuilder
@RecordBuilder.Options(useImmutableCollections = true, addFunctionalMethodsToWith = true, detectNestedRecordBuilders = true)
public record CollectionRecord<T, X extends Point>(List<T> l, Set<T> s, Map<T, X> m, Collection<X> c)
        implements CollectionRecordBuilder.With<T, X> {
    public static void main(String[] args) {
        var r = new CollectionRecord<>(List.of("hey"), Set.of("there"), Map.of("one", new Point(10, 20)),
                Set.of(new Point(30, 40)));
        Instant now = r.map((l1, s1, m1, c1) -> Instant.now());
        r.accept((l1, s1, m1, c1) -> {
        });
    }
}
