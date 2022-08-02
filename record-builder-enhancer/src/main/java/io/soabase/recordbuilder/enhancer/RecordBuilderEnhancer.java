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

public class RecordBuilderEnhancer {
    private final Session session;

    public RecordBuilderEnhancer(Session session) {
        this.session = session;
    }

    public void enhance(TypeElement typeElement, ProcessorImpl processor, List<EnhancersController.EnhancerAndArgs> specs, Session.FileStreams fileStreams) {
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

    private static Optional<MethodNode> findConstructor(ClassNode classNode) {
        String defaultConstructorDescription = classNode.recordComponents.stream()
                .map(recordComponentNode -> recordComponentNode.descriptor)
                .collect(Collectors.joining("", "(", ")V"));
        return classNode.methods.stream()
                .filter(methodNode -> methodNode.name.equals("<init>") && methodNode.desc.equals(defaultConstructorDescription))
                .findFirst();
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
}
