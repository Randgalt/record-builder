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
@RecordBuilder.Options(addSingleItemCollectionBuilders = true, singleItemBuilderPrefix = "add1", useImmutableCollections = true, addFunctionalMethodsToWith = true)
public record SingleItems<T>(List<String> strings, Set<List<T>> sets, Map<Instant, T> map, Collection<T> collection)
        implements SingleItemsBuilder.With<T> {
}
