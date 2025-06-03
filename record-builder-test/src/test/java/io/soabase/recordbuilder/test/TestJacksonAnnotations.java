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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.soabase.recordbuilder.test.JacksonAnnotated.JacksonAnnotatedRecord;
import io.soabase.recordbuilder.test.JacksonAnnotated.JacksonAnnotatedRecordCustomSetterPrefix;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TestJacksonAnnotations {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("recordBuilders")
    void addsJsonPOJOBuilderAnnotation(Class<? extends JacksonAnnotated> type, String expectedPrefix) {
        final var annotations = Arrays.stream(type.getAnnotations()).toList();
        assertThat(annotations).filteredOn(annotation -> annotation.annotationType().equals(JsonPOJOBuilder.class))
                .hasSize(1).first().asInstanceOf(InstanceOfAssertFactories.type(JsonPOJOBuilder.class))
                .satisfies(annotation -> {
                    assertThat(annotation.withPrefix()).isEqualTo(expectedPrefix);
                });
    }

    static Stream<Arguments> recordBuilders() {
        return Stream.of(arguments(JacksonAnnotatedRecordBuilder.class, ""),
                arguments(JacksonAnnotatedRecordCustomSetterPrefixBuilder.class, "set"));
    }

    @ParameterizedTest
    @ValueSource(classes = { JacksonAnnotatedRecord.class, JacksonAnnotatedRecordCustomSetterPrefix.class })
    void deserializingModelInvokesBuilder(Class<? extends JacksonAnnotated> type) throws JsonProcessingException {
        final var json = """
                {
                  "name" : "test"
                }
                """;

        final var model = objectMapper.readValue(json, type);
        assertThat(model.name()).isEqualTo("test");
        assertThat(model.type()).isEqualTo("dummy"); // default value
        assertThat(model.properties()).isNotNull().isEmpty(); // non-null initialized immutable collection
    }
}
