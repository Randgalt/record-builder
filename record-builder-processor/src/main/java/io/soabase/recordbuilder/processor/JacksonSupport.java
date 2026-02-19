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

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.annotation.processing.ProcessingEnvironment;

import static javax.tools.Diagnostic.Kind.ERROR;

class JacksonSupport {
    private static final String JACKSON_2_ANNOTATION_PACKAGE = "com.fasterxml.jackson.databind.annotation";
    private static final String JACKSON_3_ANNOTATION_PACKAGE = "tools.jackson.databind.annotation";

    private static final String JSON_POJO_BUILDER = "JsonPOJOBuilder";

    private final ProcessingEnvironment processingEnv;
    private final boolean jackson2Present;
    private final boolean jackson3Present;

    JacksonSupport(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        jackson2Present = isAnnotationClassPresent(JACKSON_2_ANNOTATION_PACKAGE, JSON_POJO_BUILDER);
        jackson3Present = isAnnotationClassPresent(JACKSON_3_ANNOTATION_PACKAGE, JSON_POJO_BUILDER);
    }

    private boolean isAnnotationClassPresent(String packageName, String className) {
        return processingEnv.getElementUtils().getTypeElement(packageName + "." + className) != null;
    }

    public void addJacksonAnnotations(RecordBuilder.Options metaData, TypeSpec.Builder builder) {
        // return without further processing if no annotation is enabled
        if (!anyJacksonAnnotationEnabled(metaData)) {
            return;
        }

        switch (metaData.jackson().version()) {
        case AUTO -> {
            if (!jackson2Present && !jackson3Present) {
                processingEnv.getMessager().printMessage(ERROR,
                        "jackson.jsonPOJOBuilder is enabled but Jackson is not found on classpath. "
                                + "Add jackson-databind dependency or disable jsonPOJOBuilder.");
                return;
            }

            if (jackson2Present) {
                addJacksonAnnotations(metaData, builder, JACKSON_2_ANNOTATION_PACKAGE);
            }

            if (jackson3Present) {
                addJacksonAnnotations(metaData, builder, JACKSON_3_ANNOTATION_PACKAGE);
            }
        }

        case JACKSON_2 -> {
            if (!jackson2Present) {
                processingEnv.getMessager().printMessage(ERROR,
                        "jackson.version is set to JACKSON_2 but Jackson 2.x is not found on classpath. "
                                + "Add jackson-databind 2.x dependency or change version to AUTO.");
                return;
            }

            addJacksonAnnotations(metaData, builder, JACKSON_2_ANNOTATION_PACKAGE);
        }

        case JACKSON_3 -> {
            if (!jackson3Present) {
                processingEnv.getMessager().printMessage(ERROR,
                        "jackson.version is set to JACKSON_3 but Jackson 3.x is not found on classpath. "
                                + "Add jackson-databind 3.x dependency or change version to AUTO.");
                return;
            }

            addJacksonAnnotations(metaData, builder, JACKSON_3_ANNOTATION_PACKAGE);
        }
        }
    }

    private boolean anyJacksonAnnotationEnabled(RecordBuilder.Options metaData) {
        return metaData.jackson().jsonPOJOBuilder();
    }

    private void addJacksonAnnotations(RecordBuilder.Options metaData, TypeSpec.Builder builder, String packageName) {
        if (metaData.jackson().jsonPOJOBuilder()) {
            addJsonPOJOBuilderAnnotation(metaData, builder, packageName);
        }
    }

    private void addJsonPOJOBuilderAnnotation(RecordBuilder.Options metaData, TypeSpec.Builder builder,
            String packageName) {
        final var annotationSpec = AnnotationSpec.builder(ClassName.get(packageName, JSON_POJO_BUILDER))
                .addMember("withPrefix", "$S", metaData.setterPrefix()).build();

        builder.addAnnotation(annotationSpec);
    }
}
