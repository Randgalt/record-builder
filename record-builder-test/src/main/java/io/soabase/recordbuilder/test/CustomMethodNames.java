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

import java.util.List;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.soabase.recordbuilder.test.CustomMethodNamesBuilder.Bean;

@RecordBuilder
@RecordBuilder.Options(
    setterPrefix = "set", getterPrefix = "get", booleanPrefix = "is", beanClassName = "Bean")
public record CustomMethodNames(
    int theValue,
    List<Integer> theList,
    boolean theBoolean) implements Bean {
}
