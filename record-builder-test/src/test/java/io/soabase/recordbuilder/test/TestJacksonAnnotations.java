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
import io.soabase.recordbuilder.test.JacksonAnnotated.JacksonAnnotatedRecordJackson2;
import io.soabase.recordbuilder.test.JacksonAnnotated.JacksonAnnotatedRecordJackson3;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TestJacksonAnnotations {
    private static final Class<?> J2_POJO_BUILDER = JsonPOJOBuilder.class;
    private static final Class<?> J3_POJO_BUILDER = tools.jackson.databind.annotation.JsonPOJOBuilder.class;

    private final ObjectMapper jackson2ObjectMapper = new ObjectMapper();
    private final tools.jackson.databind.ObjectMapper jackson3ObjectMapper = new tools.jackson.databind.ObjectMapper();

    // -------------------------------------------------------------------------
    // Jackson 2 annotation presence
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("jackson2RecordBuilders")
    void addsJackson2JsonPOJOBuilderAnnotation(Class<?> type, String expectedPrefix) {
        final var annotations = Arrays.stream(type.getAnnotations()).toList();
        assertThat(annotations).filteredOn(annotation -> annotation.annotationType().equals(J2_POJO_BUILDER)).hasSize(1)
                .first().asInstanceOf(InstanceOfAssertFactories.type(JsonPOJOBuilder.class))
                .satisfies(annotation -> assertThat(annotation.withPrefix()).isEqualTo(expectedPrefix));
    }

    static Stream<Arguments> jackson2RecordBuilders() {
        return Stream.of(arguments(JacksonAnnotatedRecordBuilder.class, ""),
                arguments(JacksonAnnotatedRecordCustomSetterPrefixBuilder.class, "set"),
                arguments(JacksonAnnotatedRecordJackson2Builder.class, ""));
    }

    // -------------------------------------------------------------------------
    // Jackson 3 annotation presence
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("jackson3RecordBuilders")
    void addsJackson3JsonPOJOBuilderAnnotation(Class<?> type, String expectedPrefix) {
        final var annotations = Arrays.stream(type.getAnnotations()).toList();
        assertThat(annotations).filteredOn(annotation -> annotation.annotationType().equals(J3_POJO_BUILDER)).hasSize(1)
                .first()
                .asInstanceOf(InstanceOfAssertFactories.type(tools.jackson.databind.annotation.JsonPOJOBuilder.class))
                .satisfies(annotation -> assertThat(annotation.withPrefix()).isEqualTo(expectedPrefix));
    }

    static Stream<Arguments> jackson3RecordBuilders() {
        return Stream.of(arguments(JacksonAnnotatedRecordJackson3Builder.class, ""));
    }

    // -------------------------------------------------------------------------
    // AUTO mode: both Jackson 2 and 3 annotations present
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("autoRecordBuilders")
    void addsAnnotationsForBothVersionsInAutoMode(Class<?> type) {
        final var annotations = Arrays.stream(type.getAnnotations()).toList();
        assertThat(annotations).filteredOn(annotation -> annotation.annotationType().equals(J2_POJO_BUILDER))
                .hasSize(1);
        assertThat(annotations).filteredOn(annotation -> annotation.annotationType().equals(J3_POJO_BUILDER))
                .hasSize(1);
    }

    static Stream<Arguments> autoRecordBuilders() {
        return Stream.of(arguments(JacksonAnnotatedRecordBuilder.class),
                arguments(JacksonAnnotatedRecordCustomSetterPrefixBuilder.class));
    }

    // -------------------------------------------------------------------------
    // Version isolation: each explicit version only adds its own annotation
    // -------------------------------------------------------------------------

    @Test
    void jackson2RecordDoesNotHaveJackson3Annotation() {
        final var annotations = Arrays.stream(JacksonAnnotatedRecordJackson2Builder.class.getAnnotations()).toList();
        assertThat(annotations).noneMatch(annotation -> annotation.annotationType().equals(J3_POJO_BUILDER));
    }

    @Test
    void jackson3RecordDoesNotHaveJackson2Annotation() {
        final var annotations = Arrays.stream(JacksonAnnotatedRecordJackson3Builder.class.getAnnotations()).toList();
        assertThat(annotations).noneMatch(annotation -> annotation.annotationType().equals(J2_POJO_BUILDER));
    }

    // -------------------------------------------------------------------------
    // jsonPOJOBuilder = false → no annotation added
    // -------------------------------------------------------------------------

    @Test
    void doesNotAddJsonPOJOBuilderAnnotationWhenDisabled() {
        final var annotations = Arrays.stream(JacksonAnnotatedRecordNoJacksonBuilder.class.getAnnotations()).toList();
        assertThat(annotations).noneMatch(annotation -> annotation.annotationType().equals(J2_POJO_BUILDER));
        assertThat(annotations).noneMatch(annotation -> annotation.annotationType().equals(J3_POJO_BUILDER));
    }

    // -------------------------------------------------------------------------
    // Deserialization round-trips
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(classes = { JacksonAnnotatedRecord.class, JacksonAnnotatedRecordCustomSetterPrefix.class,
            JacksonAnnotatedRecordJackson2.class })
    void deserializingWithJackson2InvokesBuilder(Class<? extends JacksonAnnotated> type)
            throws JsonProcessingException {
        final var json = """
                {
                  "name" : "test"
                }
                """;

        final var model = jackson2ObjectMapper.readValue(json, type);
        assertThat(model.name()).isEqualTo("test");
        assertThat(model.type()).isEqualTo("dummy"); // default value
        assertThat(model.properties()).isNotNull().isEmpty(); // non-null initialized immutable collection
    }

    @Test
    void deserializingWithJackson3InvokesBuilder() throws Exception {
        final var json = """
                {
                  "name" : "test"
                }
                """;

        final var model = jackson3ObjectMapper.readValue(json, JacksonAnnotatedRecordJackson3.class);
        assertThat(model.name()).isEqualTo("test");
        assertThat(model.type()).isEqualTo("dummy"); // default value
        assertThat(model.properties()).isNotNull().isEmpty(); // non-null initialized immutable collection
    }

}
