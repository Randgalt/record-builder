/**
 * Copyright (C) 2008 Google Inc.
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

package io.soabase.com.google.inject.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

import static io.soabase.com.google.inject.Checks.checkArgument;
import static io.soabase.com.google.inject.Checks.checkNotNull;

/**
 * Static methods for working with types that we aren't publishing in the public {@code Types} API.
 *
 * @author jessewilson@google.com (Jesse Wilson)
 */
public class MoreTypes {

    public static final Type[] EMPTY_TYPE_ARRAY = new Type[] {};

    private MoreTypes() {
    }

    /**
     * Returns true if {@code type} is free from type variables.
     */
    private static boolean isFullySpecified(Type type) {
        if (type instanceof Class) {
            return true;

        } else if (type instanceof CompositeType) {
            return ((CompositeType) type).isFullySpecified();

        } else if (type instanceof TypeVariable) {
            return false;

        } else {
            return ((CompositeType) canonicalize(type)).isFullySpecified();
        }
    }

    /**
     * Returns a type that is functionally equal but not necessarily equal according to {@link Object#equals(Object)
     * Object.equals()}. The returned type is {@link Serializable}.
     */
    public static Type canonicalize(Type type) {
        if (type instanceof Class) {
            Class<?> c = (Class<?>) type;
            return c.isArray() ? new GenericArrayTypeImpl(canonicalize(c.getComponentType())) : c;

        } else if (type instanceof CompositeType) {
            return type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) type;
            return new ParameterizedTypeImpl(p.getOwnerType(), p.getRawType(), p.getActualTypeArguments());

        } else if (type instanceof GenericArrayType) {
            GenericArrayType g = (GenericArrayType) type;
            return new GenericArrayTypeImpl(g.getGenericComponentType());

        } else if (type instanceof WildcardType) {
            WildcardType w = (WildcardType) type;
            return new WildcardTypeImpl(w.getUpperBounds(), w.getLowerBounds());

        } else {
            // type is either serializable as-is or unsupported
            return type;
        }
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class.
            // Neal isn't either but suspects some pathological case related
            // to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            checkArgument(rawType instanceof Class, "Expected a Class, but <%s> is of type %s", type,
                    type.getClass().getName());
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            // we could use the variable's bounds, but that'll won't work if there are multiple.
            // having a raw type that's more general than necessary is okay
            return Object.class;

        } else if (type instanceof WildcardType wildcardType) {
            // added by JZ
            return getRawType(wildcardType.getUpperBounds()[0]);

        } else {
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or " + "GenericArrayType, but <"
                    + type + "> is of type " + type.getClass().getName());
        }
    }

    /**
     * Returns true if {@code a} and {@code b} are equal.
     */
    public static boolean equals(Type a, Type b) {
        if (a == b) {
            // also handles (a == null && b == null)
            return true;

        } else if (a instanceof Class) {
            // Class already specifies equals().
            return a.equals(b);

        } else if (a instanceof ParameterizedType) {
            if (!(b instanceof ParameterizedType)) {
                return false;
            }

            // TODO: save a .clone() call
            ParameterizedType pa = (ParameterizedType) a;
            ParameterizedType pb = (ParameterizedType) b;
            return Objects.equals(pa.getOwnerType(), pb.getOwnerType()) && pa.getRawType().equals(pb.getRawType())
                    && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());

        } else if (a instanceof GenericArrayType) {
            if (!(b instanceof GenericArrayType)) {
                return false;
            }

            GenericArrayType ga = (GenericArrayType) a;
            GenericArrayType gb = (GenericArrayType) b;
            return equals(ga.getGenericComponentType(), gb.getGenericComponentType());

        } else if (a instanceof WildcardType) {
            if (!(b instanceof WildcardType)) {
                return false;
            }

            WildcardType wa = (WildcardType) a;
            WildcardType wb = (WildcardType) b;
            return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
                    && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

        } else if (a instanceof TypeVariable) {
            if (!(b instanceof TypeVariable)) {
                return false;
            }
            TypeVariable<?> va = (TypeVariable) a;
            TypeVariable<?> vb = (TypeVariable) b;
            return va.getGenericDeclaration().equals(vb.getGenericDeclaration()) && va.getName().equals(vb.getName());

        } else {
            // This isn't a type we support. Could be a generic array type, wildcard type, etc.
            return false;
        }
    }

    private static int hashCodeOrZero(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    public static String typeToString(Type type) {
        return type instanceof Class ? ((Class) type).getName() : type.toString();
    }

    /**
     * Returns the generic supertype for {@code type}. For example, given a class {@code IntegerSet}, the result for
     * when supertype is {@code Set.class} is {@code Set<Integer>} and the result when the supertype is
     * {@code Collection.class} is {@code Collection<Integer>}.
     */
    public static Type getGenericSupertype(Type type, Class<?> rawType, Class<?> toResolve) {
        if (toResolve == rawType) {
            return type;
        }

        // we skip searching through interfaces if unknown is an interface
        if (toResolve.isInterface()) {
            Class[] interfaces = rawType.getInterfaces();
            for (int i = 0, length = interfaces.length; i < length; i++) {
                if (interfaces[i] == toResolve) {
                    return rawType.getGenericInterfaces()[i];
                } else if (toResolve.isAssignableFrom(interfaces[i])) {
                    return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
                }
            }
        }

        // check our supertypes
        if (!rawType.isInterface()) {
            while (rawType != Object.class) {
                Class<?> rawSupertype = rawType.getSuperclass();
                if (rawSupertype == toResolve) {
                    return rawType.getGenericSuperclass();
                } else if (toResolve.isAssignableFrom(rawSupertype)) {
                    return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
                }
                rawType = rawSupertype;
            }
        }

        // we can't resolve this further
        return toResolve;
    }

    public static Type resolveTypeVariable(Type type, Class<?> rawType, TypeVariable unknown) {
        Class<?> declaredByRaw = declaringClassOf(unknown);

        // we can't reduce this further
        if (declaredByRaw == null) {
            return unknown;
        }

        Type declaredBy = getGenericSupertype(type, rawType, declaredByRaw);
        if (declaredBy instanceof ParameterizedType) {
            int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
            return ((ParameterizedType) declaredBy).getActualTypeArguments()[index];
        }

        return unknown;
    }

    private static int indexOf(Object[] array, Object toFind) {
        for (int i = 0; i < array.length; i++) {
            if (toFind.equals(array[i])) {
                return i;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the declaring class of {@code typeVariable}, or {@code null} if it was not declared by a class.
     */
    private static Class<?> declaringClassOf(TypeVariable typeVariable) {
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
        return genericDeclaration instanceof Class ? (Class<?>) genericDeclaration : null;
    }

    public static class ParameterizedTypeImpl implements ParameterizedType, Serializable, CompositeType {
        private final Type ownerType;
        private final Type rawType;
        private final Type[] typeArguments;

        public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
            // require an owner type if the raw type needs it
            ensureOwnerType(ownerType, rawType);

            this.ownerType = ownerType == null ? null : canonicalize(ownerType);
            this.rawType = canonicalize(rawType);
            this.typeArguments = typeArguments.clone();
            for (int t = 0; t < this.typeArguments.length; t++) {
                checkNotNull(this.typeArguments[t], "type parameter");
                checkNotPrimitive(this.typeArguments[t], "type parameters");
                this.typeArguments[t] = canonicalize(this.typeArguments[t]);
            }
        }

        public Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        public Type getRawType() {
            return rawType;
        }

        public Type getOwnerType() {
            return ownerType;
        }

        public boolean isFullySpecified() {
            if (ownerType != null && !MoreTypes.isFullySpecified(ownerType)) {
                return false;
            }

            if (!MoreTypes.isFullySpecified(rawType)) {
                return false;
            }

            for (Type type : typeArguments) {
                if (!MoreTypes.isFullySpecified(type)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ParameterizedType && MoreTypes.equals(this, (ParameterizedType) other);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(typeArguments) ^ rawType.hashCode() ^ hashCodeOrZero(ownerType);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(30 * (typeArguments.length + 1));
            stringBuilder.append(typeToString(rawType));

            if (typeArguments.length == 0) {
                return stringBuilder.toString();
            }

            stringBuilder.append("<").append(typeToString(typeArguments[0]));
            for (int i = 1; i < typeArguments.length; i++) {
                stringBuilder.append(", ").append(typeToString(typeArguments[i]));
            }
            return stringBuilder.append(">").toString();
        }

        private static void ensureOwnerType(Type ownerType, Type rawType) {
            if (rawType instanceof Class<?>) {
                Class rawTypeAsClass = (Class) rawType;
                checkArgument(ownerType != null || rawTypeAsClass.getEnclosingClass() == null,
                        "No owner type for enclosed %s", rawType);
                checkArgument(ownerType == null || rawTypeAsClass.getEnclosingClass() != null,
                        "Owner type for unenclosed %s", rawType);
            }
        }

        private static final long serialVersionUID = 0;
    }

    public static class GenericArrayTypeImpl implements GenericArrayType, Serializable, CompositeType {
        private final Type componentType;

        public GenericArrayTypeImpl(Type componentType) {
            this.componentType = canonicalize(componentType);
        }

        public Type getGenericComponentType() {
            return componentType;
        }

        public boolean isFullySpecified() {
            return MoreTypes.isFullySpecified(componentType);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GenericArrayType && MoreTypes.equals(this, (GenericArrayType) o);
        }

        @Override
        public int hashCode() {
            return componentType.hashCode();
        }

        @Override
        public String toString() {
            return typeToString(componentType) + "[]";
        }

        private static final long serialVersionUID = 0;
    }

    /**
     * The WildcardType interface supports multiple upper bounds and multiple lower bounds. We only support what the
     * Java 6 language needs - at most one bound. If a lower bound is set, the upper bound must be Object.class.
     */
    public static class WildcardTypeImpl implements WildcardType, Serializable, CompositeType {
        private final Type upperBound;
        private final Type lowerBound;

        public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            checkArgument(lowerBounds.length <= 1, "Must have at most one lower bound.");
            checkArgument(upperBounds.length == 1, "Must have exactly one upper bound.");

            if (lowerBounds.length == 1) {
                checkNotNull(lowerBounds[0], "lowerBound");
                checkNotPrimitive(lowerBounds[0], "wildcard bounds");
                checkArgument(upperBounds[0] == Object.class, "bounded both ways");
                this.lowerBound = canonicalize(lowerBounds[0]);
                this.upperBound = Object.class;

            } else {
                checkNotNull(upperBounds[0], "upperBound");
                checkNotPrimitive(upperBounds[0], "wildcard bounds");
                this.lowerBound = null;
                this.upperBound = canonicalize(upperBounds[0]);
            }
        }

        public Type[] getUpperBounds() {
            return new Type[] { upperBound };
        }

        public Type[] getLowerBounds() {
            return lowerBound != null ? new Type[] { lowerBound } : EMPTY_TYPE_ARRAY;
        }

        public boolean isFullySpecified() {
            return MoreTypes.isFullySpecified(upperBound)
                    && (lowerBound == null || MoreTypes.isFullySpecified(lowerBound));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof WildcardType && MoreTypes.equals(this, (WildcardType) other);
        }

        @Override
        public int hashCode() {
            // this equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds());
            return (lowerBound != null ? 31 + lowerBound.hashCode() : 1) ^ (31 + upperBound.hashCode());
        }

        @Override
        public String toString() {
            if (lowerBound != null) {
                return "? super " + typeToString(lowerBound);
            } else if (upperBound == Object.class) {
                return "?";
            } else {
                return "? extends " + typeToString(upperBound);
            }
        }

        private static final long serialVersionUID = 0;
    }

    private static void checkNotPrimitive(Type type, String use) {
        checkArgument(!(type instanceof Class<?>) || !((Class) type).isPrimitive(),
                "Primitive types are not allowed in %s: %s", use, type);
    }

    /** A type formed from other types, such as arrays, parameterized types or wildcard types */
    private interface CompositeType {
        /** Returns true if there are no type variables in this type. */
        boolean isFullySpecified();
    }
}
