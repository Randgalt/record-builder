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

import org.junit.jupiter.api.Test;

import static io.soabase.recordbuilder.test.deconstructors.AccessorTestDaoBuilder.AccessorTestDao;
import static org.assertj.core.api.Assertions.assertThat;

public class TestAccessors {
    @Test
    public void testAccessors() {
        AccessorTest accessorTest = AccessorTest.create("hey", 42);
        assertThat(AccessorTestDao("hey", 42)).isEqualTo(AccessorTestDao.from(accessorTest));
    }

    @Test
    public void testAccessorsNoBuilder() {
        AccessorTestNoBuilder accessorTest = new AccessorTestNoBuilder("hey", 42);
        assertThat(new AccessorTestNoBuilderDao("hey", 42)).isEqualTo(AccessorTestNoBuilderDao.from(accessorTest));
    }
}
