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
import recordbuilder.org.objectweb.asm.Opcodes;
import recordbuilder.org.objectweb.asm.tree.InsnList;
import recordbuilder.org.objectweb.asm.tree.MethodInsnNode;
import recordbuilder.org.objectweb.asm.tree.VarInsnNode;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public abstract class CopyCollectionBase
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
                    // aload_1
                    // invokestatic  #7                  // InterfaceMethod java/util/List.copyOf:(Ljava/util/Collection;)Ljava/util/Set;
                    // astore_1
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    if (processor.types().isAssignable(entry.erasedType(), listType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, list(), "copyOf", listMethod(), isInterface()));
                    } else if (processor.types().isAssignable(entry.erasedType(), collectionType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, set(), "copyOf", setMethod(), isInterface()));
                    } else {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, map(), "copyOf", mapMethod(), isInterface()));
                    }
                    insnList.add(new VarInsnNode(Opcodes.ASTORE, entry.parameterIndex()));
                });
        return insnList;
    }

    abstract protected boolean isInterface();

    abstract protected String mapMethod();

    abstract protected String setMethod();

    abstract protected String listMethod();

    abstract protected String map();

    abstract protected String set();

    abstract protected String list();
}
