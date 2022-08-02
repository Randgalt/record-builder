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

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import io.soabase.recordbuilder.enhancer.EnhancersController.EnhancerAndArgs;
import io.soabase.recordbuilder.enhancer.Session.FileStreams;
import picocli.CommandLine;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.stream.Collectors;

public class RecordBuilderEnhancerPlugin
        implements Plugin, TaskListener {
    private volatile Session session;

    @Override
    public String getName() {
        return "recordbuilderenhancer";
    }

    @Override
    public boolean autoStart() {
        return true;
    }

    @Override
    public void init(JavacTask task, String... args) {
        PluginOptions pluginOptions = new PluginOptions();
        CommandLine commandLine = new CommandLine(pluginOptions);
        commandLine.parseArgs(args);
        if (!pluginOptions.disable) {
            session = new Session(task, this, pluginOptions, commandLine);
        }
    }

    @Override
    public void finished(TaskEvent taskEvent) {
        if (taskEvent.getKind() == TaskEvent.Kind.GENERATE) {
            TypeElement typeElement = taskEvent.getTypeElement();
            ProcessorImpl processor = session.newProcessor(taskEvent);
            session.checkPrintHelp(processor);
            List<EnhancerAndArgs> enhancers = session.enhancersController().getEnhancers(processor, typeElement);
            if (!enhancers.isEmpty()) {
                if (typeElement.getRecordComponents().isEmpty()) {
                    processor.logError(typeElement.getQualifiedName() + " is not a record");
                } else {
                    if (processor.verboseRequested()) {
                        processor.logWarning("Enhancing %s with %s".formatted(typeElement.getSimpleName(), enhancers.stream().map(enhancer -> enhancer.getClass().getName()).collect(Collectors.joining(","))));
                    }
                    session.getFileStreams(processor, typeElement, taskEvent.getCompilationUnit().getSourceFile().toUri()).ifPresent(fileStreams -> enhance(typeElement, processor.withEnhancers(enhancers), enhancers, fileStreams));
                }
            }
        }
    }

    private void enhance(TypeElement typeElement, ProcessorImpl processor, List<EnhancerAndArgs> specs, FileStreams fileStreams) {
        RecordBuilderEnhancer enhancer = new RecordBuilderEnhancer(session);
        enhancer.enhance(typeElement, processor, specs, fileStreams);
    }

}
