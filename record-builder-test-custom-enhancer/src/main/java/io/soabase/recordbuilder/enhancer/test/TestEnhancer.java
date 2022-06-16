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
package io.soabase.recordbuilder.enhancer.test;

import io.soabase.recordbuilder.enhancer.spi.Processor;
import io.soabase.recordbuilder.enhancer.spi.RecordBuilderEnhancer;
import recordbuilder.org.objectweb.asm.Opcodes;
import recordbuilder.org.objectweb.asm.tree.FieldInsnNode;
import recordbuilder.org.objectweb.asm.tree.InsnList;
import recordbuilder.org.objectweb.asm.tree.InsnNode;
import recordbuilder.org.objectweb.asm.tree.MethodInsnNode;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class TestEnhancer
    implements RecordBuilderEnhancer
{
    @Override
    public InsnList enhance(Processor processor, TypeElement element, List<String> arguments)
    {
        /*
           4: getstatic     #7                  // Field io/soabase/recordbuilder/enhancer/test/Counter.COUNTER:Ljava/util/concurrent/atomic/AtomicInteger;
           7: invokevirtual #13                 // Method java/util/concurrent/atomic/AtomicInteger.incrementAndGet:()I
          10: pop
         */
        InsnList insnList = new InsnList();
        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "io/soabase/recordbuilder/enhancer/test/Counter", "COUNTER", "Ljava/util/concurrent/atomic/AtomicInteger;"));
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "incrementAndGet", "()I"));
        insnList.add(new InsnNode(Opcodes.POP));
        return insnList;
    }

    public String description() {
        return "This is a test";
    }
}
