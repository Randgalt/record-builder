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
import io.soabase.recordbuilder.serialization.token.gsontests.TestTypes.BagOfPrimitives;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static io.soabase.recordbuilder.serialization.standard.StandardSerializers.standardRegistry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for Json serialization and deserialization of collections.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class CollectionTest {
    private final RecordBuilderSerializer serializer;

    public CollectionTest() {
        serializer = new RecordBuilderSerializer(standardRegistry());
    }

    @Test
    public void testTopLevelCollectionOfIntegersSerialization() {
        Collection<Integer> target = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Type targetType = new TypeLiteral<Collection<Integer>>() {
        }.getType();
        String json = serializer.toJson(target, targetType);
        assertThat(json).isEqualTo("[1,2,3,4,5,6,7,8,9]");
    }

    @Test
    public void testTopLevelCollectionOfIntegersDeserialization() {
        String json = "[0,1,2,3,4,5,6,7,8,9]";
        Type collectionType = new TypeLiteral<Collection<Integer>>() {
        }.getType();
        Collection<Integer> target = serializer.fromJson(json, collectionType);
        int[] expected = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        assertThat(toIntArray(target)).isEqualTo(expected);
    }

    @Test
    public void testTopLevelListOfIntegerCollectionsDeserialization() {
        String json = "[[1,2,3],[4,5,6],[7,8,9]]";
        Type collectionType = new TypeLiteral<Collection<Collection<Integer>>>() {
        }.getType();
        List<Collection<Integer>> target = serializer.fromJson(json, collectionType);
        int[][] expected = new int[3][3];
        for (int i = 0; i < 3; ++i) {
            int start = (3 * i) + 1;
            for (int j = 0; j < 3; ++j) {
                expected[i][j] = start + j;
            }
        }

        for (int i = 0; i < 3; i++) {
            assertThat(toIntArray(target.get(i))).isEqualTo(expected[i]);
        }
    }

    @Test
    @SuppressWarnings("JdkObsolete")
    public void testLinkedListSerialization() {
        List<String> list = new LinkedList<>();
        list.add("a1");
        list.add("a2");
        Type linkedListType = new TypeLiteral<LinkedList<String>>() {
        }.getType();
        String json = serializer.toJson(list, linkedListType);
        assertThat(json).contains("a1");
        assertThat(json).contains("a2");
    }

    @Test
    public void testLinkedListDeserialization() {
        String json = "['a1','a2']";
        Type linkedListType = new TypeLiteral<LinkedList<String>>() {
        }.getType();
        List<String> list = serializer.fromJson(json, linkedListType);
        assertThat(list.get(0)).isEqualTo("a1");
        assertThat(list.get(1)).isEqualTo("a2");
    }

    @Test
    @SuppressWarnings("JdkObsolete")
    public void testQueueSerialization() {
        Queue<String> queue = new LinkedList<>();
        queue.add("a1");
        queue.add("a2");
        Type queueType = new TypeLiteral<Queue<String>>() {
        }.getType();
        String json = serializer.toJson(queue, queueType);
        assertThat(json).contains("a1");
        assertThat(json).contains("a2");
    }

    @Test
    public void testQueueDeserialization() {
        String json = "['a1','a2']";
        Type queueType = new TypeLiteral<Queue<String>>() {
        }.getType();
        Queue<String> queue = serializer.fromJson(json, queueType);
        assertThat(queue.element()).isEqualTo("a1");
        queue.remove();
        assertThat(queue.element()).isEqualTo("a2");
    }

    @Test
    public void testPriorityQueue() {
        Type type = new TypeLiteral<PriorityQueue<Integer>>() {
        }.getType();
        PriorityQueue<Integer> queue = serializer.fromJson("[10, 20, 22]", type);
        assertThat(queue.size()).isEqualTo(3);
        String json = serializer.toJson(queue);
        assertThat(queue.remove()).isEqualTo(10);
        assertThat(queue.remove()).isEqualTo(20);
        assertThat(queue.remove()).isEqualTo(22);
        assertThat(json).isEqualTo("[10,20,22]");
    }

    @Test
    public void testVector() {
        Type type = new TypeLiteral<Vector<Integer>>() {
        }.getType();
        Vector<Integer> target = serializer.fromJson("[10, 20, 31]", type);
        assertThat(target.size()).isEqualTo(3);
        assertThat(target.get(0)).isEqualTo(10);
        assertThat(target.get(1)).isEqualTo(20);
        assertThat(target.get(2)).isEqualTo(31);
        String json = serializer.toJson(target);
        assertThat(json).isEqualTo("[10,20,31]");
    }

    @Test
    public void testStack() {
        Type type = new TypeLiteral<Stack<Integer>>() {
        }.getType();
        Stack<Integer> target = serializer.fromJson("[11, 13, 17]", type);
        assertThat(target.size()).isEqualTo(3);
        String json = serializer.toJson(target);
        assertThat(target.pop()).isEqualTo(17);
        assertThat(target.pop()).isEqualTo(13);
        assertThat(target.pop()).isEqualTo(11);
        assertThat(json).isEqualTo("[11,13,17]");
    }

    private static class CollectionWithoutNoArgsConstructor<E> extends AbstractCollection<E> {
        // Remove implicit no-args constructor
        public CollectionWithoutNoArgsConstructor(int unused) {
        }

        @Override
        public boolean add(E e) {
            throw new AssertionError("not used by test");
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * Tests that when a custom Collection class without no-args constructor is deserialized, Gson does not use JDK
     * Unsafe to create an instance, since that likely leads to a broken Collection instance.
     */
    @Test
    public void testCollectionWithoutNoArgsConstructor() {
        /*
         * TODO var collectionType = new TypeLiteral<CollectionWithoutNoArgsConstructor<String>>() { }; JsonIOException
         * e = assertThrows(JsonIOException.class, () -> serializer.deserializeFromJsonString("[]", collectionType));
         * assertThat(e) .hasMessageThat() .isEqualTo( "Unable to create instance of " +
         * CollectionWithoutNoArgsConstructor.class + "; Register an InstanceCreator or a TypeAdapter for this type.");
         *
         * // But serialization should work fine assertThat(serializer.serializeToJsonString(new
         * CollectionWithoutNoArgsConstructor<>(0))).isEqualTo("[]");
         *
         * // Deserialization should work when registering custom creator gson = new GsonBuilder() .registerTypeAdapter(
         * CollectionWithoutNoArgsConstructor.class, (InstanceCreator<CollectionWithoutNoArgsConstructor<?>>) type ->
         * new CollectionWithoutNoArgsConstructor<>(0)) .create(); var collection =
         * serializer.deserializeFromJsonString("[]", collectionType);
         * assertThat(collection).isInstanceOf(CollectionWithoutNoArgsConstructor.class);
         */
    }

    @Test
    public void testNullsInListSerialization() {
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add(null);
        list.add("bar");
        String expected = "[\"foo\",null,\"bar\"]";
        Type typeOfList = new TypeLiteral<List<String>>() {
        }.getType();
        String json = serializer.toJson(list, typeOfList);
        assertThat(json).isEqualTo(expected);
    }

    @Test
    public void testNullsInListDeserialization() {
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add(null);
        expected.add("bar");
        String json = "[\"foo\",null,\"bar\"]";
        Type expectedType = new TypeLiteral<List<String>>() {
        }.getType();
        List<String> target = serializer.fromJson(json, expectedType);
        for (int i = 0; i < expected.size(); ++i) {
            assertThat(target.get(i)).isEqualTo(expected.get(i));
        }
    }

    @Test
    public void testCollectionOfObjectSerialization() {
        List<Object> target = new ArrayList<>();
        target.add("Hello");
        target.add("World");
        assertThat(serializer.toJson(target)).isEqualTo("[\"Hello\",\"World\"]");

        Type type = new TypeLiteral<List<Object>>() {
        }.getType();
        assertThat(serializer.toJson(target, type)).isEqualTo("[\"Hello\",\"World\"]");
    }

    @Test
    public void testCollectionOfObjectWithNullSerialization() {
        List<Object> target = new ArrayList<>();
        target.add("Hello");
        target.add(null);
        target.add("World");
        assertThat(serializer.toJson(target)).isEqualTo("[\"Hello\",null,\"World\"]");

        Type type = new TypeLiteral<List<Object>>() {
        }.getType();
        assertThat(serializer.toJson(target, type)).isEqualTo("[\"Hello\",null,\"World\"]");
    }

    @Test
    public void testCollectionOfStringsSerialization() {
        List<String> target = new ArrayList<>();
        target.add("Hello");
        target.add("World");
        assertThat(serializer.toJson(target)).isEqualTo("[\"Hello\",\"World\"]");
    }

    @Test
    public void testCollectionOfBagOfPrimitivesSerialization() {
        List<BagOfPrimitives> target = new ArrayList<>();
        BagOfPrimitives objA = new BagOfPrimitives(3L, 1, true, "blah");
        BagOfPrimitives objB = new BagOfPrimitives(2L, 6, false, "blahB");
        target.add(objA);
        target.add(objB);

        String result = serializer.toJson(target);
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        for (BagOfPrimitives obj : target) {
            assertThat(result).contains(obj.getExpectedJson());
        }
    }

    @Test
    public void testCollectionOfStringsDeserialization() {
        String json = "[\"Hello\",\"World\"]";
        Type collectionType = new TypeLiteral<Collection<String>>() {
        }.getType();
        Collection<String> target = serializer.fromJson(json, collectionType);

        assertThat(target).containsExactly("Hello", "World");
    }

    @Test
    public void testRawCollectionOfIntegersSerialization() {
        Collection<Integer> target = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(serializer.toJson(target)).isEqualTo("[1,2,3,4,5,6,7,8,9]");
    }

    @Test
    public void testObjectCollectionSerialization() {
        BagOfPrimitives bag1 = new BagOfPrimitives();
        Collection<?> target = Arrays.asList(bag1, bag1, "test");
        String json = serializer.toJson(target);
        assertThat(json).contains(bag1.getExpectedJson());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testRawCollectionDeserializationNotAllowed() {
        String json = "[0,1,2,3,4,5,6,7,8,9]";
        Collection<?> integers = serializer.fromJson(json, Collection.class);
        // JsonReader converts numbers to double by default so we need a floating point comparison
        assertThat((Collection) integers).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        json = "[\"Hello\", \"World\"]";
        Collection<?> strings = serializer.fromJson(json, Collection.class);
        assertThat((Collection) strings).containsExactly("Hello", "World");
    }

    @Test
    public void testRawCollectionOfBagOfPrimitivesNotAllowed() {
        BagOfPrimitives bag = new BagOfPrimitives(10, 20, false, "stringValue");
        String json = '[' + bag.getExpectedJson() + ',' + bag.getExpectedJson() + ']';
        Collection<?> target = serializer.fromJson(json, Collection.class);
        assertThat(target.size()).isEqualTo(2);
        for (Object bag1 : target) {
            // Gson 2.0 converts raw objects into maps
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) bag1;
            assertThat(map.values()).containsExactlyInAnyOrder(10, 20, false, "stringValue");
        }
    }

    @Test
    public void testWildcardPrimitiveCollectionSerilaization() {
        Collection<? extends Integer> target = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Type collectionType = new TypeLiteral<Collection<? extends Integer>>() {
        }.getType();
        String json = serializer.toJson(target, collectionType);
        assertThat(json).isEqualTo("[1,2,3,4,5,6,7,8,9]");

        json = serializer.toJson(target);
        assertThat(json).isEqualTo("[1,2,3,4,5,6,7,8,9]");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testWildcardPrimitiveCollectionDeserilaization() {
        String json = "[1,2,3,4,5,6,7,8,9]";
        Type collectionType = new TypeLiteral<Collection<? extends Integer>>() {
        }.getType();
        Collection target = serializer.fromJson(json, collectionType);
        assertThat(target.size()).isEqualTo(9);
        assertThat(target).contains(1);
        assertThat(target).contains(2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testWildcardCollectionField() {
        Collection<BagOfPrimitives> collection = new ArrayList<>();
        BagOfPrimitives objA = new BagOfPrimitives(3L, 1, true, "blah");
        BagOfPrimitives objB = new BagOfPrimitives(2L, 6, false, "blahB");
        collection.add(objA);
        collection.add(objB);

        ObjectWithWildcardCollection target = new ObjectWithWildcardCollection(collection);
        String json = serializer.toJson(target);
        assertThat(json).contains(objA.getExpectedJson());
        assertThat(json).contains(objB.getExpectedJson());

        target = serializer.fromJson(json, ObjectWithWildcardCollection.class);
        Collection<? extends BagOfPrimitives> deserializedCollection = target.getCollection();
        assertThat(deserializedCollection.size()).isEqualTo(2);
        assertThat((Collection) deserializedCollection).contains(objA);
        assertThat((Collection) deserializedCollection).contains(objB);
    }

    @Test
    public void testFieldIsArrayList() {
        HasArrayListField object = new HasArrayListField();
        object.longs.add(1L);
        object.longs.add(3L);
        String json = serializer.toJson(object, HasArrayListField.class);
        assertThat(json).isEqualTo("{\"longs\":[1,3]}");
        HasArrayListField copy = serializer.fromJson("{\"longs\":[1,3]}", HasArrayListField.class);
        assertThat(copy.longs).isEqualTo(Arrays.asList(1L, 3L));
    }

    @Test
    public void testUserCollectionTypeAdapter() {
        /*
         * TODO Type listOfString = new TypeLiteral<List<String>>() { }.getType(); Object stringListSerializer = new
         * JsonSerializer<List<String>>() {
         *
         * @Override public JsonElement serialize( List<String> src, Type typeOfSrc, JsonSerializationContext context) {
         * return new JsonPrimitive(src.get(0) + ";" + src.get(1)); } }; Gson gson = new
         * GsonBuilder().registerTypeAdapter(listOfString, stringListSerializer).create();
         * assertThat(serializer.serializeToJsonString(Arrays.asList("ab", "cd"), listOfString)).isEqualTo("\"ab;cd\"");
         */
    }

    public static class HasArrayListField {
        ArrayList<Long> longs;

        public HasArrayListField() {
            longs = new ArrayList<>();
        }

        public HasArrayListField(ArrayList<Long> longs) {
            this.longs = longs;
        }

        @Deconstructor
        public void deconstructor(Consumer<ArrayList<Long>> longs) {
            longs.accept(this.longs);
        }
    }

    private static int[] toIntArray(Collection<?> collection) {
        int[] ints = new int[collection.size()];
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext(); ++i) {
            Object obj = iterator.next();
            if (obj instanceof Integer) {
                ints[i] = (Integer) obj;
            } else if (obj instanceof Long) {
                ints[i] = ((Long) obj).intValue();
            }
        }
        return ints;
    }

    // TODO - this is private in the original test - add traits, etc. to make things accessible
    public static class ObjectWithWildcardCollection {
        private final Collection<? extends BagOfPrimitives> collection;

        public ObjectWithWildcardCollection(Collection<? extends BagOfPrimitives> collection) {
            this.collection = collection;
        }

        public Collection<? extends BagOfPrimitives> getCollection() {
            return collection;
        }

        @Deconstructor
        public void deconstructor(Consumer<Collection<? extends BagOfPrimitives>> collection) {
            collection.accept(this.collection);
        }
    }

    // TODO - this is private in the original test - add traits, etc. to make things accessible
    public static class Entry {
        int value;

        public Entry(int value) {
            this.value = value;
        }

        @Deconstructor
        public void deconstructor(IntConsumer value) {
            value.accept(this.value);
        }
    }

    @Test
    public void testSetSerialization() {
        Set<Entry> set = new HashSet<>();
        set.add(new Entry(1));
        set.add(new Entry(2));
        String json = serializer.toJson(set);
        assertThat(json).contains("1");
        assertThat(json).contains("2");
    }

    @Test
    public void testSetDeserialization() {
        String json = "[{'value':1},{'value':2}]";
        Type type = new TypeLiteral<Set<Entry>>() {
        }.getType();
        Set<Entry> set = serializer.fromJson(json, type);
        assertThat(set.size()).isEqualTo(2);
        assertThat(set).extracting("value").containsExactlyInAnyOrder(1, 2);
    }

    // TODO - this is private in the original test - add traits, etc. to make things accessible
    public static class BigClass {
        private Map<String, ? extends List<SmallClass>> inBig;

        public BigClass() {
        }

        public BigClass(Map<String, ? extends List<SmallClass>> inBig) {
            this.inBig = inBig;
        }

        @Deconstructor
        public void deconstructor(Consumer<Map<String, ? extends List<SmallClass>>> inBig) {
            inBig.accept(this.inBig);
        }
    }

    // TODO - this is private in the original test - add traits, etc. to make things accessible
    public static class SmallClass {
        private String inSmall;

        public SmallClass() {
        }

        public SmallClass(String inSmall) {
            this.inSmall = inSmall;
        }

        @Deconstructor
        public void deconstructor(Consumer<String> inSmall) {
            inSmall.accept(this.inSmall);
        }
    }

    @Test
    public void testIssue1107() {
        String json = "{\n" + "  \"inBig\": {\n" + "    \"key\": [\n" + "      { \"inSmall\": \"hello\" }\n" + "    ]\n"
                + "  }\n" + "}";
        BigClass bigClass = serializer.fromJson(json, BigClass.class);
        SmallClass small = bigClass.inBig.get("key").get(0);
        assertThat(small).isNotNull();
        assertThat(small.inSmall).isEqualTo("hello");
    }
}