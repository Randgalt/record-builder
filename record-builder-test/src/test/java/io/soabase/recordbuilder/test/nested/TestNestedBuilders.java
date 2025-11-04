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
package io.soabase.recordbuilder.test.nested;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestNestedBuilders {
    @Test
    public void testNestedBuilders() {
        Employee employee = EmployeeBuilder.builder().firstName("John").lastName("Doe").address(
                b -> b.address("123 Main St").cityState(cs -> cs.city("Springfield").state("IL")).country("USA"))
                .build();

        Employee employee2 = employee.with(b -> b.address(
                a -> a.cityState(cs -> cs.city("Shelbyville")).country("Nope").cityState(cs -> cs.state("Good"))));
        assertThat(employee2).isEqualTo(
                new Employee("John", "Doe", new Address("123 Main St", new CityState("Shelbyville", "Good"), "Nope")));

        Employee employee3 = employee.withAddress(a -> a.cityState(cs -> cs.city("Shelbyville2")));
        assertThat(employee3).isEqualTo(
                new Employee("John", "Doe", new Address("123 Main St", new CityState("Shelbyville2", "IL"), "USA")));

        Employee employee4 = employee.withAddress(a -> a.country("Israel"));
        assertThat(employee4).isEqualTo(
                new Employee("John", "Doe", new Address("123 Main St", new CityState("Springfield", "IL"), "Israel")));
    }
}
