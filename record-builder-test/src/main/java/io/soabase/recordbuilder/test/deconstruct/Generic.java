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
package io.soabase.recordbuilder.test.deconstruct;

import io.soabase.recordbuilder.core.RecordBuilder;
import io.soabase.recordbuilder.core.RecordTuple;

@RecordTuple
@RecordBuilder.Options(tupleSuffix = "Shim", fromMethodName = "to")
public class Generic<T1, T2> {
    private final T1 t1;
    private final T2 t2;

    public Generic(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    @RecordTuple.Component
    public String getString() {
        return "string";
    }

    @RecordTuple.Component
    public T1 t1() {
        return t1;
    }

    @RecordTuple.Component("kookoo")
    public T2 t2() {
        return t2;
    }
}
