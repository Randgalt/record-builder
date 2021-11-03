/**
 * Copyright 2019 Jordan Zimmerman
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestIncludes {
    @Test
    void testOptionsOnInclude() {
        // assert it's not prefixed with the enclosing class name
        IncludeWithOption.Hey hey = io.soabase.recordbuilder.test.HeyBuilder.builder().s("this is s").build();
        Assertions.assertEquals("this is s", hey.s());
    }
}
