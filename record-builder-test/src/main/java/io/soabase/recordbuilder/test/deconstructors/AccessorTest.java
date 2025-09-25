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

import java.util.Objects;

@Deconstructor
public class AccessorTest {
    private String something;
    private int another;

    public static AccessorTest create(String something, int another) {
        return new AccessorTest(something, another);
    }

    private AccessorTest(String something, int another) {
        this.something = something;
        this.another = another;
    }

    @DeconstructorAccessor
    public String getSomething() {
        return something;
    }

    @DeconstructorAccessor
    public int getAnother() {
        return another;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AccessorTest that))
            return false;
        return getAnother() == that.getAnother() && Objects.equals(getSomething(), that.getSomething());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSomething(), getAnother());
    }
}
