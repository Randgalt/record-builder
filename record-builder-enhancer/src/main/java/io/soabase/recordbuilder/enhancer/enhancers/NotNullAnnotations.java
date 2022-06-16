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
import recordbuilder.org.objectweb.asm.tree.InsnList;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class NotNullAnnotations
        implements RecordBuilderEnhancer
{
    public static final String DEFAULT_EXPRESSION = "(notnull)|(nonnull)";

    @Override
    public InsnList enhance(Processor processor, TypeElement element, List<String> arguments) {
        InsnList insnList = new InsnList();
        if (arguments.size() > 1) {
            processor.logError("Too many arguments to NotNullAnnotations.");
        } else {
            String expression = arguments.isEmpty() ? DEFAULT_EXPRESSION : arguments.get(0);
            try {
                Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
                processor.asEntries(element)
                        .stream()
                        .filter(entry -> !entry.erasedType().getKind().isPrimitive())
                        .filter(entry -> hasMatchingAnnotation(entry, pattern))
                        .forEach(entry -> RequireNonNull.enhance(insnList, entry));
            } catch (PatternSyntaxException e) {
                processor.logError("Bad argument to NotNullAnnotations: " + e.getMessage());
            }
        }
        return insnList;
    }

    private boolean hasMatchingAnnotation(Entry entry, Pattern pattern)
    {
        Stream<? extends AnnotationMirror> typeMirrors = entry.element().asType().getAnnotationMirrors().stream();
        Stream<? extends AnnotationMirror> elementMirrors = entry.element().getAnnotationMirrors().stream();
        return Stream.concat(typeMirrors, elementMirrors)
                .anyMatch(mirror -> pattern.matcher(mirror.getAnnotationType().asElement().getSimpleName().toString()).matches());
    }
}
