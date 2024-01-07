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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestOnceOnly {
    @Test
    void testOnceOnly() {
        assertThrows(IllegalStateException.class, () -> OnceOnlyBuilder.builder().a(1).a(2));
        assertThrows(IllegalStateException.class, () -> OnceOnlyBuilder.builder().b(1).b(2));
        assertThrows(IllegalStateException.class, () -> OnceOnlyBuilder.builder().c(1).c(2));

        assertDoesNotThrow(() -> OnceOnlyBuilder.builder().a(1).b(2).c(3).build());
    }

    @Test
    void testStagedOnceOnly() {
        OnceOnlyBuilder.AStage aStage = OnceOnlyBuilder.stagedBuilder();

        OnceOnlyBuilder.BStage bStage = aStage.a(1);
        assertThrows(IllegalStateException.class, () -> aStage.a(1));

        OnceOnlyBuilder.CStage cStage = bStage.b(2);
        assertThrows(IllegalStateException.class, () -> bStage.b(2));

        OnceOnlyBuilder.OnceOnlyBuilderStage builderStage = cStage.c(3);
        assertThrows(IllegalStateException.class, () -> cStage.c(3));

        assertDoesNotThrow(builderStage::build);
        assertEquals(new OnceOnly(1, 2, 3), builderStage.build());
    }
}
