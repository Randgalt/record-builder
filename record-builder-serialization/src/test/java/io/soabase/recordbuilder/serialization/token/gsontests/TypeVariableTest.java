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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static io.soabase.recordbuilder.serialization.standard.StandardSerializers.standardRegistry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional test for Gson serialization and deserialization of classes with type variables.
 *
 * @author Joel Leitch
 */
public class TypeVariableTest {
    private final RecordBuilderSerializer serializer;

    public TypeVariableTest() {
        serializer = new RecordBuilderSerializer(standardRegistry());
    }

    @Test
    public void testAdvancedTypeVariables() {
        Bar bar1 = new Bar("someString", 1, true);
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(1);
        arrayList.add(2);
        arrayList.add(3);
        bar1.map.put("key1", arrayList);
        bar1.map.put("key2", new ArrayList<>());
        String json = serializer.toJson(bar1);

        Bar bar2 = serializer.fromJson(json, Bar.class);
        assertThat(bar2).isEqualTo(bar1);
    }

    /*
     * @Test public void testTypeVariablesViaTypeParameter() { Foo<String, Integer> original = new Foo<>("e", 5, false);
     * original.map.put("f", Arrays.asList(6, 7)); Type type = new TypeLiteral<Foo<String, Integer>>() {}.getType();
     * String json = serializer.toJson(original, type); assertThat(json) .isEqualTo(
     * "{\"someSField\":\"e\",\"someTField\":5,\"map\":{\"f\":[6,7]},\"redField\":false}");
     * assertThat(serializer.<Foo<String, Integer>>fromJson(json, type)).isEqualTo(original); }
     */

    @Test
    public void testBasicTypeVariables() {
        Blue blue1 = new Blue(true);
        String json = serializer.toJson(blue1);

        Blue blue2 = serializer.fromJson(json, Blue.class);
        assertThat(blue2).isEqualTo(blue1);
    }

    // for missing hashCode() override
    @SuppressWarnings({ "overrides", "EqualsHashCode" })
    public static class Blue extends Red<Boolean> {
        public Blue() {
            super(false);
        }

        public Blue(boolean value) {
            super(value);
        }

        @Deconstructor
        public void deconstructor(Consumer<Boolean> value) {
            value.accept(redField);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Blue)) {
                return false;
            }
            Blue blue = (Blue) o;
            return redField.equals(blue.redField);
        }
    }

    public static class Red<S> {
        protected S redField;

        public Red() {
        }

        public Red(S redField) {
            this.redField = redField;
        }
    }

    @SuppressWarnings({ "overrides", "EqualsHashCode" }) // for missing hashCode() override
    public static class Foo<S, T> extends Red<Boolean> {
        protected S someSField;
        protected T someTField;
        public final Map<S, List<T>> map = new HashMap<>();

        public Foo() {
        }

        public Foo(S sValue, T tValue, Boolean redField) {
            super(redField);
            this.someSField = sValue;
            this.someTField = tValue;
        }

        public Foo(S sValue, T tValue, Boolean redField, Map<S, List<T>> map) {
            super(redField);
            this.someSField = sValue;
            this.someTField = tValue;
            this.map.putAll(map);
        }

        @Deconstructor
        public void deconstructor(Consumer<S> someSField, IntConsumer someTField, Consumer<Boolean> redField,
                Consumer<Map<S, List<T>>> map) {
            someSField.accept(this.someSField);
            someTField.accept((Integer) this.someTField);
            redField.accept(this.redField);
            map.accept(this.map);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (!(o instanceof Foo<?, ?>)) {
                return false;
            }
            Foo<S, T> realFoo = (Foo<S, T>) o;
            return redField.equals(realFoo.redField) && someTField.equals(realFoo.someTField)
                    && someSField.equals(realFoo.someSField) && map.equals(realFoo.map);
        }
    }

    public static class Bar extends Foo<String, Integer> {
        public Bar() {
            this("", 0, false);
        }

        public Bar(String s, Integer i, boolean b) {
            super(s, i, b);
        }

        public Bar(String s, Integer i, boolean b, Map<String, List<Integer>> map) {
            super(s, i, b, map);
        }

        @Deconstructor
        public void deconstructor(Consumer<String> s, IntConsumer i, Consumer<Boolean> b,
                Consumer<Map<String, List<Integer>>> map) {
            s.accept(this.someSField);
            i.accept(this.someTField);
            b.accept(redField);
            map.accept(this.map);
        }
    }
}