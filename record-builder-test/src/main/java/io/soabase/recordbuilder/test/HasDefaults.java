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

import io.soabase.recordbuilder.core.IgnoreDefaultMethod;
import io.soabase.recordbuilder.core.RecordInterface;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RecordInterface
public interface HasDefaults {
    Instant time();

    default Instant tomorrow() {
        return Instant.now().plusMillis(TimeUnit.DAYS.toMillis(1));
    }

    @IgnoreDefaultMethod
    default void complexMethod(String s1, String s2) {
        // do nothing
    }
}
