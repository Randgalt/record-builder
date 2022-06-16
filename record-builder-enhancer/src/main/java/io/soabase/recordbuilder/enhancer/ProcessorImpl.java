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
package io.soabase.recordbuilder.enhancer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;
import io.soabase.recordbuilder.enhancer.EnhancersController.EnhancerAndArgs;
import io.soabase.recordbuilder.enhancer.spi.Entry;
import io.soabase.recordbuilder.enhancer.spi.Processor;
import io.soabase.recordbuilder.enhancer.spi.RecordBuilderEnhancer;

import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

class ProcessorImpl
    implements Processor {
    private final Collection<? extends Class<? extends RecordBuilderEnhancer>> enhancers;
    private final Elements elements;
    private final Types types;
    private final Trees trees;
    private final CompilationUnitTree compilationUnit;
    private final boolean verboseRequested;

    ProcessorImpl(Elements elements, Types types, Trees trees, CompilationUnitTree compilationUnit, boolean verboseRequested) {
        this(Set.of(), elements, types, trees, compilationUnit, verboseRequested);
    }

    private ProcessorImpl(Collection<? extends Class<? extends RecordBuilderEnhancer>> enhancers, Elements elements, Types types, Trees trees, CompilationUnitTree compilationUnit, boolean verboseRequested) {
        this.enhancers = enhancers;
        this.elements = elements;
        this.types = types;
        this.trees = trees;
        this.compilationUnit = compilationUnit;
        this.verboseRequested = verboseRequested;
    }

    ProcessorImpl withEnhancers(List<EnhancerAndArgs> enhancers)
    {
        Collection<? extends Class<? extends RecordBuilderEnhancer>> enhancersList = enhancers.stream().map(enhancerAndArgs -> enhancerAndArgs.enhancer().getClass()).toList();
        return new ProcessorImpl(enhancersList, elements, types, trees, compilationUnit, verboseRequested);
    }

    @Override
    public boolean verboseRequested() {
        return verboseRequested;
    }

    @Override
    public List<Entry> asEntries(TypeElement element) {
        List<? extends RecordComponentElement> recordComponents = element.getRecordComponents();
        return IntStream.range(0, recordComponents.size())
                .mapToObj(index -> new Entry(index + 1, recordComponents.get(index), types().erasure(recordComponents.get(index).asType())))
                .toList();
    }

    @Override
    public boolean hasEnhancer(Class<? extends RecordBuilderEnhancer> enhancer) {
        return enhancers.contains(enhancer);
    }

    @Override
    public void logInfo(CharSequence msg) {
        printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public void logWarning(CharSequence msg) {
        printMessage(Diagnostic.Kind.MANDATORY_WARNING, msg);
    }

    @Override
    public void logError(CharSequence msg) {
        msg += " - Use -h for help in your -Xplugin arguments";
        printMessage(Diagnostic.Kind.ERROR, msg);
    }

    @Override
    public Elements elements() {
        return elements;
    }

    @Override
    public Types types() {
        return types;
    }

    private void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        trees.printMessage(kind, "[RecordBuilder Enhancer] " + msg, compilationUnit, compilationUnit);
    }
}
