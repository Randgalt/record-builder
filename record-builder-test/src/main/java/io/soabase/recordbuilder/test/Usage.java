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
package io.soabase.recordbuilder.test;

public class Usage {
    public static void main(String[] args) {
        var hey = SimpleRecordBuilder.builder().i(10).s("hey").build();
        System.out.println(hey);
        var hey2 = SimpleRecordBuilder.builder(hey).i(100).build();
        System.out.println(hey2);

        var person = new PersonRecord("me", 42);
        outputPerson(person);
        var aged = PersonRecordBuilder.builder(person).age(100).build();
        outputPerson(aged);
    }

    private static void outputPerson(Person p) {
        System.out.println(p.toString());
    }
}
