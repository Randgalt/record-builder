/**
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
package io.soabase.recordbuilder.enhancer.enhancers;

public class GuavaCopyCollectionNullableEmpty extends CopyCollectionNullableEmptyBase {
    @Override
    protected boolean isInterface() {
        return false;
    }

    @Override
    protected String mapEmptyMethod() {
        return "()Lcom/google/common/collect/ImmutableMap;";
    }

    @Override
    protected String setEmptyMethod() {
        return "()Lcom/google/common/collect/ImmutableSet;";
    }

    @Override
    protected String listEmptyMethod() {
        return "()Lcom/google/common/collect/ImmutableList;";
    }

    @Override
    protected String mapCopyMethod() {
        return "(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;";
    }

    @Override
    protected String setCopyMethod() {
        return "(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableSet;";
    }

    @Override
    protected String listCopyMethod() {
        return "(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;";
    }

    @Override
    protected String map() {
        return "com/google/common/collect/ImmutableMap";
    }

    @Override
    protected String set() {
        return "com/google/common/collect/ImmutableSet";
    }

    @Override
    protected String list() {
        return "com/google/common/collect/ImmutableList";
    }
}
