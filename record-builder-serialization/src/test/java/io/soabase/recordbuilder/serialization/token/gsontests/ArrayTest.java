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
package io.soabase.recordbuilder.serialization.token.gsontests;

import io.soabase.com.google.inject.TypeLiteral;
import io.soabase.recordbuilder.serialization.RecordBuilderSerializer;
import io.soabase.recordbuilder.serialization.token.gsontests.TestTypes.BagOfPrimitives;
import io.soabase.recordbuilder.serialization.token.gsontests.TestTypes.ClassWithObjects;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static io.soabase.recordbuilder.serialization.standard.StandardSerializers.standardRegistry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Functional tests for Json serialization and deserialization of arrays.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ArrayTest {
    private final RecordBuilderSerializer serializer;

    public ArrayTest() {
        serializer = new RecordBuilderSerializer(standardRegistry());
    }

    @Test
    public void testTopLevelArrayOfIntsSerialization() {
        int[] target = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        assertThat(serializer.toJson(target)).isEqualTo("[1,2,3,4,5,6,7,8,9]");
    }

    @Test
    public void testTopLevelArrayOfIntsDeserialization() {
        int[] expected = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        int[] actual = serializer.fromJson("[1,2,3,4,5,6,7,8,9]", int[].class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testInvalidArrayDeserialization() {
        String json = "[1, 2 3, 4, 5]";
        // Gson should not deserialize array elements with missing ','
        assertThatThrownBy(() -> serializer.fromJson(json, int[].class)).isInstanceOf(RuntimeException.class); // TODO
    }

    @Test
    public void testEmptyArraySerialization() {
        int[] target = {};
        assertThat(serializer.toJson(target)).isEqualTo("[]");
    }

    @Test
    public void testEmptyArrayDeserialization() {
        int[] actualObject = serializer.fromJson("[]", int[].class);
        assertThat(actualObject).hasSize(0);

        Integer[] actualObject2 = serializer.fromJson("[]", Integer[].class);
        assertThat(actualObject2).hasSize(0);

        actualObject = serializer.fromJson("[ ]", int[].class);
        assertThat(actualObject).hasSize(0);
    }

    @Test
    public void testNullsInArraySerialization() {
        String[] array = { "foo", null, "bar" };
        String expected = "[\"foo\",null,\"bar\"]";
        String json = serializer.toJson(array);
        assertThat(json).isEqualTo(expected);
    }

    @Test
    public void testNullsInArrayDeserialization() {
        String json = "[\"foo\",null,\"bar\"]";
        String[] expected = { "foo", null, "bar" };
        String[] target = serializer.fromJson(json, expected.getClass());
        assertThat(target).isEqualTo(expected);
    }

    @Test
    public void testSingleNullInArraySerialization() {
        BagOfPrimitives[] array = new BagOfPrimitives[1];
        array[0] = null;
        String json = serializer.toJson(array);
        assertThat(json).isEqualTo("[null]");
    }

    @Test
    public void testSingleNullInArrayDeserialization() {
        BagOfPrimitives[] array = serializer.fromJson("[null]", BagOfPrimitives[].class);
        assertThat(array).isEqualTo(new BagOfPrimitives[] { null });
    }

    @Test
    public void testNullsInArrayWithSerializeNullPropertySetSerialization() {
        String[] array = { "foo", null, "bar" };
        String expected = "[\"foo\",null,\"bar\"]";
        String json = serializer.toJson(array);
        assertThat(json).isEqualTo(expected);
    }

    @Test
    public void testArrayOfStringsSerialization() {
        String[] target = { "Hello", "World" };
        assertThat(serializer.toJson(target)).isEqualTo("[\"Hello\",\"World\"]");
    }

    @Test
    public void testArrayOfStringsDeserialization() {
        String json = "[\"Hello\",\"World\"]";
        String[] target = serializer.fromJson(json, String[].class);
        assertThat(Arrays.asList(target)).containsExactly("Hello", "World");
    }

    @Test
    public void testSingleStringArraySerialization() {
        String[] s = { "hello" };
        String output = serializer.toJson(s);
        assertThat(output).isEqualTo("[\"hello\"]");
    }

    @Test
    public void testSingleStringArrayDeserialization() {
        String json = "[\"hello\"]";
        String[] arrayType = serializer.fromJson(json, String[].class);
        assertThat(Arrays.asList(arrayType)).containsExactly("hello");
    }

    @Test
    public void testArrayOfCollectionSerialization() {
        StringBuilder sb = new StringBuilder("[");
        int arraySize = 3;

        Type typeToSerialize = new TypeLiteral<Collection<Integer>[]>() {
        }.getType();
        @SuppressWarnings("unchecked")
        Collection<Integer>[] arrayOfCollection = new ArrayList[arraySize];
        for (int i = 0; i < arraySize; ++i) {
            int startValue = (3 * i) + 1;
            sb.append('[').append(startValue).append(',').append(startValue + 1).append(']');
            ArrayList<Integer> tmpList = new ArrayList<>();
            tmpList.add(startValue);
            tmpList.add(startValue + 1);
            arrayOfCollection[i] = tmpList;

            if (i < arraySize - 1) {
                sb.append(',');
            }
        }
        sb.append(']');

        String json = serializer.toJson(arrayOfCollection, typeToSerialize);
        assertThat(json).isEqualTo(sb.toString());
    }

    @Test
    public void testArrayOfCollectionDeserialization() {
        String json = "[[1,2],[3,4]]";
        Type type = new TypeLiteral<Collection<Integer>[]>() {
        }.getType();
        Collection<Integer>[] target = serializer.fromJson(json, type);

        assertThat(target.length).isEqualTo(2);
        assertThat(target[0].toArray(new Integer[0])).isEqualTo(new Integer[] { 1, 2 });
        assertThat(target[1].toArray(new Integer[0])).isEqualTo(new Integer[] { 3, 4 });
    }

    @Test
    public void testArrayOfPrimitivesAsObjectsSerialization() {
        Object[] objs = new Object[] { 1, "abc", 0.3f, 5L };
        String json = serializer.toJson(objs);
        assertThat(json).contains("abc");
        assertThat(json).contains("0.3");
        assertThat(json).contains("5");
    }

    @Test
    public void testArrayOfPrimitivesAsObjectsDeserialization() {
        String json = "[1,\"abc\",0.3,1.1,5]";
        Object[] objs = serializer.fromJson(json, Object[].class);
        assertThat(((Number) objs[0]).intValue()).isEqualTo(1);
        assertThat(objs[1]).isEqualTo("abc");
        assertThat(((Number) objs[2]).doubleValue()).isEqualTo(0.3);
        assertThat(new BigDecimal(objs[3].toString())).isEqualTo(new BigDecimal("1.1"));
        assertThat(((Number) objs[4])).isEqualTo(5);
    }

    @Test
    public void testObjectArrayWithNonPrimitivesSerialization() {
        ClassWithObjects classWithObjects = new ClassWithObjects();
        BagOfPrimitives bagOfPrimitives = new BagOfPrimitives();
        String classWithObjectsJson = serializer.toJson(classWithObjects);
        String bagOfPrimitivesJson = serializer.toJson(bagOfPrimitives);

        Object[] objects = { classWithObjects, bagOfPrimitives };
        String json = serializer.toJson(objects);

        assertThat(json).contains(classWithObjectsJson);
        assertThat(json).contains(bagOfPrimitivesJson);
    }

    @Test
    public void testArrayOfNullSerialization() {
        Object[] array = { null };
        String json = serializer.toJson(array);
        assertThat(json).isEqualTo("[null]");
    }

    @Test
    public void testArrayOfNullDeserialization() {
        String[] values = serializer.fromJson("[null]", String[].class);
        assertThat(values[0]).isNull();
    }

    /** Regression tests for Issue 272 */

    @Test
    public void testMultidimensionalArraysSerialization() {
        String[][] items = { { "3m Co", "71.72", "0.02", "0.03", "4/2 12:00am", "Manufacturing" },
                { "Alcoa Inc", "29.01", "0.42", "1.47", "4/1 12:00am", "Manufacturing" } };
        String json = serializer.toJson(items);
        assertThat(json).contains("[[\"3m Co");
        assertThat(json).contains("Manufacturing\"]]");
    }

    @Test
    public void testMultidimensionalObjectArraysSerialization() {
        Object[][] array = { new Object[] { 1, 2 } };
        assertThat(serializer.toJson(array)).isEqualTo("[[1,2]]");
    }

    @Test
    public void testMultidimensionalPrimitiveArraysSerialization() {
        int[][] array = { { 1, 2 }, { 3, 4 } };
        assertThat(serializer.toJson(array)).isEqualTo("[[1,2],[3,4]]");
    }

    /** Regression test for Issue 205 */

    @Test
    public void testMixingTypesInObjectArraySerialization() {
        Object[] array = { 1, 2, new Object[] { "one", "two", 3 } };
        assertThat(serializer.toJson(array)).isEqualTo("[1,2,[\"one\",\"two\",3]]");
    }

    /** Regression tests for Issue 272 */

    @Test
    public void testMultidimensionalArraysDeserialization() {
        String json = "[['3m Co','71.72','0.02','0.03','4/2 12:00am','Manufacturing'],"
                + "['Alcoa Inc','29.01','0.42','1.47','4/1 12:00am','Manufacturing']]";
        String[][] items = serializer.fromJson(json, String[][].class);
        assertThat(items[0][0]).isEqualTo("3m Co");
        assertThat(items[1][5]).isEqualTo("Manufacturing");
    }

    @Test
    public void testMultidimensionalPrimitiveArraysDeserialization() {
        String json = "[[1,2],[3,4]]";
        int[][] expected = { { 1, 2 }, { 3, 4 } };
        assertThat(serializer.fromJson(json, int[][].class)).isEqualTo(expected);
    }

    /** http://code.google.com/p/google-gson/issues/detail?id=342 */

    @Test
    public void testArrayElementsAreArrays() {
        Object[] stringArrays = { new String[] { "test1", "test2" }, new String[] { "test3", "test4" } };
        assertThat(serializer.toJson(stringArrays)).isEqualTo("[[\"test1\",\"test2\"],[\"test3\",\"test4\"]]");
    }
}
