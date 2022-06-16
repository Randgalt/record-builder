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

public class EmptyNullString
        implements RecordBuilderEnhancer {
    @Override
    public InsnList enhance(Processor processor, TypeElement element, List<String> arguments) {
        TypeMirror stringType = processor.elements().getTypeElement("java.lang.String").asType();
        InsnList insnList = new InsnList();
        processor.asEntries(element)
                .stream()
                .filter(entry -> processor.types().isAssignable(entry.erasedType(), stringType))
                .forEach(entry -> {
                    /*
                       4: aload_1
                       5: ifnull        12
                       8: aload_1
                       9: goto          14
                      12: ldc           #7                  // String
                      14: astore_1
                    */
                    LabelNode doEmptylabel = new LabelNode(new Label());
                    LabelNode doAssignlabel = new LabelNode(new Label());
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    insnList.add(new JumpInsnNode(Opcodes.IFNULL, doEmptylabel));
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    insnList.add(new JumpInsnNode(Opcodes.GOTO, doAssignlabel));
                    insnList.add(doEmptylabel);
                    insnList.add(new LdcInsnNode(""));
                    insnList.add(doAssignlabel);
                    insnList.add(new VarInsnNode(Opcodes.ASTORE, entry.parameterIndex()));
                });
        return insnList;
    }
}
