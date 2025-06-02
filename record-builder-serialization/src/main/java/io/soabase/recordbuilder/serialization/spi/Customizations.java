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
package io.soabase.recordbuilder.serialization.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Customizations {
    private final Map<CustomizationKey<?>, Object> customizations;

    private Customizations(Map<CustomizationKey<?>, Object> customizations) {
        this.customizations = Map.copyOf(customizations);
    }

    public <T> Optional<T> get(CustomizationKey<T> key) {
        return Optional.ofNullable(key.type().cast(customizations.get(key)));
    }

    public <T> T require(CustomizationKey<T> key) {
        return get(key).orElseThrow(() -> new IllegalArgumentException("Customization not found for key: " + key));
    }

    public boolean isTrue(CustomizationKey<Boolean> key) {
        return get(key).orElse(false);
    }

    public interface Builder {
        <T> Builder put(CustomizationKey<T> key, T value);

        Customizations build();
    }

    public static Builder builder() {
        return new Builder() {
            private final Map<CustomizationKey<?>, Object> customizations = new HashMap<>();

            @Override
            public <T> Builder put(CustomizationKey<T> key, T value) {
                customizations.put(key, value);
                return this;
            }

            @Override
            public Customizations build() {
                return new Customizations(customizations);
            }
        };
    }
}
