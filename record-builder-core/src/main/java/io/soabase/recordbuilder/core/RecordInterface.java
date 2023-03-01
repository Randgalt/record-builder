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

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface RecordInterface {
    boolean addRecordBuilder() default true;

    @Target({ElementType.TYPE, ElementType.PACKAGE})
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Include {
        /**
         * @return collection of classes to include
         */
        Class<?>[] value() default {};

        /**
         * Synonym for {@code value()}. When using the other attributes it maybe clearer to
         * use {@code classes()} instead of {@code value()}. Note: both attributes are applied
         * (i.e. a union of classes from both attributes).
         *
         * @return collection of classes
         */
        Class<?>[] classes() default {};

        /**
         * If true the generated record is annotated with {@code @RecordBuilder}
         *
         * @return true/false
         */
        boolean addRecordBuilder() default true;

        /**
         * Pattern used to generate the package for the generated class. The value
         * is the literal package name however two replacement values can be used. '@'
         * is replaced with the package of the {@code Include} annotation. '*' is replaced with
         * the package of the included class.
         *
         * @return package pattern
         */
        String packagePattern() default "@";
    }
}
