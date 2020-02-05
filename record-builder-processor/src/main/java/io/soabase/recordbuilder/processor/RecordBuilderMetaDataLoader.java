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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.processing.ProcessingEnvironment;

class RecordBuilderMetaDataLoader {
	private final RecordBuilderMetaData metaData;

	RecordBuilderMetaDataLoader(ProcessingEnvironment processingEnv, Consumer<String> logger) {
		Map<String, String> options = processingEnv.getOptions();
		String metaDataClassName = options.get(RecordBuilderMetaData.JAVAC_OPTION_NAME);
		if ((metaDataClassName != null) && !metaDataClassName.isEmpty()) {
			RecordBuilderMetaData loadedMetaData = null;
			try {
				Class<?> clazz = Class.forName(metaDataClassName);
				loadedMetaData = (RecordBuilderMetaData) clazz.getDeclaredConstructor().newInstance();
				logger.accept("Found meta data: " + clazz);
			} catch (InvocationTargetException e) {
				// log the thrown exception instead of the invocation target exception
				logger.accept("Could not load meta data: " + metaDataClassName + " - " + e);
			} catch (Exception e) {
				logger.accept("Could not load meta data: " + metaDataClassName + " - " + e);
			}
			metaData = (loadedMetaData != null) ? loadedMetaData : RecordBuilderMetaData.DEFAULT;
		} else {
			metaData = new OptionBasedRecordBuilderMetaData(options);
		}
	}

	RecordBuilderMetaData getMetaData() {
		return metaData;
	}
}
