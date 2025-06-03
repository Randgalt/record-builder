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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

public interface JacksonAnnotated {
    String name();

    String type();

    Map<String, Object> properties();

    @RecordBuilder
    @RecordBuilder.Options(addJacksonAnnotations = true, useImmutableCollections = true, prefixEnclosingClassNames = false)
    @JsonDeserialize(builder = JacksonAnnotatedRecordBuilder.class)
    record JacksonAnnotatedRecord(String name, @RecordBuilder.Initializer("DEFAULT_TYPE") String type,
            Map<String, Object> properties) implements JacksonAnnotated {
        public static final String DEFAULT_TYPE = "dummy";
    }

    @RecordBuilder
    @RecordBuilder.Options(addJacksonAnnotations = true, useImmutableCollections = true, prefixEnclosingClassNames = false, setterPrefix = "set")
    @JsonDeserialize(builder = JacksonAnnotatedRecordCustomSetterPrefixBuilder.class)
    record JacksonAnnotatedRecordCustomSetterPrefix(String name, @RecordBuilder.Initializer("DEFAULT_TYPE") String type,
            Map<String, Object> properties) implements JacksonAnnotated {
        public static final String DEFAULT_TYPE = "dummy";
    }
}
