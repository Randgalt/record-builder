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
package io.soabase.recordbuilder.option;

import java.util.Optional;

public sealed interface Option<T> permits Some, None {
    T value();

    static <T> Some<T> some(T value) {
        return new Some<>(value);
    }

    static <T> None<T> none() {
        return new None<>();
    }

    static <T> Option<T> of(T value) {
        return (value != null) ? some(value) : none();
    }

    static <T> Option<T> from(Optional<T> optional) {
        return optional.isPresent() ? some(optional.get()) : none();
    }
}
