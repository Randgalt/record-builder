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
package io.soabase.recordbuilder.serialization.standard;

import io.soabase.com.google.inject.util.MoreTypes;
import io.soabase.recordbuilder.serialization.spi.Deserializer;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;
import io.soabase.recordbuilder.serialization.spi.Serializer;
import io.soabase.recordbuilder.serialization.token.MetaToken.FieldNameToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

import static io.soabase.recordbuilder.serialization.spi.SerializationRegistry.isAssignablePredicate;

public class CollectionSerializers {
    private CollectionSerializers() {
    }

    public static void register(SerializationRegistry registry) {
        registry.registerSerializer("Iterable", isAssignablePredicate(Iterable.class), _ -> iterableSerializer);
        registry.registerSerializer("Map", isAssignablePredicate(Map.class), _ -> mapSerializer);

        registry.registerDeserializer("PriorityQueue", isAssignablePredicate(PriorityQueue.class),
                type -> collectionDeserializer(type, PriorityQueue::new));
        registry.registerDeserializer("Stack", isAssignablePredicate(Stack.class),
                type -> collectionDeserializer(type, Stack::new));
        registry.registerDeserializer("Vector", isAssignablePredicate(Vector.class),
                type -> collectionDeserializer(type, Vector::new));
        registry.registerDeserializer("Set", isAssignablePredicate(Set.class),
                type -> collectionDeserializer(type, HashSet::new));
        registry.registerDeserializer("Queue", isAssignablePredicate(Queue.class),
                type -> collectionDeserializer(type, LinkedList::new));
        registry.registerDeserializer("List", isAssignablePredicate(List.class),
                type -> collectionDeserializer(type, ArrayList::new));
        registry.registerDeserializer("Map", isAssignablePredicate(Map.class), CollectionSerializers::mapDeserializer);
        registry.registerDeserializer("Collection", isAssignablePredicate(Collection.class),
                type -> collectionDeserializer(type, ArrayList::new));
    }

    public static Deserializer mapDeserializer(Type type) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type itemType = parameterizedType.getActualTypeArguments()[1];

        return (registry, stream) -> {
            Map<String, Object> map = new HashMap<>(); // TODO
            Deserializer deserializer = registry.requiredDeserializer(itemType);

            for (stream.object(); stream.hasMore(); stream.advance()) {
                FieldNameToken fieldNameToken = stream.current().as(FieldNameToken.class);
                stream.advance();

                map.put(fieldNameToken.fieldName(), deserializer.deserialize(registry, stream));
            }
            return map;
        };
    }

    public static Deserializer collectionDeserializer(Type type, Supplier<Collection<Object>> collectionFactory) {
        Type itemType = MoreTypes.componentType(type);

        return (registry, stream) -> {
            Deserializer deserializer = registry.requiredDeserializer(itemType);
            Collection<Object> list = collectionFactory.get();

            for (stream.array(); stream.hasMore(); stream.advance()) {
                list.add(deserializer.deserialize(registry, stream));
            }

            return list;
        };
    }

    public static final Serializer iterableSerializer = (registry, obj, sink) -> {
        Iterable<?> iterable = (Collection<?>) obj;

        Iterator<?> iterator = iterable.iterator();
        sink.startArray();
        boolean isFirst = true;
        while (iterator.hasNext()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sink.separator();
            }

            Object item = iterator.next();
            if (item == null) {
                sink.nullValue();
            } else {
                Serializer serializer = registry.requiredSerializer(item.getClass());
                serializer.serialize(registry, item, sink);
            }
        }
        sink.endArray();
    };

    public static final Serializer mapSerializer = (registry, obj, sink) -> {
        Map<?, ?> map = (Map<?, ?>) obj;
        Serializer serializer = null;

        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        sink.startObject();
        boolean isFirst = true;
        while (iterator.hasNext()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sink.separator();
            }

            Map.Entry<?, ?> item = iterator.next();
            String key = String.valueOf(item.getKey());
            Object value = item.getValue();

            if (serializer == null) {
                serializer = registry.requiredSerializer(value.getClass()); // TODO
            }
            sink.startField(key);
            serializer.serialize(registry, value, sink);
        }
        sink.endObject();
    };

    public static final Deserializer listDeserializer = (registry, stream) -> {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    };
}
