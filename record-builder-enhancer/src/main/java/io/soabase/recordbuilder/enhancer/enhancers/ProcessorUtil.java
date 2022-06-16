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

import io.soabase.recordbuilder.enhancer.spi.Entry;
import io.soabase.recordbuilder.enhancer.spi.Processor;
import io.soabase.recordbuilder.enhancer.spi.RecordBuilderEnhancer;

import javax.lang.model.type.TypeMirror;
import java.util.stream.Stream;

class ProcessorUtil {
    private ProcessorUtil() {
    }

    static boolean isNotHandledByOthers(Class<? extends RecordBuilderEnhancer> enhancer, Processor processor, Entry entry, TypeMirror... types)
    {
        if (!processor.hasEnhancer(enhancer)) {
            return true;
        }
        if (types == null) {
            return true;
        }
        return Stream.of(types).noneMatch(type -> processor.types().isAssignable(entry.erasedType(), type));
    }
}
