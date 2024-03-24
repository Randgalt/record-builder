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
package io.soabase.recordbuilder.wrappers;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

public record Option<T>(T value) {
    public boolean isEmpty() {
        return value == null;
    }

    public boolean isPresent() {
        return value != null;
    }

    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public Stream<T> stream() {
        return (value == null) ? Stream.empty() : Stream.of(value);
    }

    public static <T> Option<T> wrap(Optional<T> optional) {
        return optional.map(Option::new).orElseGet(() -> new Option<>(null));
    }

    public Optional<T> unwrap() {
        return Optional.ofNullable(value);
    }
}
