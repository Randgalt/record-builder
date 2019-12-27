/**
 * Copyright 2019 Jordan Zimmerman
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
package io.soabase.recordbuilder.processor;

import io.soabase.recordbuilder.core.RecordBuilderMetaData;

import java.util.function.Consumer;

class RecordBuilderMetaDataLoader {
    private final RecordBuilderMetaData metaData;

    RecordBuilderMetaDataLoader(String metaDataClassName, Consumer<String> logger) {
        RecordBuilderMetaData localMetaData = null;
        if ((metaDataClassName != null) && !metaDataClassName.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(metaDataClassName);
                localMetaData = (RecordBuilderMetaData) clazz.getDeclaredConstructor().newInstance();
                logger.accept("Found meta data: " + localMetaData.getClass());
            } catch (Exception e) {
                logger.accept("Could not load meta data: " + metaDataClassName + " - " + e.getMessage());
            }
        }
        metaData = (localMetaData != null) ? localMetaData : RecordBuilderMetaData.DEFAULT;
    }

    RecordBuilderMetaData getMetaData() {
        return metaData;
    }
}
