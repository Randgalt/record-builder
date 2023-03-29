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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;
import java.util.List;

class TestValidation {
    @Test
    void testNotNulls() {
        Assertions.assertThrows(NullPointerException.class, () -> RequiredRecordBuilder.builder().build());
    }

    @Test
    void testValidation() {
        Assertions.assertThrows(ValidationException.class, () -> RequiredRecord2Builder.builder().build());
    }

    @Test
    void testNotNullsWithNewProperty() {
        var valid = RequiredRecordBuilder.builder().hey("hey").i(1).l(List.of()).build();
        Assertions.assertThrows(NullPointerException.class, () -> valid.withHey(null));
    }

    @Test
    void testValidationWithNewProperty() {
        var valid = RequiredRecord2Builder.builder().hey("hey").i(1).build();
        Assertions.assertThrows(ValidationException.class, () -> valid.withHey(null));
    }

    @Test
    void testRequestWithValid() {
        Assertions.assertDoesNotThrow(
                () -> RequestWithValidBuilder.builder().part(new RequestWithValid.Part("jsfjsf")).build());
        Assertions.assertThrows(ValidationException.class,
                () -> RequestWithValidBuilder.builder().part(new RequestWithValid.Part("")).build());
    }
}
