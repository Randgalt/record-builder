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
package io.soabase.recordbuilder.test.jacoco;

import io.soabase.recordbuilder.core.RecordBuilderFull;
import io.soabase.recordbuilder.core.RecordBuilderGenerated;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@RecordBuilderFull
@RecordBuilderGenerated
public record FullRecordForJacoco(@NotNull List<Number> numbers, @NotNull Map<Number, FullRecordForJacoco> fullRecords,
        @NotNull String justAString) {
}
