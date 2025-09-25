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

import io.soabase.recordbuilder.core.RecordBuilder.Deconstructor;
import io.soabase.recordbuilder.core.RecordBuilder.DeconstructorAccessor;

@Deconstructor
public class AltAccessorTest {
    private String something;
    private int another;

    public static AltAccessorTest create(String something, int another) {
        return new AltAccessorTest(something, another);
    }

    private AltAccessorTest(String something, int another) {
        this.something = something;
        this.another = another;
    }

    @DeconstructorAccessor
    public String something() {
        return something;
    }

    @DeconstructorAccessor
    public int another() {
        return another;
    }
}
