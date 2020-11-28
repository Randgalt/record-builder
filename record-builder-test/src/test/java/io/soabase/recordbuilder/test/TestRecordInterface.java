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

import java.time.Instant;

import static io.soabase.recordbuilder.test.SimpleGenericRecordBuilder.SimpleGenericRecord;
import static io.soabase.recordbuilder.test.SimpleRecordBuilder.SimpleRecord;

public class TestRecordInterface
{
    @Test
    public void testHasDefaults()
    {
        var r1 = new HasDefaultsRecord(Instant.MIN, Instant.MAX);
        var r2 = r1.with(b -> b.tomorrow(Instant.MIN));
        Assertions.assertEquals(Instant.MIN, r1.time());
        Assertions.assertEquals(Instant.MAX, r1.tomorrow());
        Assertions.assertEquals(Instant.MIN, r2.time());
        Assertions.assertEquals(Instant.MIN, r2.tomorrow());
    }

    @Test
    public void testStaticConstructor()
    {
        var simple = SimpleRecord(10,"hey");
        Assertions.assertEquals(simple.i(), 10);
        Assertions.assertEquals(simple.s(), "hey");

        var now = Instant.now();
        var generic = SimpleGenericRecord(101, now);
        Assertions.assertEquals(generic.i(), 101);
        Assertions.assertEquals(generic.s(), now);
    }
}
