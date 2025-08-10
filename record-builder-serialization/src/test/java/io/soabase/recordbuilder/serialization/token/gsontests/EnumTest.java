/*
 * Copyright (C) 2008 Google Inc.
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
import io.soabase.recordbuilder.serialization.RecordBuilderSerializer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;

import static io.soabase.recordbuilder.serialization.standard.StandardSerializers.standardRegistry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for Java 5.0 enums.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class EnumTest {
  private final RecordBuilderSerializer serializer;

  public EnumTest() {
    serializer = new RecordBuilderSerializer(standardRegistry());
  }

  @Test
  public void testTopLevelEnumSerialization() {
    String result = serializer.toJson(MyEnum.VALUE1);
    assertThat(result).isEqualTo('"' + MyEnum.VALUE1.toString() + '"');
  }

  @Test
  public void testTopLevelEnumDeserialization() {
    MyEnum result = serializer.fromJson('"' + MyEnum.VALUE1.toString() + '"', MyEnum.class);
    assertThat(result).isEqualTo(MyEnum.VALUE1);
  }

  @Test
  public void testCollectionOfEnumsSerialization() {
    Type type = new TypeLiteral<Collection<MyEnum>>() {}.getType();
    Collection<MyEnum> target = new ArrayList<>();
    target.add(MyEnum.VALUE1);
    target.add(MyEnum.VALUE2);
    String expectedJson = "[\"VALUE1\",\"VALUE2\"]";
    String actualJson = serializer.toJson(target);
    assertThat(actualJson).isEqualTo(expectedJson);
    actualJson = serializer.toJson(target, type);
    assertThat(actualJson).isEqualTo(expectedJson);
  }

  @Test
  public void testCollectionOfEnumsDeserialization() {
    Type type = new TypeLiteral<Collection<MyEnum>>() {}.getType();
    String json = "[\"VALUE1\",\"VALUE2\"]";
    Collection<MyEnum> target = serializer.fromJson(json, type);
    assertThat(target).contains(MyEnum.VALUE1);
    assertThat(target).contains(MyEnum.VALUE2);
  }

  @Test
  public void testClassWithEnumFieldSerialization() {
    ClassWithEnumFields target = new ClassWithEnumFields();
    assertThat(serializer.toJson(target)).isEqualTo(target.getExpectedJson());
  }

  @Test
  public void testClassWithEnumFieldDeserialization() {
    String json = "{value1:'VALUE1',value2:'VALUE2'}";
    ClassWithEnumFields target = serializer.fromJson(json, ClassWithEnumFields.class);
    assertThat(target.value1).isEqualTo(MyEnum.VALUE1);
    assertThat(target.value2).isEqualTo(MyEnum.VALUE2);
  }

  private static enum MyEnum {
    VALUE1,
    VALUE2
  }

  private static class ClassWithEnumFields {
    private final MyEnum value1 = MyEnum.VALUE1;
    private final MyEnum value2 = MyEnum.VALUE2;

    public String getExpectedJson() {
      return "{\"value1\":\"" + value1 + "\",\"value2\":\"" + value2 + "\"}";
    }
  }

  /** Test for issue 226. */
  @Test
  @SuppressWarnings("GetClassOnEnum")
  public void testEnumSubclass() {
    assertThat(Roshambo.ROCK.getClass()).isNotEqualTo(Roshambo.class);
    assertThat(serializer.toJson(Roshambo.ROCK)).isEqualTo("\"ROCK\"");
    assertThat(serializer.toJson(EnumSet.allOf(Roshambo.class)))
        .isEqualTo("[\"ROCK\",\"PAPER\",\"SCISSORS\"]");
    assertThat(serializer.fromJson("\"ROCK\"", Roshambo.class)).isEqualTo(Roshambo.ROCK);
    Set<Roshambo> deserialized =
            serializer.fromJson("[\"ROCK\",\"PAPER\",\"SCISSORS\"]", new TypeLiteral<>() {}.getType());
    assertThat(deserialized).isEqualTo(EnumSet.allOf(Roshambo.class));

    // A bit contrived, but should also work if explicitly deserializing using anonymous enum
    // subclass
    assertThat(serializer.fromJson("\"ROCK\"", Roshambo.ROCK.getClass())).isEqualTo(Roshambo.ROCK);
  }

/*
  @Test
  @SuppressWarnings("GetClassOnEnum")
  public void testEnumSubclassWithRegisteredTypeAdapter() {
    gson =
        new GsonBuilder()
            .registerTypeHierarchyAdapter(Roshambo.class, new MyEnumTypeAdapter())
            .create();
    assertThat(Roshambo.ROCK.getClass()).isNotEqualTo(Roshambo.class);
    assertThat(serializer.toJson(Roshambo.ROCK)).isEqualTo("\"123ROCK\"");
    assertThat(serializer.toJson(EnumSet.allOf(Roshambo.class)))
        .isEqualTo("[\"123ROCK\",\"123PAPER\",\"123SCISSORS\"]");
    assertThat(serializer.fromJson("\"123ROCK\"", Roshambo.class)).isEqualTo(Roshambo.ROCK);
    Set<Roshambo> deserialized =
            serializer.fromJson("[\"123ROCK\",\"123PAPER\",\"123SCISSORS\"]", new TypeLiteral<>() {});
    assertThat(deserialized).isEqualTo(EnumSet.allOf(Roshambo.class));
  }
*/

  @Test
  public void testEnumSubclassAsParameterizedType() {
    Collection<Roshambo> list = new ArrayList<>();
    list.add(Roshambo.ROCK);
    list.add(Roshambo.PAPER);

    String json = serializer.toJson(list);
    assertThat(json).isEqualTo("[\"ROCK\",\"PAPER\"]");

    Type collectionType = new TypeLiteral<Collection<Roshambo>>() {}.getType();
    Collection<Roshambo> actualJsonList = serializer.fromJson(json, collectionType);
    assertThat(actualJsonList).contains(Roshambo.ROCK);
    assertThat(actualJsonList).contains(Roshambo.PAPER);
  }

/*
  @Test
  public void testEnumCaseMapping() {
    assertThat(serializer.fromJson("\"boy\"", Gender.class)).isEqualTo(Gender.MALE);
    assertThat(serializer.toJson(Gender.MALE, Gender.class)).isEqualTo("\"boy\"");
  }
*/

  @Test
  public void testEnumSet() {
    EnumSet<Roshambo> foo = EnumSet.of(Roshambo.ROCK, Roshambo.PAPER);
    String json = serializer.toJson(foo);
    assertThat(json).isEqualTo("[\"ROCK\",\"PAPER\"]");

    Type type = new TypeLiteral<EnumSet<Roshambo>>() {}.getType();
    EnumSet<Roshambo> bar = serializer.fromJson(json, type);
    assertThat(bar).containsExactly(Roshambo.ROCK, Roshambo.PAPER)/*.inOrder()*/;
    assertThat(bar).doesNotContain(Roshambo.SCISSORS);
  }

  @Test
  public void testEnumMap() {
    EnumMap<MyEnum, String> map = new EnumMap<>(MyEnum.class);
    map.put(MyEnum.VALUE1, "test");
    String json = serializer.toJson(map);
    assertThat(json).isEqualTo("{\"VALUE1\":\"test\"}");

    Type type = new TypeLiteral<EnumMap<MyEnum, String>>() {}.getType();
    EnumMap<?, ?> actualMap = serializer.fromJson("{\"VALUE1\":\"test\"}", type);
    Map<?, ?> expectedMap = Collections.singletonMap(MyEnum.VALUE1, "test");
    assertThat(actualMap).isEqualTo(expectedMap);
  }

  private enum Roshambo {
    ROCK {
      @Override
      Roshambo defeats() {
        return SCISSORS;
      }
    },
    PAPER {
      @Override
      Roshambo defeats() {
        return ROCK;
      }
    },
    SCISSORS {
      @Override
      Roshambo defeats() {
        return PAPER;
      }
    };

    @SuppressWarnings("unused")
    abstract Roshambo defeats();
  }

/*
  private static class MyEnumTypeAdapter
      implements JsonSerializer<Roshambo>, JsonDeserializer<Roshambo> {
    @Override
    public JsonElement serialize(Roshambo src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive("123" + src.name());
    }

    @Override
    public Roshambo deserialize(JsonElement json, Type classOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return Roshambo.valueOf(json.getAsString().substring(3));
    }
  }

  private enum Gender {
    @SerializedName("boy")
    MALE,

    @SerializedName("girl")
    FEMALE
  }
*/

  @Test
  public void testEnumClassWithFields() {
    assertThat(serializer.toJson(Color.RED)).isEqualTo("\"RED\"");
    assertThat(serializer.fromJson("RED", Color.class).value).isEqualTo("red");
    assertThat(serializer.fromJson("BLUE", Color.class).index).isEqualTo(2);
  }

  private enum Color {
    RED("red", 1),
    BLUE("blue", 2),
    GREEN("green", 3);
    final String value;
    final int index;

    private Color(String value, int index) {
      this.value = value;
      this.index = index;
    }
  }

  @Test
  public void testEnumToStringRead() {
    // Should still be able to read constant name
    assertThat(serializer.fromJson("\"A\"", CustomToString.class)).isEqualTo(CustomToString.A);
    // Should be able to read toString() value
    assertThat(serializer.fromJson("\"test\"", CustomToString.class)).isEqualTo(CustomToString.A);

    assertThat(serializer.fromJson("\"other\"", CustomToString.class)).isNull();
  }

  private enum CustomToString {
    A;

    @Override
    public String toString() {
      return "test";
    }
  }

  /** Test that enum constant names have higher precedence than {@code toString()} result. */
  @Test
  public void testEnumToStringReadInterchanged() {
    assertThat(serializer.fromJson("\"A\"", InterchangedToString.class))
        .isEqualTo(InterchangedToString.A);
    assertThat(serializer.fromJson("\"B\"", InterchangedToString.class))
        .isEqualTo(InterchangedToString.B);
  }

  private enum InterchangedToString {
    A("B"),
    B("A");

    private final String toString;

    InterchangedToString(String toString) {
      this.toString = toString;
    }

    @Override
    public String toString() {
      return toString;
    }
  }

  /**
   * Verifies that the enum adapter works for a public JDK enum class and no {@code
   * InaccessibleObjectException} is thrown, despite using reflection internally to account for the
   * constant names possibly being obfuscated.
   */
  @Test
  public void testJdkEnum() {
    assertThat(serializer.toJson(Thread.State.NEW)).isEqualTo("\"NEW\"");
    assertThat(serializer.fromJson("\"NEW\"", Thread.State.class)).isEqualTo(Thread.State.NEW);
  }
}