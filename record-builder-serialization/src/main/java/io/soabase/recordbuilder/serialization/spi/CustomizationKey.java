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
package io.soabase.recordbuilder.serialization.spi;

public record CustomizationKey<T>(Class<T> type) {
    public static final CustomizationKey<Boolean> MAKE_METHODS_ACCESSIBLE = new CustomizationKey<>(Boolean.class);
    public static final CustomizationKey<Boolean> IGNORE_UNKNOWN_FIELDS = new CustomizationKey<>(Boolean.class);
    public static final CustomizationKey<Boolean> DO_NOT_WRITE_NULL_OBJECT_VALUES = new CustomizationKey<>(
            Boolean.class);
}
