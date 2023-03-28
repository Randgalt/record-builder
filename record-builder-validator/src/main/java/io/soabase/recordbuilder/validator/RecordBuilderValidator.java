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
package io.soabase.recordbuilder.validator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

// complete Java Validation via reflection to avoid dependencies
public class RecordBuilderValidator {
    private static final Object validator;
    private static final Method validationMethod;
    private static final Constructor<?> constraintViolationExceptionCtor;
    private static final Class<?>[] emptyGroups = new Class<?>[0];

    private static final boolean PRINT_ERROR_STACKTRACE = Boolean.getBoolean("record_builder_validator_errors");

    static {
        Object localValidator = null;
        Method localValidationMethod = null;
        Constructor<?> localConstraintViolationExceptionCtor = null;
        try {
            var validationClass = Class.forName("javax.validation.Validation");
            var factoryClass = validationClass.getDeclaredMethod("buildDefaultValidatorFactory");
            var factory = factoryClass.invoke(null);
            var getValidatorMethod = factory.getClass().getMethod("getValidator");
            var constraintViolationExceptionClass = Class.forName("javax.validation.ConstraintViolationException");
            localValidator = getValidatorMethod.invoke(factory);
            localValidationMethod = localValidator.getClass().getMethod("validate", Object.class, Class[].class);
            localConstraintViolationExceptionCtor = constraintViolationExceptionClass.getConstructor(Set.class);
        } catch (Exception e) {
            if (PRINT_ERROR_STACKTRACE) {
                e.printStackTrace();
            }
        }
        validator = localValidator;
        validationMethod = localValidationMethod;
        constraintViolationExceptionCtor = localConstraintViolationExceptionCtor;
    }

    public static <T> T validate(T o) {
        if ((validator != null) && (validationMethod != null)) {
            try {
                var violations = validationMethod.invoke(validator, o, emptyGroups);
                if (!((Collection<?>) violations).isEmpty()) {
                    throw (RuntimeException) constraintViolationExceptionCtor.newInstance(violations);
                }
            } catch (IllegalAccessException | InstantiationException e) {
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
