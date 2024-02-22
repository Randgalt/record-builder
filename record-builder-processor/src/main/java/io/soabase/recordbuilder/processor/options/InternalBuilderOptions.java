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
package io.soabase.recordbuilder.processor.options;

import io.soabase.recordbuilder.core.RecordBuilder;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public interface InternalBuilderOptions extends WitherOptions, GeneralOptions {
    static InternalBuilderOptions build(Object source) {
        return (InternalBuilderOptions) Proxy.newProxyInstance(InternalBuilderOptions.class.getClassLoader(),
                new Class[] { InternalBuilderOptions.class }, (proxy, method, args) -> {
                    Method sourceMethod = source.getClass().getMethod(method.getName());
                    return sourceMethod.invoke(source);
                });
    }

    String suffix();

    String interfaceSuffix();

    String copyMethodName();

    String builderMethodName();

    String buildMethodName();

    String componentsMethodName();

    boolean enableWither();

    boolean inheritComponentAnnotations();

    boolean emptyDefaultForOptional();

    boolean addConcreteSettersForOptional();

    boolean interpretNotNulls();

    String interpretNotNullsPattern();

    boolean useValidationApi();

    boolean useImmutableCollections();

    boolean useUnmodifiableCollections();

    boolean allowNullableCollections();

    boolean addSingleItemCollectionBuilders();

    String singleItemBuilderPrefix();

    String setterPrefix();

    boolean enableGetters();

    String getterPrefix();

    String booleanPrefix();

    String beanClassName();

    boolean addStaticBuilder();

    String mutableListClassName();

    String mutableSetClassName();

    String mutableMapClassName();

    Modifier[] builderClassModifiers();

    boolean publicBuilderConstructors();

    RecordBuilder.BuilderMode builderMode();

    String stagedBuilderMethodName();

    String stagedBuilderMethodSuffix();

    boolean onceOnlyAssignment();

    String onceOnlyAssignmentName();
}
