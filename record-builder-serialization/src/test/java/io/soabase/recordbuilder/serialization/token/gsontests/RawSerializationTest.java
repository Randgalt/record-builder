/*
 * Copyright (C) 2010 Google Inc.
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
package io.soabase.recordbuilder.serialization.token.gsontests;

import io.soabase.com.google.inject.TypeLiteral;
import io.soabase.recordbuilder.core.RecordBuilder.Deconstructor;
import io.soabase.recordbuilder.serialization.RecordBuilderSerializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static io.soabase.recordbuilder.serialization.standard.StandardSerializers.standardRegistry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests to validate serialization of parameterized types without explicit types
 *
 * @author Inderjeet Singh
 */
public class RawSerializationTest {

  private final RecordBuilderSerializer serializer;

  public RawSerializationTest() {
    serializer = new RecordBuilderSerializer(standardRegistry());
  }

  @Test
  public void testCollectionOfPrimitives() {
    Collection<Integer> ints = Arrays.asList(1, 2, 3, 4, 5);
    String json = serializer.toJson(ints);
    assertThat(json).isEqualTo("[1,2,3,4,5]");
  }

  @Test
  public void testCollectionOfObjects() {
    Collection<Foo> foos = Arrays.asList(new Foo(1), new Foo(2));
    String json = serializer.toJson(foos);
    assertThat(json).isEqualTo("[{\"b\":1},{\"b\":2}]");
  }

  @Test
  public void testParameterizedObject() {
    Bar<Foo> bar = new Bar<>(new Foo(1));
    String expectedJson = "{\"t\":{\"b\":1}}";
    // Ensure that serialization works without specifying the type explicitly
    String json = serializer.toJson(bar);
    assertThat(json).isEqualTo(expectedJson);
    // Ensure that serialization also works when the type is specified explicitly
    json = serializer.toJson(bar, new TypeLiteral<Bar<Foo>>() {}.getType());
    assertThat(json).isEqualTo(expectedJson);
  }

  @Test
  public void testTwoLevelParameterizedObject() {
    Bar<Bar<Foo>> bar = new Bar<>(new Bar<>(new Foo(1)));
    String expectedJson = "{\"t\":{\"t\":{\"b\":1}}}";
    // Ensure that serialization works without specifying the type explicitly
    String json = serializer.toJson(bar);
    assertThat(json).isEqualTo(expectedJson);
    // Ensure that serialization also works when the type is specified explicitly
    json = serializer.toJson(bar, new TypeLiteral<Bar<Bar<Foo>>>() {}.getType());
    assertThat(json).isEqualTo(expectedJson);
  }

  @Test
  public void testThreeLevelParameterizedObject() {
    Bar<Bar<Bar<Foo>>> bar = new Bar<>(new Bar<>(new Bar<>(new Foo(1))));
    String expectedJson = "{\"t\":{\"t\":{\"t\":{\"b\":1}}}}";
    // Ensure that serialization works without specifying the type explicitly
    String json = serializer.toJson(bar);
    assertThat(json).isEqualTo(expectedJson);
    // Ensure that serialization also works when the type is specified explicitly
    json = serializer.toJson(bar, new TypeLiteral<Bar<Bar<Bar<Foo>>>>() {}.getType());
    assertThat(json).isEqualTo(expectedJson);
  }

  // TODO JZ - changed to public for now
  public static class Foo {
    @SuppressWarnings("unused")
    int b;

    Foo(int b) {
      this.b = b;
    }

    @Deconstructor
    public void deconstructor(IntConsumer b) {
      b.accept(this.b);
    }
  }

  // TODO JZ - changed to public for now
  public static class Bar<T> {
    @SuppressWarnings("unused")
    T t;

    Bar(T t) {
      this.t = t;
    }

    @Deconstructor
    public void deconstructor(Consumer<T> t) {
      t.accept(this.t);
    }
  }
}