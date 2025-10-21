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
package io.soabase.recordbuilder.processor;

import io.soabase.recordbuilder.core.RecordBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RecordBuilderOptions {
    private static final Map<String, Object> defaultValues = buildDefaultValues();

    static RecordBuilder.Options build(Map<String, String> options) {
        return (RecordBuilder.Options) Proxy.newProxyInstance(RecordBuilderOptions.class.getClassLoader(),
                new Class[] { RecordBuilder.Options.class }, (proxy, method, args) -> {
                    var name = method.getName();
                    var defaultValue = defaultValues.get(name);
                    var option = options.get(name);
                    if (option != null) {
                        if (defaultValue instanceof String) {
                            return option;
                        }
                        if (defaultValue instanceof Boolean) {
                            return Boolean.parseBoolean(option);
                        }
                        if (defaultValue instanceof Integer) {
                            return Integer.parseInt(option);
                        }
                        if (defaultValue instanceof Long) {
                            return Long.parseLong(option);
                        }
                        if (defaultValue instanceof Double) {
                            return Double.parseDouble(option);
                        }
                        if (defaultValue instanceof RecordBuilder.BuilderMode) {
                            return RecordBuilder.BuilderMode.valueOf(option);
                        }
                        if (defaultValue instanceof RecordBuilder.ConcreteSettersForOptionalMode) {
                            return RecordBuilder.ConcreteSettersForOptionalMode.valueOf(option);
                        }
                        throw new IllegalArgumentException("Unhandled option type: " + defaultValue.getClass());
                    }
                    return defaultValue;
                });
    }

    static Set<String> optionNames() {
        return Stream.of(RecordBuilder.Options.class.getDeclaredMethods()).map(Method::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, Object> buildDefaultValues() {
        var workMap = new HashMap<String, Object>();
        for (Method method : RecordBuilder.Options.class.getDeclaredMethods()) {
            workMap.put(method.getName(), method.getDefaultValue());
        }
        workMap.put("toString", "Generated RecordBuilder.Options");
        return Map.copyOf(workMap);
    }

    private RecordBuilderOptions() {
    }
}
