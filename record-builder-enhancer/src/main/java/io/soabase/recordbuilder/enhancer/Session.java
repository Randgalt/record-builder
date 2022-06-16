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
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import io.soabase.recordbuilder.enhancer.spi.Processor;
import picocli.CommandLine;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class Session implements SessionFlag {
    private final JavacTask task;
    private final StandardJavaFileManager fileManager;
    private final Trees trees;
    private final EnhancersController enhancersController;
    private final PluginOptions pluginOptions;
    private final CommandLine commandLine;

    public Session(JavacTask task, TaskListener taskListener, PluginOptions pluginOptions, CommandLine commandLine) {
        this.task = task;
        this.pluginOptions = pluginOptions;
        this.commandLine = commandLine;
        task.addTaskListener(taskListener);
        enhancersController = new EnhancersController();
        //PluginOptions.setupCommandLine(this.commandLine, enhancersController.enhancers(), true);
        fileManager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(new File(this.pluginOptions.directory).getAbsoluteFile()));
        } catch (IOException e) {
            throw new RuntimeException("Could not set the class output path", e);
        }

        trees = Trees.instance(task);

        if (pluginOptions.outputTo != null) {
            if (!pluginOptions.outputTo.isDirectory() && !pluginOptions.outputTo.exists() && !pluginOptions.outputTo.mkdirs()) {
                throw new RuntimeException("Could not create directory: " + pluginOptions.outputTo);
            }
        }
    }

    public boolean enabled()
    {
        return !pluginOptions.disable;
    }

    public boolean isDryRun()
    {
        return pluginOptions.dryRun;
    }

    public JavacTask task() {
        return task;
    }

    public interface FileStreams
    {
        InputStream openInputStream() throws IOException;

        OutputStream openOutputStream() throws IOException;
    }

    public Optional<FileStreams> getFileStreams(Processor processor, TypeElement element, URI fileUri) {
        String directory = pluginOptions.directory;
        if (directory.isEmpty()) {
            String pathToFile = fileUri.getPath();
            int srcMainJavaIndex = pathToFile.indexOf("src/main/java");
            if (srcMainJavaIndex >= 0) {
                directory = pathToFile.substring(0, srcMainJavaIndex) + "target/classes";
            }
            else {
                directory = ".";
            }
        }
        String className = getClassName(element);
        File absoluteDirectory = new File(directory).getAbsoluteFile();
        if (pluginOptions.verbose) {
            processor.logWarning("Using classPath %s for class %s".formatted(absoluteDirectory, className));
        }
        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(absoluteDirectory));
            JavaFileObject inputJavaFile = fileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT, className, JavaFileObject.Kind.CLASS, null);
            JavaFileObject outputJavaFile;
            if (pluginOptions.outputTo != null) {
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(pluginOptions.outputTo));
                outputJavaFile = fileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT, className, JavaFileObject.Kind.CLASS, null);
            }
            else {
                outputJavaFile = inputJavaFile;
            }
            FileStreams fileStreams = new FileStreams() {
                @Override
                public InputStream openInputStream() throws IOException {
                    return inputJavaFile.openInputStream();
                }

                @Override
                public OutputStream openOutputStream() throws IOException {
                    return outputJavaFile.openOutputStream();
                }
            };
            return Optional.of(fileStreams);
        } catch (IOException e) {
            processor.logError("Could not set classpath to %s for class %s: %s".formatted(absoluteDirectory, className, e.getMessage()));
            return Optional.empty();
        }
    }

    public EnhancersController enhancersController() {
        return enhancersController;
    }

    public ProcessorImpl newProcessor(TaskEvent taskEvent)
    {
        return new ProcessorImpl(task.getElements(), task.getTypes(), trees, taskEvent.getCompilationUnit(), pluginOptions.verbose);
    }

    public void checkPrintHelp(Processor processor)
    {
        if (pluginOptions.helpRequested && getAndSetFlag("show-help")) {
            StringWriter help = new StringWriter();
            help.write('\n');
            commandLine.usage(new PrintWriter(help));
            processor.logError(help.toString());
        }
    }

    @Override
    public boolean getAndSetFlag(String name)
    {
        boolean wasSet = isSet(name);
        if (!wasSet) {
            set(name);
            return true;
        }
        return false;
    }

    private String getClassName(TypeElement typeElement) {
        return task.getElements().getPackageOf(typeElement).getQualifiedName() + "." + getClassName(typeElement, "");
    }

    private static String getClassName(Element element, String separator) {
        // prefix enclosing class names if nested in a class
        if (element instanceof TypeElement) {
            return getClassName(element.getEnclosingElement(), "$") + element.getSimpleName().toString() + separator;
        }
        return "";
    }

    private boolean isSet(String name)
    {
        return Boolean.parseBoolean(System.getProperty(toKey(name)));
    }

    private void set(String name)
    {
        System.setProperty(toKey(name), "true");
    }

    private String toKey(String name) {
        return Session.class.getName() + ":" + name;
    }
}
