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
import io.soabase.recordbuilder.core.RecordBuilder.Deconstructor;
import io.soabase.recordbuilder.serialization.RecordBuilderSerializer;
import io.soabase.recordbuilder.serialization.token.gsontests.ParameterizedTypeFixtures.MyParameterizedType;
import io.soabase.recordbuilder.serialization.token.gsontests.TestTypes.BagOfPrimitives;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static io.soabase.recordbuilder.serialization.standard.StandardSerializers.standardRegistry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for the serialization and deserialization of parameterized types in Gson.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ParameterizedTypesTest {
    private final RecordBuilderSerializer serializer;

    public ParameterizedTypesTest() {
        serializer = new RecordBuilderSerializer(standardRegistry());
    }

    @Test
    public void testParameterizedTypesSerialization() {
        MyParameterizedType<Integer> src = new MyParameterizedType<>(10);
        Type typeOfSrc = new TypeLiteral<MyParameterizedType<Integer>>() {
        }.getType();
        String json = serializer.toJson(src, typeOfSrc);
        assertThat(json).isEqualTo(src.getExpectedJson());
    }

    @Test
    public void testParameterizedTypeDeserialization() {
        BagOfPrimitives bag = new BagOfPrimitives();
        MyParameterizedType<BagOfPrimitives> expected = new MyParameterizedType<>(bag);
        Type expectedType = new TypeLiteral<MyParameterizedType<BagOfPrimitives>>() {
        }.getType();
        BagOfPrimitives bagDefaultInstance = new BagOfPrimitives();
        /*
         * Gson gson = new GsonBuilder() .registerTypeAdapter( expectedType, new
         * MyParameterizedTypeInstanceCreator<>(bagDefaultInstance)) .create();
         */

        String json = expected.getExpectedJson();
        MyParameterizedType<BagOfPrimitives> actual = serializer.fromJson(json, expectedType);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTypesWithMultipleParametersSerialization() {
        MultiParameters<Integer, Float, Double, String, BagOfPrimitives> src = new MultiParameters<>(10, 1.0F, 2.1D,
                "abc", new BagOfPrimitives());
        Type typeOfSrc = new TypeLiteral<MultiParameters<Integer, Float, Double, String, BagOfPrimitives>>() {
        }.getType();
        String json = serializer.toJson(src, typeOfSrc);
        String expected = "{\"a\":10,\"b\":1.0,\"c\":2.1,\"d\":\"abc\","
                + "\"e\":{\"longValue\":0,\"intValue\":0,\"booleanValue\":false,\"stringValue\":\"\"}}";
        assertThat(json).isEqualTo(expected);
    }

    @Test
    public void testTypesWithMultipleParametersDeserialization() {
        Type typeOfTarget = new TypeLiteral<MultiParameters<Integer, Float, Double, String, BagOfPrimitives>>() {
        }.getType();
        String json = "{\"a\":10,\"b\":1.0,\"c\":2.1,\"d\":\"abc\","
                + "\"e\":{\"longValue\":0,\"intValue\":0,\"booleanValue\":false,\"stringValue\":\"\"}}";
        MultiParameters<Integer, Float, Double, String, BagOfPrimitives> target = serializer.fromJson(json,
                typeOfTarget);
        MultiParameters<Integer, Float, Double, String, BagOfPrimitives> expected = new MultiParameters<>(10, 1.0F,
                2.1D, "abc", new BagOfPrimitives());
        assertThat(target).isEqualTo(expected);
    }

    /*
     * @Test public void testParameterizedTypeWithCustomSerializer() { Type ptIntegerType = new
     * TypeLiteral<MyParameterizedType<Integer>>() {}.getType(); Type ptStringType = new
     * TypeLiteral<MyParameterizedType<String>>() {}.getType(); Gson gson = new GsonBuilder()
     * .registerTypeAdapter(ptIntegerType, new MyParameterizedTypeAdapter<Integer>()) .registerTypeAdapter(ptStringType,
     * new MyParameterizedTypeAdapter<String>()) .create(); MyParameterizedType<Integer> intTarget = new
     * MyParameterizedType<>(10); String json = serializer.toJson(intTarget, ptIntegerType);
     * assertThat(json).isEqualTo(MyParameterizedTypeAdapter.getExpectedJson(intTarget));
     *
     * MyParameterizedType<String> stringTarget = new MyParameterizedType<>("abc"); json =
     * serializer.toJson(stringTarget, ptStringType);
     * assertThat(json).isEqualTo(MyParameterizedTypeAdapter.getExpectedJson(stringTarget)); }
     *
     * @Test public void testParameterizedTypesWithCustomDeserializer() { Type ptIntegerType = new
     * TypeLiteral<MyParameterizedType<Integer>>() {}.getType(); Type ptStringType = new
     * TypeLiteral<MyParameterizedType<String>>() {}.getType(); Gson gson = new GsonBuilder()
     * .registerTypeAdapter(ptIntegerType, new MyParameterizedTypeAdapter<Integer>()) .registerTypeAdapter(ptStringType,
     * new MyParameterizedTypeAdapter<String>()) .registerTypeAdapter(ptStringType, new
     * MyParameterizedTypeInstanceCreator<>("")) .registerTypeAdapter(ptIntegerType, new
     * MyParameterizedTypeInstanceCreator<>(0)) .create();
     *
     * MyParameterizedType<Integer> src = new MyParameterizedType<>(10); String json =
     * MyParameterizedTypeAdapter.getExpectedJson(src); MyParameterizedType<Integer> intTarget =
     * serializer.fromJson(json, ptIntegerType); assertThat(intTarget.value).isEqualTo(10);
     *
     * MyParameterizedType<String> srcStr = new MyParameterizedType<>("abc"); json =
     * MyParameterizedTypeAdapter.getExpectedJson(srcStr); MyParameterizedType<String> stringTarget =
     * serializer.fromJson(json, ptStringType); assertThat(stringTarget.value).isEqualTo("abc"); }
     */

    /*
     * @Test public void testParameterizedTypesWithWriterSerialization() { Writer writer = new StringWriter();
     * MyParameterizedType<Integer> src = new MyParameterizedType<>(10); Type typeOfSrc = new
     * TypeLiteral<MyParameterizedType<Integer>>() {}.getType(); serializer.toJson(src, typeOfSrc, writer);
     * assertThat(writer.toString()).isEqualTo(src.getExpectedJson()); }
     */

    @Test
    public void testParameterizedTypeWithReaderDeserialization() {
        BagOfPrimitives bag = new BagOfPrimitives();
        MyParameterizedType<BagOfPrimitives> expected = new MyParameterizedType<>(bag);
        Type expectedType = new TypeLiteral<MyParameterizedType<BagOfPrimitives>>() {
        }.getType();
        BagOfPrimitives bagDefaultInstance = new BagOfPrimitives();
        /*
         * Gson gson = new GsonBuilder() .registerTypeAdapter( expectedType, new
         * MyParameterizedTypeInstanceCreator<>(bagDefaultInstance)) .create();
         */

        Reader json = new StringReader(expected.getExpectedJson());
        MyParameterizedType<BagOfPrimitives> actual = serializer.fromJson(json, expectedType);
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    private static <T> T[] arrayOf(T... args) {
        return args;
    }

    @Test
    public void testVariableTypeFieldsAndGenericArraysSerialization() {
        Integer obj = 0;
        Integer[] array = { 1, 2, 3 };
        List<Integer> list = new ArrayList<>();
        list.add(4);
        list.add(5);
        List<Integer>[] arrayOfLists = arrayOf(list, list);

        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(obj, array, list, arrayOfLists,
                list, arrayOfLists);
        String json = serializer.toJson(objToSerialize, typeOfSrc);

        assertThat(json).isEqualTo(objToSerialize.getExpectedJson());
    }

    @Test
    public void testVariableTypeFieldsAndGenericArraysDeserialization() {
        Integer obj = 0;
        Integer[] array = { 1, 2, 3 };
        List<Integer> list = new ArrayList<>();
        list.add(4);
        list.add(5);
        List<Integer>[] arrayOfLists = arrayOf(list, list);

        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(obj, array, list, arrayOfLists,
                list, arrayOfLists);
        String json = serializer.toJson(objToSerialize, typeOfSrc);
        ObjectWithTypeVariables<Integer> objAfterDeserialization = serializer.fromJson(json, typeOfSrc);

        assertThat(json).isEqualTo(objAfterDeserialization.getExpectedJson());
    }

    @Test
    public void testVariableTypeDeserialization() {
        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(0, null, null, null, null,
                null);
        String json = serializer.toJson(objToSerialize, typeOfSrc);
        ObjectWithTypeVariables<Integer> objAfterDeserialization = serializer.fromJson(json, typeOfSrc);

        assertThat(json).isEqualTo(objAfterDeserialization.getExpectedJson());
    }

    @Test
    public void testVariableTypeArrayDeserialization() {
        Integer[] array = { 1, 2, 3 };

        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(null, array, null, null, null,
                null);
        String json = serializer.toJson(objToSerialize, typeOfSrc);
        ObjectWithTypeVariables<Integer> objAfterDeserialization = serializer.fromJson(json, typeOfSrc);

        assertThat(json).isEqualTo(objAfterDeserialization.getExpectedJson());
    }

    @Test
    public void testParameterizedTypeWithVariableTypeDeserialization() {
        List<Integer> list = new ArrayList<>();
        list.add(4);
        list.add(5);

        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(null, null, list, null, null,
                null);
        String json = serializer.toJson(objToSerialize, typeOfSrc);
        ObjectWithTypeVariables<Integer> objAfterDeserialization = serializer.fromJson(json, typeOfSrc);

        assertThat(json).isEqualTo(objAfterDeserialization.getExpectedJson());
    }

    @Test
    public void testParameterizedTypeGenericArraysSerialization() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        List<Integer>[] arrayOfLists = arrayOf(list, list);

        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(null, null, null, arrayOfLists,
                null, null);
        String json = serializer.toJson(objToSerialize, typeOfSrc);
        assertThat(json).isEqualTo("{\"arrayOfListOfTypeParameters\":[[1,2],[1,2]]}");
    }

    @Test
    public void testParameterizedTypeGenericArraysDeserialization() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        List<Integer>[] arrayOfLists = arrayOf(list, list);

        Type typeOfSrc = new TypeLiteral<ObjectWithTypeVariables<Integer>>() {
        }.getType();
        ObjectWithTypeVariables<Integer> objToSerialize = new ObjectWithTypeVariables<>(null, null, null, arrayOfLists,
                null, null);
        String json = serializer.toJson(objToSerialize, typeOfSrc);
        ObjectWithTypeVariables<Integer> objAfterDeserialization = serializer.fromJson(json, typeOfSrc);

        assertThat(json).isEqualTo(objAfterDeserialization.getExpectedJson());
    }

    /**
     * An test object that has fields that are type variables.
     *
     * @param <T>
     *            Enforce T to be a string to make writing the "toExpectedJson" method easier.
     */
    public static class ObjectWithTypeVariables<T extends Number> {
        private final T typeParameterObj;
        private final T[] typeParameterArray;
        private final List<T> listOfTypeParameters;
        private final List<T>[] arrayOfListOfTypeParameters;
        private final List<? extends T> listOfWildcardTypeParameters;
        private final List<? extends T>[] arrayOfListOfWildcardTypeParameters;

        // For use by Gson
        @SuppressWarnings("unused")
        private ObjectWithTypeVariables() {
            this(null, null, null, null, null, null);
        }

        @Deconstructor
        public void deconstructor(Consumer<T> typeParameterObj, Consumer<T[]> typeParameterArray,
                Consumer<List<T>> listOfTypeParameters, Consumer<List<T>[]> arrayOfListOfTypeParameters,
                Consumer<List<? extends T>> listOfWildcardTypeParameters,
                Consumer<List<? extends T>[]> arrayOfListOfWildcardTypeParameters) {
            typeParameterObj.accept(this.typeParameterObj);
            typeParameterArray.accept(this.typeParameterArray);
            listOfTypeParameters.accept(this.listOfTypeParameters);
            arrayOfListOfTypeParameters.accept(this.arrayOfListOfTypeParameters);
            listOfWildcardTypeParameters.accept(this.listOfWildcardTypeParameters);
            arrayOfListOfWildcardTypeParameters.accept(this.arrayOfListOfWildcardTypeParameters);
        }

        public ObjectWithTypeVariables(T obj, T[] array, List<T> list, List<T>[] arrayOfList,
                List<? extends T> wildcardList, List<? extends T>[] arrayOfWildcardList) {
            this.typeParameterObj = obj;
            this.typeParameterArray = array;
            this.listOfTypeParameters = list;
            this.arrayOfListOfTypeParameters = arrayOfList;
            this.listOfWildcardTypeParameters = wildcardList;
            this.arrayOfListOfWildcardTypeParameters = arrayOfWildcardList;
        }

        public String getExpectedJson() {
            StringBuilder sb = new StringBuilder().append("{");

            boolean needsComma = false;
            if (typeParameterObj != null) {
                sb.append("\"typeParameterObj\":").append(toString(typeParameterObj));
                needsComma = true;
            }

            if (typeParameterArray != null) {
                if (needsComma) {
                    sb.append(',');
                }
                sb.append("\"typeParameterArray\":[");
                appendObjectsToBuilder(sb, Arrays.asList(typeParameterArray));
                sb.append(']');
                needsComma = true;
            }

            if (listOfTypeParameters != null) {
                if (needsComma) {
                    sb.append(',');
                }
                sb.append("\"listOfTypeParameters\":[");
                appendObjectsToBuilder(sb, listOfTypeParameters);
                sb.append(']');
                needsComma = true;
            }

            if (arrayOfListOfTypeParameters != null) {
                if (needsComma) {
                    sb.append(',');
                }
                sb.append("\"arrayOfListOfTypeParameters\":[");
                appendObjectsToBuilder(sb, arrayOfListOfTypeParameters);
                sb.append(']');
                needsComma = true;
            }

            if (listOfWildcardTypeParameters != null) {
                if (needsComma) {
                    sb.append(',');
                }
                sb.append("\"listOfWildcardTypeParameters\":[");
                appendObjectsToBuilder(sb, listOfWildcardTypeParameters);
                sb.append(']');
                needsComma = true;
            }

            if (arrayOfListOfWildcardTypeParameters != null) {
                if (needsComma) {
                    sb.append(',');
                }
                sb.append("\"arrayOfListOfWildcardTypeParameters\":[");
                appendObjectsToBuilder(sb, arrayOfListOfWildcardTypeParameters);
                sb.append(']');
                needsComma = true;
            }
            sb.append('}');
            return sb.toString();
        }

        private void appendObjectsToBuilder(StringBuilder sb, Iterable<? extends T> iterable) {
            boolean isFirst = true;
            for (T obj : iterable) {
                if (!isFirst) {
                    sb.append(',');
                }
                isFirst = false;
                sb.append(toString(obj));
            }
        }

        private void appendObjectsToBuilder(StringBuilder sb, List<? extends T>[] arrayOfList) {
            boolean isFirst = true;
            for (List<? extends T> list : arrayOfList) {
                if (!isFirst) {
                    sb.append(',');
                }
                isFirst = false;
                if (list != null) {
                    sb.append('[');
                    appendObjectsToBuilder(sb, list);
                    sb.append(']');
                } else {
                    sb.append("null");
                }
            }
        }

        public String toString(T obj) {
            return obj.toString();
        }
    }

    public static final class MultiParameters<A, B, C, D, E> {
        A a;
        B b;
        C c;
        D d;
        E e;

        // For use by Gson
        @SuppressWarnings("unused")
        private MultiParameters() {
        }

        public MultiParameters(A a, B b, C c, D d, E e) {
            super();
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
        }

        @Deconstructor
        public void deconstructor(Consumer<A> a, Consumer<B> b, Consumer<C> c, Consumer<D> d, Consumer<E> e) {
            a.accept(this.a);
            b.accept(this.b);
            c.accept(this.c);
            d.accept(this.d);
            e.accept(this.e);
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            result = prime * result + ((b == null) ? 0 : b.hashCode());
            result = prime * result + ((c == null) ? 0 : c.hashCode());
            result = prime * result + ((d == null) ? 0 : d.hashCode());
            result = prime * result + ((e == null) ? 0 : e.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MultiParameters<?, ?, ?, ?, ?>)) {
                return false;
            }
            MultiParameters<?, ?, ?, ?, ?> that = (MultiParameters<?, ?, ?, ?, ?>) o;
            return Objects.equals(a, that.a) && Objects.equals(b, that.b) && Objects.equals(c, that.c)
                    && Objects.equals(d, that.d) && Objects.equals(e, that.e);
        }
    }

    // Begin: tests to reproduce issue 103
    public static class Quantity {
        @SuppressWarnings("unused")
        int q = 10;

        public Quantity() {
        }

        public Quantity(int q) {
            this.q = q;
        }

        @Deconstructor
        public void deconstructor(Consumer<Integer> q) {
            q.accept(this.q);
        }
    }

    public static class MyQuantity extends Quantity {
        @SuppressWarnings("unused")
        int q2 = 20;

        public MyQuantity() {
        }

        public MyQuantity(int q2) {
            this.q2 = q2;
        }

        @Deconstructor
        public void deconstructor(Consumer<Integer> q2) {
            q2.accept(this.q2);
        }
    }

    private interface Measurable<T> {
    }

    private interface Field<T> {
    }

    private interface Immutable {
    }

    public static final class Amount<Q extends Quantity>
            implements Measurable<Q>, Field<Amount<?>>, Serializable, Immutable {
        private static final long serialVersionUID = -7560491093120970437L;

        int value = 30;

        public Amount() {
        }

        public Amount(int value) {
            this.value = value;
        }

        @Deconstructor
        public void deconstructor(IntConsumer value) {
            value.accept(this.value);
        }
    }

    @Test
    public void testDeepParameterizedTypeSerialization() {
        Amount<MyQuantity> amount = new Amount<>();
        String json = serializer.toJson(amount);
        assertThat(json).contains("value");
        assertThat(json).contains("30");
    }

    @Test
    public void testDeepParameterizedTypeDeserialization() {
        String json = "{'value':30}";
        Type type = new TypeLiteral<Amount<MyQuantity>>() {
        }.getType();
        Amount<MyQuantity> amount = serializer.fromJson(json, type);
        assertThat(amount.value).isEqualTo(30);
    }

    // End: tests to reproduce issue 103

    private static void assertCorrectlyDeserialized(Object object) {
        @SuppressWarnings("unchecked")
        List<Quantity> list = (List<Quantity>) object;
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).q).isEqualTo(4);
    }

    /*
     * @Test public void testGsonFromJsonTypeToken() { TypeLiteral<List<Quantity>> typeToken = new TypeLiteral<>() {};
     * Type type = typeToken.getType();
     *
     * { JsonObject jsonObject = new JsonObject(); jsonObject.addProperty("q", 4); JsonArray jsonArray = new
     * JsonArray(); jsonArray.add(jsonObject);
     *
     * assertCorrectlyDeserialized(serializer.fromJson(jsonArray, typeToken));
     * assertCorrectlyDeserialized(serializer.fromJson(jsonArray, type)); }
     *
     * String json = "[{\"q\":4}]";
     *
     * { assertCorrectlyDeserialized(serializer.fromJson(json, typeToken));
     * assertCorrectlyDeserialized(serializer.fromJson(json, type)); }
     *
     * { assertCorrectlyDeserialized(serializer.fromJson(new StringReader(json), typeToken));
     * assertCorrectlyDeserialized(serializer.fromJson(new StringReader(json), type)); }
     *
     * { JsonReader reader = new JsonReader(new StringReader(json));
     * assertCorrectlyDeserialized(serializer.fromJson(reader, typeToken));
     *
     * reader = new JsonReader(new StringReader(json)); assertCorrectlyDeserialized(serializer.fromJson(reader, type));
     * } }
     */
}