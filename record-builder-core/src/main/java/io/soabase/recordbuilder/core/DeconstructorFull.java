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
package io.soabase.recordbuilder.core;

import java.lang.annotation.*;

/**
 * An alternate form of {@code @Deconstructor} that has most optional features turned on for the generated record
 * builder
 */
@RecordBuilder.DeconstructorTemplate(options = @RecordBuilder.Options(interpretNotNulls = true, useImmutableCollections = true, addSingleItemCollectionBuilders = true, addFunctionalMethodsToWith = true, addClassRetainedGenerated = true))
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Inherited
public @interface DeconstructorFull {
}
