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
package io.soabase.recordbuilder.processor.options;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public interface InternalWitherOptions extends WitherOptions, GeneralOptions {
    static InternalWitherOptions build(Object source) {
        return (InternalWitherOptions) Proxy.newProxyInstance(InternalWitherOptions.class.getClassLoader(),
                new Class[] { InternalWitherOptions.class }, (proxy, method, args) -> {
                    Method sourceMethod = source.getClass().getMethod(method.getName());
                    return sourceMethod.invoke(source);
                });
    }
}
