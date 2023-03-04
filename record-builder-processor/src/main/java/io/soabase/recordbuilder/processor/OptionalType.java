/*
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
package io.soabase.recordbuilder.processor;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public record OptionalType(TypeName typeName, TypeName valueType) {

    private static final TypeName optionalType = TypeName.get(Optional.class);
    private static final TypeName optionalIntType = TypeName.get(OptionalInt.class);
    private static final TypeName optionalLongType = TypeName.get(OptionalLong.class);
    private static final TypeName optionalDoubleType = TypeName.get(OptionalDouble.class);

    private static boolean isOptional(ClassType component) {
        if (component.typeName().equals(optionalType)) {
            return true;
        }
        return (component.typeName() instanceof ParameterizedTypeName parameterizedTypeName)
                && parameterizedTypeName.rawType.equals(optionalType);
    }

    static Optional<OptionalType> fromClassType(final ClassType component) {
        if (isOptional(component)) {
            if (!(component.typeName() instanceof ParameterizedTypeName parameterizedType)) {
                return Optional.of(new OptionalType(optionalType, TypeName.get(Object.class)));
            }
            final TypeName containingType = parameterizedType.typeArguments.isEmpty()
                    ? TypeName.get(Object.class)
                    : parameterizedType.typeArguments.get(0);
            return Optional.of(new OptionalType(optionalType, containingType));
        }
        if (component.typeName().equals(optionalIntType)) {
            return Optional.of(new OptionalType(optionalIntType, TypeName.get(int.class)));
        }
        if (component.typeName().equals(optionalLongType)) {
            return Optional.of(new OptionalType(optionalLongType, TypeName.get(long.class)));
        }
        if (component.typeName().equals(optionalDoubleType)) {
            return Optional.of(new OptionalType(optionalDoubleType, TypeName.get(double.class)));
        }
        return Optional.empty();
    }

    public boolean isOptional() {
        return typeName.equals(optionalType);
    }
}
