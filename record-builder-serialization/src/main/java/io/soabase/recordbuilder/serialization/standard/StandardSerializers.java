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

import io.soabase.recordbuilder.serialization.spi.Customizations;
import io.soabase.recordbuilder.serialization.spi.SerializationRegistry;

import static io.soabase.recordbuilder.serialization.standard.StandardCustomizations.putStandardCustomizations;

public class StandardSerializers {
    private StandardSerializers() {
    }

    public static SerializationRegistry standardRegistry() {
        Customizations customizations = putStandardCustomizations(Customizations.builder()).build();
        SerializationRegistry registry = new CachingSerializationRegistry(
                new NullHandlingSerializationRegistry(new StandardSerializationRegistry(customizations)));
        registerStandard(registry);
        return registry;
    }

    public static void registerStandard(SerializationRegistry registry) {
        PrimitiveSerializers.register(registry);
        ArraySerializers.register(registry);
        JdkSerializers.register(registry);
        CollectionSerializers.register(registry);

        RecordSerializer.register(registry);
        RecordDeserializer.register(registry);
        DeconstructorSerializer.register(registry);
        DeconstructorDeserializer.register(registry);
    }
}
