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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.stream.IntStream;

import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.recordBuilderGeneratedAnnotation;

class WitherBuilder {
    private final BuilderContext context;

    WitherBuilder(BuilderContext context) {
        this.context = context;
    }

    TypeSpec buildWithClass() {
        /*
         * Adds a nested interface that adds withers similar to:
         *
         * public class MyRecordBuilder { public interface With { // with methods } }
         */
        var classBuilder = TypeSpec.interfaceBuilder(context.metaData().withClassName())
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addJavadoc("Add withers to {@code $L}\n", context.recordClassType().name()).addModifiers(Modifier.PUBLIC)
                .addTypeVariables(context.typeVariables());
        if (context.metaData().addClassRetainedGenerated()) {
            classBuilder.addAnnotation(recordBuilderGeneratedAnnotation);
        }
/*
        context.recordComponents().forEach(component -> addNestedGetterMethod(classBuilder, component, component.name()));
        addWithBuilderMethod(classBuilder);
        addWithSuppliedBuilderMethod(classBuilder);
        IntStream.range(0, context.recordComponents().size())
                .forEach(index -> add1WithMethod(classBuilder, recordComponents.get(index), index));
        if (context.metaData().addFunctionalMethodsToWith()) {
            classBuilder.addType(buildFunctionalInterface("Function", true))
                    .addType(buildFunctionalInterface("Consumer", false))
                    .addMethod(buildFunctionalHandler("Function", "map", true))
                    .addMethod(buildFunctionalHandler("Consumer", "accept", false));
        }

        return classBuilder.build();
    }

    private void addNestedGetterMethod(TypeSpec.Builder classBuilder, RecordClassType component, String methodName) {
        */
/*
         * For a single record component, add a getter similar to:
         *
         * T p();
         *//*

        var methodSpecBuilder = MethodSpec.methodBuilder(methodName)
                .addJavadoc("Return the current value for the {@code $L} record component in the builder\n",
                        component.name())
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).addAnnotation(generatedRecordBuilderAnnotation)
                .returns(component.typeName());
        addAccessorAnnotations(component, methodSpecBuilder, this::filterOutValid);
        classBuilder.addMethod(methodSpecBuilder.build());
*/
    }
}
