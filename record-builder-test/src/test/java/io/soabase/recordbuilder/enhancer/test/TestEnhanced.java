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
package io.soabase.recordbuilder.enhancer.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class TestEnhanced {
    @Test
    void testOptional()
    {
        OptionalTest optionalTest = new OptionalTest(null, null, null, null);
        Assertions.assertTrue(optionalTest.d().isEmpty());
        Assertions.assertTrue(optionalTest.i().isEmpty());
        Assertions.assertTrue(optionalTest.l().isEmpty());
        Assertions.assertTrue(optionalTest.o().isEmpty());
    }

    @Test
    void testString()
    {
        StringTest stringTest = new StringTest(null);
        Assertions.assertNotNull(stringTest.s());
    }

    @Test
    void testCopyCollectionNullableEmptyTest()
    {
        CopyCollectionNullableEmptyTest test = new CopyCollectionNullableEmptyTest(null, null, null, null);
        Assertions.assertTrue(test.c().isEmpty());
        Assertions.assertTrue(test.l().isEmpty());
        Assertions.assertTrue(test.s().isEmpty());
        Assertions.assertTrue(test.m().isEmpty());
    }

    @Test
    void testCustomEnhanced()
    {
        Instant now = Instant.now();
        int current = Counter.COUNTER.get();
        CustomEnhanced customEnhanced = new CustomEnhanced(null, null, now, null);
        Assertions.assertTrue(customEnhanced.o().isEmpty());
        Assertions.assertEquals(customEnhanced.s(), "");
        Assertions.assertTrue(customEnhanced.l().isEmpty());
        Assertions.assertEquals(customEnhanced.i(), now);
        Assertions.assertEquals(current - 1, Counter.COUNTER.get());
    }

    @Test
    void testGuavaCopyCollectionNullableEmptyTest()
    {
        GuavaCopyCollectionNullableEmptyTest test = new GuavaCopyCollectionNullableEmptyTest(null, null, null, null);
        Assertions.assertTrue(test.l() instanceof ImmutableList<BigInteger>);
        Assertions.assertTrue(test.s() instanceof ImmutableSet<Boolean>);
        Assertions.assertTrue(test.c() instanceof ImmutableSet<Instant>);
        Assertions.assertTrue(test.m() instanceof ImmutableMap<Map<String, Short>, AtomicBoolean>);
    }

    @Test
    void testPlugin()
    {
        int previous = Counter.COUNTER.get();
        new PluginTest(0);
        Assertions.assertEquals(previous + 1, Counter.COUNTER.get());
    }

    @Test
    void testNotNullAnnotations()
    {
        NotNullAnnotation notNullAnnotation = new NotNullAnnotation(null, 10, 10.0);
        Assertions.assertNull(notNullAnnotation.s());
        Assertions.assertNotNull(notNullAnnotation.i());
        Assertions.assertNotNull(notNullAnnotation.d());
        Assertions.assertThrows(NullPointerException.class, () -> new NotNullAnnotation("s", null, 10.0));
        Assertions.assertThrows(NullPointerException.class, () -> new NotNullAnnotation("s", 100, null));
        Assertions.assertThrows(NullPointerException.class, NotNullAnnotation::new);
    }
}
