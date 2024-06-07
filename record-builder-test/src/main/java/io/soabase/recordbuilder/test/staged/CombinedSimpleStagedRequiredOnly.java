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
package io.soabase.recordbuilder.test.staged;

import io.soabase.recordbuilder.core.RecordBuilder;

import javax.validation.constraints.Null;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;

@RecordBuilder
@RecordBuilder.Options(useImmutableCollections = true, builderMode = RecordBuilder.BuilderMode.STANDARD_AND_STAGED_REQUIRED_ONLY)
public record CombinedSimpleStagedRequiredOnly(Set<Integer> retryableStatusCodes, String retryIntervalConfiguration) {
}