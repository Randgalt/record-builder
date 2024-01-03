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
package io.soabase.recordbuilder.processor;

import com.squareup.javapoet.AnnotationSpec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import static java.lang.Character.isISOControl;

// copied from JavaPoet and altered to ignore class -> TypeMirror issues
class AnnotationSpecUtil {
    public static AnnotationSpec getAnnotationSpec(Annotation annotation, boolean ignoreClassTypes) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(annotation.annotationType());
        try {
            Method[] methods = annotation.annotationType().getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (ignoreClassTypes && (method.getReturnType().isAssignableFrom(Class.class)
                        || method.getReturnType().isAssignableFrom(Class[].class))) {
                    continue;
                }

                Object value = method.invoke(annotation);
                if (value.getClass().isArray()) {
                    for (int i = 0; i < Array.getLength(value); i++) {
                        addMemberForValue(builder, method.getName(), Array.get(value, i));
                    }
                    continue;
                }
                if (value instanceof Annotation) {
                    builder.addMember(method.getName(), "$L", getAnnotationSpec((Annotation) value, ignoreClassTypes));
                    continue;
                }
                addMemberForValue(builder, method.getName(), value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Reflecting " + annotation + " failed!", e);
        }
        return builder.build();
    }

    @SuppressWarnings("UnusedReturnValue")
    private static AnnotationSpec.Builder addMemberForValue(AnnotationSpec.Builder builder, String memberName,
            Object value) {
        if (value instanceof Class<?>) {
            return builder.addMember(memberName, "$T.class", value);
        }
        if (value instanceof Enum) {
            return builder.addMember(memberName, "$T.$L", value.getClass(), ((Enum<?>) value).name());
        }
        if (value instanceof String) {
            return builder.addMember(memberName, "$S", value);
        }
        if (value instanceof Float) {
            return builder.addMember(memberName, "$Lf", value);
        }
        if (value instanceof Character) {
            return builder.addMember(memberName, "'$L'", characterLiteralWithoutSingleQuotes((char) value));
        }
        return builder.addMember(memberName, "$L", value);
    }

    @SuppressWarnings({ "EnhancedSwitchMigration", "UnnecessaryUnicodeEscape" })
    static String characterLiteralWithoutSingleQuotes(char c) {
        // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
        switch (c) {
        case '\b':
            return "\\b"; /* \u0008: backspace (BS) */
        case '\t':
            return "\\t"; /* \u0009: horizontal tab (HT) */
        case '\n':
            return "\\n"; /* \u000a: linefeed (LF) */
        case '\f':
            return "\\f"; /* \u000c: form feed (FF) */
        case '\r':
            return "\\r"; /* \u000d: carriage return (CR) */
        case '\"':
            return "\""; /* \u0022: double quote (") */
        case '\'':
            return "\\'"; /* \u0027: single quote (') */
        case '\\':
            return "\\\\"; /* \u005c: backslash (\) */
        default:
            return isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
        }
    }
}
