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
package io.soabase.recordbuilder.test.deconstructors;

import io.soabase.recordbuilder.core.DeconstructorFull;

import java.util.function.Consumer;

public class ComplexClass<T, L extends ComplexClass<T, L>> {
    private final T t;
    private final ComplexClass<T, L> c;

    public ComplexClass(T t, ComplexClass<T, L> c) {
        this.t = t;
        this.c = c;
    }

    @DeconstructorFull
    public void deconstructor(Consumer<T> t, Consumer<ComplexClass<T, L>> c) {
        t.accept(this.t);
        c.accept(this.c);
    }
}
