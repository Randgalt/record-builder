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

import io.soabase.recordbuilder.enhancer.spi.Processor;
import io.soabase.recordbuilder.enhancer.spi.RecordBuilderEnhancer;
import recordbuilder.org.objectweb.asm.Label;
import recordbuilder.org.objectweb.asm.Opcodes;
import recordbuilder.org.objectweb.asm.tree.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public abstract class CopyCollectionNullableEmptyBase
        implements RecordBuilderEnhancer {
    @Override
    public InsnList enhance(Processor processor, TypeElement element, List<String> arguments) {
        TypeMirror collectionType = processor.elements().getTypeElement("java.util.Collection").asType();
        TypeMirror listType = processor.elements().getTypeElement("java.util.List").asType();
        TypeMirror mapType = processor.elements().getTypeElement("java.util.Map").asType();

        InsnList insnList = new InsnList();
        processor.asEntries(element).stream()
                .filter(entry -> processor.types().isAssignable(entry.erasedType(), collectionType) || processor.types().isAssignable(entry.erasedType(), mapType))
                .forEach(entry -> {
                    /*
                       4: aload_1
                       5: ifnull        15
                       8: aload_1
                       9: invokestatic  #7                  // InterfaceMethod java/util/Set.copyOf:(Ljava/util/Collection;)Ljava/util/Set;
                      12: goto          18
                      15: invokestatic  #13                 // InterfaceMethod java/util/Set.of:()Ljava/util/Set;
                      18: astore_1
                     */
                    LabelNode doEmptylabel = new LabelNode(new Label());
                    LabelNode doCopylabel = new LabelNode(new Label());
                    LabelNode doAssignlabel = new LabelNode(new Label());
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    insnList.add(new JumpInsnNode(Opcodes.IFNULL, doEmptylabel));
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    if (processor.types().isAssignable(entry.erasedType(), listType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, list(), "copyOf", listCopyMethod(), isInterface()));
                    } else if (processor.types().isAssignable(entry.erasedType(), collectionType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, set(), "copyOf", setCopyMethod(), isInterface()));
                    } else {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, map(), "copyOf", mapCopyMethod(), isInterface()));
                    }
                    insnList.add(new JumpInsnNode(Opcodes.GOTO, doAssignlabel));
                    insnList.add(doEmptylabel);
                    if (processor.types().isAssignable(entry.erasedType(), listType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, list(), "of", listEmptyMethod(), isInterface()));
                    } else if (processor.types().isAssignable(entry.erasedType(), collectionType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, set(), "of", setEmptyMethod(), isInterface()));
                    } else {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, map(), "of", mapEmptyMethod(), isInterface()));
                    }
                    insnList.add(doAssignlabel);
                    insnList.add(new VarInsnNode(Opcodes.ASTORE, entry.parameterIndex()));
                });
        return insnList;
    }

    abstract protected boolean isInterface();

    abstract protected String mapEmptyMethod();

    abstract protected String setEmptyMethod();

    abstract protected String listEmptyMethod();

    abstract protected String mapCopyMethod();

    abstract protected String setCopyMethod();

    abstract protected String listCopyMethod();

    abstract protected String map();

    abstract protected String set();

    abstract protected String list();
}
