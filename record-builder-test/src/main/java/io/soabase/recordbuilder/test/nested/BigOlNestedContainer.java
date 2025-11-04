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
package io.soabase.recordbuilder.test.nested;

import io.soabase.recordbuilder.core.RecordBuilderFull;
import io.soabase.recordbuilder.test.*;

@RecordBuilderFull
public record BigOlNestedContainer<T, X extends Point>(Annotated annotated, CollectionCopying<T> collectionCopying,
        CollectionRecord<T, X> collectionRecord, FullRecord fullRecord, ConvertRequest convertRequest,
        TemplateTest templateTest, WildcardSingleItems<T> wildcardSingleItems,
        DuplicateMethodNames duplicateMethodNames, BigOlNestedContainer<T, X> recursive)
        implements BigOlNestedContainerBuilder.With<T, X> {
}
