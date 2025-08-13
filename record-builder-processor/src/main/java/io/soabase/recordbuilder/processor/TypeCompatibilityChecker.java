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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// from Claude.ai
class TypeCompatibilityChecker {
    private final Types typeUtils;

    TypeCompatibilityChecker(ProcessingEnvironment processingEnv) {
        this.typeUtils = processingEnv.getTypeUtils();
    }

    boolean canAssignMethodReturnToField(ExecutableElement method, TypeMirror fieldType) {
        TypeMirror returnType = method.getReturnType();

        // If no type variables, use simple assignment check
        if (method.getTypeParameters().isEmpty()) {
            return typeUtils.isAssignable(returnType, fieldType);
        }

        // Check if there exists a valid substitution of type variables
        // that would make the assignment possible
        return canBeAssignedWithTypeSubstitution(returnType, fieldType, method);
    }

    private boolean canBeAssignedWithTypeSubstitution(TypeMirror returnType, TypeMirror fieldType,
            ExecutableElement method) {
        // Try to find a substitution that works
        Map<String, TypeMirror> substitution = new HashMap<>();
        return findValidSubstitution(returnType, fieldType, method.getTypeParameters(), substitution);
    }

    private boolean findValidSubstitution(TypeMirror returnType, TypeMirror fieldType,
            List<? extends TypeParameterElement> typeParams, Map<String, TypeMirror> substitution) {

        // Base case: try current substitution
        TypeMirror substitutedReturn = substituteTypeVariables(returnType, substitution);
        if (typeUtils.isAssignable(substitutedReturn, fieldType)) {
            return true;
        }

        // If we have unresolved type variables, try to resolve them
        TypeVariableCollector collector = new TypeVariableCollector();
        substitutedReturn.accept(collector, null);

        for (String unresolvedVar : collector.getTypeVariables()) {
            if (!substitution.containsKey(unresolvedVar)) {
                // Find the type parameter
                TypeParameterElement typeParam = findTypeParameter(unresolvedVar, typeParams);
                if (typeParam != null) {
                    // Try different concrete types that satisfy the bounds
                    for (TypeMirror candidate : generateCandidateTypes(typeParam, fieldType)) {
                        substitution.put(unresolvedVar, candidate);
                        if (findValidSubstitution(returnType, fieldType, typeParams, new HashMap<>(substitution))) {
                            return true;
                        }
                        substitution.remove(unresolvedVar);
                    }
                }
            }
        }

        return false;
    }

    private TypeParameterElement findTypeParameter(String name, List<? extends TypeParameterElement> typeParams) {
        return typeParams.stream().filter(tp -> tp.getSimpleName().toString().equals(name)).findFirst().orElse(null);
    }

    private List<TypeMirror> generateCandidateTypes(TypeParameterElement typeParam, TypeMirror targetType) {
        // Generate candidate types based on:
        // 1. The bounds of the type parameter
        // 2. What would make sense for the target type

        List<TypeMirror> candidates = new java.util.ArrayList<>();

        // Add upper bounds
        for (TypeMirror bound : typeParam.getBounds()) {
            if (!bound.toString().equals("java.lang.Object")) {
                candidates.add(bound);
            }
        }

        // If target type gives us hints, use them
        if (targetType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredTarget = (DeclaredType) targetType;
            for (TypeMirror typeArg : declaredTarget.getTypeArguments()) {
                if (isAssignableToAllBounds(typeArg, typeParam)) {
                    candidates.add(typeArg);
                }
            }
        }

        // Add Object as fallback
        if (candidates.isEmpty()) {
            candidates.add(typeUtils
                    .getDeclaredType((TypeElement) typeUtils.asElement(typeUtils.erasure(typeParam.asType()))));
        }

        return candidates;
    }

    private boolean isAssignableToAllBounds(TypeMirror candidate, TypeParameterElement typeParam) {
        for (TypeMirror bound : typeParam.getBounds()) {
            if (!typeUtils.isAssignable(candidate, bound)) {
                return false;
            }
        }
        return true;
    }

    private TypeMirror substituteTypeVariables(TypeMirror type, Map<String, TypeMirror> substitution) {
        return new TypeVariableSubstitutor(typeUtils, substitution).visit(type);
    }

    // Helper classes for type variable handling
    private static class TypeVariableCollector extends SimpleTypeVisitor8<Void, Void> {
        private final java.util.Set<String> typeVariables = new java.util.HashSet<>();

        @Override
        public Void visitTypeVariable(TypeVariable t, Void aVoid) {
            typeVariables.add(t.asElement().getSimpleName().toString());
            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType t, Void aVoid) {
            for (TypeMirror arg : t.getTypeArguments()) {
                arg.accept(this, aVoid);
            }
            return null;
        }

        public java.util.Set<String> getTypeVariables() {
            return typeVariables;
        }
    }

    private static class TypeVariableSubstitutor extends SimpleTypeVisitor8<TypeMirror, Void> {
        private final Types typeUtils;
        private final Map<String, TypeMirror> substitution;

        public TypeVariableSubstitutor(Types typeUtils, Map<String, TypeMirror> substitution) {
            this.typeUtils = typeUtils;
            this.substitution = substitution;
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, Void aVoid) {
            String name = t.asElement().getSimpleName().toString();
            return substitution.getOrDefault(name, t);
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
            TypeElement element = (TypeElement) t.asElement();
            TypeMirror[] newArgs = new TypeMirror[t.getTypeArguments().size()];

            for (int i = 0; i < t.getTypeArguments().size(); i++) {
                newArgs[i] = t.getTypeArguments().get(i).accept(this, aVoid);
            }

            if (newArgs.length == 0) {
                return t;
            } else {
                return typeUtils.getDeclaredType(element, newArgs);
            }
        }

        @Override
        protected TypeMirror defaultAction(TypeMirror e, Void aVoid) {
            return e;
        }
    }
}