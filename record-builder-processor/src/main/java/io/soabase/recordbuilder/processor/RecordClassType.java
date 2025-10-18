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

import com.palantir.javapoet.TypeName;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import java.util.List;

public class RecordClassType extends ClassType {
    private final TypeKind typeKind;
    private final TypeName rawTypeName;
    private final String accessorName;
    private final List<? extends AnnotationMirror> accessorAnnotations;
    private final List<? extends AnnotationMirror> canonicalConstructorAnnotations;

    public RecordClassType(TypeKind typeKind, TypeName typeName, TypeName rawTypeName, String name, String accessorName,
            List<? extends AnnotationMirror> accessorAnnotations,
            List<? extends AnnotationMirror> canonicalConstructorAnnotations) {
        super(typeName, name);
        this.typeKind = typeKind;
        this.rawTypeName = rawTypeName;
        this.accessorName = accessorName;
        this.accessorAnnotations = accessorAnnotations;
        this.canonicalConstructorAnnotations = canonicalConstructorAnnotations;
    }

    public TypeKind typeKind() {
        return typeKind;
    }

    public String accessorName() {
        return accessorName;
    }

    public TypeName rawTypeName() {
        return rawTypeName;
    }

    public List<? extends AnnotationMirror> getAccessorAnnotations() {
        return accessorAnnotations;
    }

    public List<? extends AnnotationMirror> getCanonicalConstructorAnnotations() {
        return canonicalConstructorAnnotations;
    }
}
