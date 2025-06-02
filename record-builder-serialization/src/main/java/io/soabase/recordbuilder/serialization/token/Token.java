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
package io.soabase.recordbuilder.serialization.token;

import java.util.NoSuchElementException;

public sealed interface Token permits ValueToken, MetaToken {
    default <T extends Token> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        // TODO
        throw new NoSuchElementException("Cannot cast " + this.getClass().getName() + " to " + type.getName());
    }
}
