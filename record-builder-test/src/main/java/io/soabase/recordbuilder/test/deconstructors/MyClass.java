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

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MyClass {
    private final int i;
    private final String s;

    public MyClass(int i, String s) {
        this.i = i;
        this.s = s;
    }

    @RecordBuilder.Deconstructor
    public void deconstructor(IntConsumer i, Consumer<String> s) {
        i.accept(this.i);
        s.accept(this.s);
    }
}
