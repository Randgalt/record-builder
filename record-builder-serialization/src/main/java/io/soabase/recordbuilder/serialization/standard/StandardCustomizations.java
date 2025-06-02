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

import static io.soabase.recordbuilder.serialization.spi.CustomizationKey.*;

public final class StandardCustomizations {
    private StandardCustomizations() {
    }

    public static Customizations.Builder putStandardCustomizations(Customizations.Builder builder) {
        builder.put(MAKE_METHODS_ACCESSIBLE, true);
        builder.put(IGNORE_UNKNOWN_FIELDS, true);
        builder.put(DO_NOT_WRITE_NULL_OBJECT_VALUES, true);
        return builder;
    }
}
