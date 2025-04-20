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

import static org.assertj.core.api.Assertions.assertThat;

public class TestGenericClass {
    @Test
    public void testBuilder() {
        GenericClass<String> genericClass = new GenericClass<>("what?");
        GenericClassDao<String> unapplied = GenericClassDao.from(genericClass);
        assertThat(unapplied.t()).isEqualTo("what?");

        GenericClassDao<String> yo = GenericClassDaoBuilder.<String> builder().t("yo").build();
        assertThat(yo.t()).isEqualTo("yo");

        GenericClassDao<String> nope = yo.withT("nope");
        assertThat(nope.t()).isEqualTo("nope");
    }
}
