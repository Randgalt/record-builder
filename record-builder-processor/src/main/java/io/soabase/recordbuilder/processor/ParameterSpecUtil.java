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

import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.ElementType;
import javax.annotation.processing.ProcessingEnvironment;

final class ParameterSpecUtil {

    public static ParameterSpec.Builder createParameterSpec(RecordClassType component, boolean inheritAnnotations,
            ProcessingEnvironment processingEnv) {
        return createParameterSpec(component, component.typeName(), inheritAnnotations, processingEnv);
    }

    public static ParameterSpec.Builder createParameterSpec(RecordClassType component, TypeName type,
            boolean inheritAnnotations, ProcessingEnvironment processingEnv) {
        if (inheritAnnotations) {
            type = ElementUtils.getAnnotatedTypeName(component, type, ElementType.PARAMETER, processingEnv);
        }
        return ParameterSpec.builder(type, component.name());
    }

    private ParameterSpecUtil() {
    }
}
