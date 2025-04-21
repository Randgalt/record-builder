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

import java.util.List;
import java.util.Map;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
@RecordBuilder.Options(getterPrefix = "get", booleanPrefix = "is", addFunctionalMethodsToWith = true)
public record CustomMethodNamesWithFunctionalMethods<K, V>(Map<K, V> kvMap, int theValue, List<Integer> theList,
        boolean theBoolean) implements CustomMethodNamesWithFunctionalMethodsBuilder.With {
}
