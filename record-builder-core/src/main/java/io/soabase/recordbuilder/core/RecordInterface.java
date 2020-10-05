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
package io.soabase.recordbuilder.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RecordInterface {
    boolean addRecordBuilder() default true;

    @Target({ElementType.TYPE, ElementType.PACKAGE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Include {
        Class<?>[] value();

        boolean addRecordBuilder() default true;

        /**
         * Pattern used to generate the package for the generated class. The value
         * is the literal package name however two replacement values can be used. '@'
         * is replaced with the package of the Include annotation. '*' is replaced with
         * the package of the included class.
         *
         * @return package pattern
         */
        String packagePattern() default "@";
    }
}
