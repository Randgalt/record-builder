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
import io.soabase.recordbuilder.core.RecordBuilderFull;

import java.util.Optional;

@RecordBuilderFull
public record Initialized(@RecordBuilder.Initializer("DEFAULT_NAME") String name,
        @RecordBuilder.Initializer("defaultAge") int age, Optional<String> dummy,
        @RecordBuilder.Initializer(value = "defaultAltName", source = AltSource.class) Optional<String> altName) {

    public static final String DEFAULT_NAME = "hey";

    public static int defaultAge() {
        return 18;
    }

    public static class AltSource {
        public static Optional<String> defaultAltName() {
            return Optional.of("alt");
        }
    }
}
