package io.soabase.com.google.inject.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;

public final class MoreTypes {
    private MoreTypes() {
    }

    public static Type componentType(Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                return clazz.getComponentType();
            }
            if (Collection.class.isAssignableFrom(clazz)) {
                return Object.class;
            }
        } else if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        } else if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments()[0];
        } else if (type instanceof WildcardType) {
            return ((WildcardType) type).getUpperBounds()[0];
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    public static Type unbox(Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz == Character.class) {
                return char.class;
            } else if (clazz == Boolean.class) {
                return boolean.class;
            } else if (clazz == Byte.class) {
                return byte.class;
            } else if (clazz == Short.class) {
                return short.class;
            } else if (clazz == Integer.class) {
                return int.class;
            } else if (clazz == Long.class) {
                return long.class;
            } else if (clazz == Float.class) {
                return float.class;
            } else if (clazz == Double.class) {
                return double.class;
            }
        }
        return type;
    }
}
