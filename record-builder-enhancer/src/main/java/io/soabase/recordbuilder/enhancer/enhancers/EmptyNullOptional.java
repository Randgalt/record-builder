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

public class EmptyNullOptional
        implements RecordBuilderEnhancer {
    @Override
    public InsnList enhance(Processor processor, TypeElement element, List<String> arguments) {
        TypeMirror optionalType = processor.elements().getTypeElement("java.util.Optional").asType();
        TypeMirror optionalIntType = processor.elements().getTypeElement("java.util.OptionalInt").asType();
        TypeMirror optionalLongType = processor.elements().getTypeElement("java.util.OptionalLong").asType();
        TypeMirror optionalDouble = processor.elements().getTypeElement("java.util.OptionalDouble").asType();

        InsnList insnList = new InsnList();
        processor.asEntries(element)
                .stream()
                .filter(entry -> processor.types().isAssignable(entry.erasedType(), optionalType) || processor.types().isAssignable(entry.erasedType(), optionalIntType) || processor.types().isAssignable(entry.erasedType(), optionalLongType) || processor.types().isAssignable(entry.erasedType(), optionalDouble))
                .forEach(entry -> {
                    /*
                       4: aload         5
                       6: ifnull        14
                       9: aload         5
                      11: goto          17
                      14: invokestatic  #7                  // Method java/util/Optional.empty:()Ljava/util/Optional;
                      17: astore        5
                    */
                    LabelNode doEmptylabel = new LabelNode(new Label());
                    LabelNode doAssignlabel = new LabelNode(new Label());
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    insnList.add(new JumpInsnNode(Opcodes.IFNULL, doEmptylabel));
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
                    insnList.add(new JumpInsnNode(Opcodes.GOTO, doAssignlabel));
                    insnList.add(doEmptylabel);
                    if (processor.types().isAssignable(entry.erasedType(), optionalIntType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/OptionalInt", "empty", "()Ljava/util/OptionalInt;"));
                    }
                    else if (processor.types().isAssignable(entry.erasedType(), optionalLongType)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/OptionalLong", "empty", "()Ljava/util/OptionalLong;"));
                    }
                    else if (processor.types().isAssignable(entry.erasedType(), optionalDouble)) {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/OptionalDouble", "empty", "()Ljava/util/OptionalDouble;"));
                    }
                    else {
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;"));
                    }
                    insnList.add(doAssignlabel);
                    insnList.add(new VarInsnNode(Opcodes.ASTORE, entry.parameterIndex()));
                });
        return insnList;
    }
}
