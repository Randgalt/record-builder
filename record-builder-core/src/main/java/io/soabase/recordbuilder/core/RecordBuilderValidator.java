/**
 * Copyright 2019 Jordan Zimmerman
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.recordbuilder.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RecordBuilderValidator {
    private static final Object validator;
    private static final Method validationMethod;
    private static final Class<?>[] emptyGroups = new Class<?>[0];

    static {
        Object localValidator = null;
        Method localValidationMethod = null;
        try {
            var validationClass = Class.forName("javax.validation.Validation");
            var factoryClass = validationClass.getDeclaredMethod("buildDefaultValidatorFactory");
            var factory = factoryClass.invoke(null);
            var getValidatorMethod = factory.getClass().getMethod("getValidator");
            localValidator = getValidatorMethod.invoke(factory);
            localValidationMethod = localValidator.getClass().getMethod("validate", Object.class, Class[].class);
        } catch (Exception e) {
            // TODO - silently ignore or print a diagnostic?
        }
        validator = localValidator;
        validationMethod = localValidationMethod;
    }

    public static <T> T validate(T o) {
        if ((validator != null) && (validationMethod != null)) {
            try {
                validationMethod.invoke(validator, o, emptyGroups);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() != null) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    }
                    throw new RuntimeException(e.getCause());
                }
                throw new RuntimeException(e);
            }
        }
        return o;
    }

    private RecordBuilderValidator() {
    }
}
