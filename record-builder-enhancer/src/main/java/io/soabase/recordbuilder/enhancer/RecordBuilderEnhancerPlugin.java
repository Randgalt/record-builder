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
import recordbuilder.org.objectweb.asm.ClassReader;
import recordbuilder.org.objectweb.asm.ClassWriter;
import recordbuilder.org.objectweb.asm.Opcodes;
import recordbuilder.org.objectweb.asm.tree.*;

import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
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

    private boolean removeAndSaveSuperCall(MethodNode constructor, InsnList insnList) {
        ListIterator<AbstractInsnNode> iterator = constructor.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode node = iterator.next();
            iterator.remove();
            insnList.add(node);
            if ((node.getOpcode() == Opcodes.INVOKESPECIAL) && ((MethodInsnNode) node).owner.equals("java/lang/Record") && ((MethodInsnNode) node).name.equals("<init>")) {
                return true;
            }
        }
        return false;
    }

    private void enhance(TypeElement typeElement, ProcessorImpl processor, List<EnhancerAndArgs> specs, FileStreams fileStreams) {
        try {
            ClassNode classNode = new ClassNode();
            try (InputStream in = fileStreams.openInputStream()) {
                ClassReader classReader = new ClassReader(in);
                classReader.accept(classNode, 0);
            }
            InsnList insnList = new InsnList();
            MethodNode constructor = findConstructor(classNode).orElseThrow(() -> new IllegalStateException("Could not find default constructor"));
            if (!removeAndSaveSuperCall(constructor, insnList)) {
                processor.logError("Unrecognized constructor - missing super() call.");
                return;
            }
            specs.stream()
                    .map(spec -> spec.enhancer().enhance(processor, typeElement, spec.arguments()))
                    .forEach(insnList::add);
            constructor.instructions.insert(insnList);

            ClassWriter classWriter = new ClassWriter(Opcodes.ASM9 | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);

            if (!session.isDryRun()) {
                try (OutputStream out = fileStreams.openOutputStream()) {
                    out.write(classWriter.toByteArray());
                }
            }
        } catch (IOException e) {
            processor.logError("Could not process " + typeElement.getQualifiedName() + " - " + e.getMessage());
        }
    }

    static String adjustedClassName(Class<?> clazz) {
        return clazz.getName().replace('$', '.');
    }

    private static Optional<MethodNode> findConstructor(ClassNode classNode) {
        String defaultConstructorDescription = classNode.recordComponents.stream()
                .map(recordComponentNode -> recordComponentNode.descriptor)
                .collect(Collectors.joining("", "(", ")V"));
        return classNode.methods.stream()
                .filter(methodNode -> methodNode.name.equals("<init>") && methodNode.desc.equals(defaultConstructorDescription))
                .findFirst();
    }
}
