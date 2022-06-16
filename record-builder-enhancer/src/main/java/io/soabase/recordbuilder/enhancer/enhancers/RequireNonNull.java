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

import io.soabase.recordbuilder.enhancer.spi.Entry;
import io.soabase.recordbuilder.enhancer.spi.Processor;
import io.soabase.recordbuilder.enhancer.spi.RecordBuilderEnhancer;
import recordbuilder.org.objectweb.asm.Opcodes;
import recordbuilder.org.objectweb.asm.tree.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static io.soabase.recordbuilder.enhancer.enhancers.ProcessorUtil.isNotHandledByOthers;

public class RequireNonNull
        implements RecordBuilderEnhancer {
    @Override
    public InsnList enhance(Processor processor, TypeElement element, List<String> arguments) {
        TypeMirror optionalType = processor.elements().getTypeElement("java.util.Optional").asType();
        TypeMirror stringType = processor.elements().getTypeElement("java.lang.String").asType();
        TypeMirror collectionType = processor.elements().getTypeElement("java.util.Collection").asType();
        TypeMirror listType = processor.elements().getTypeElement("java.util.List").asType();
        TypeMirror mapType = processor.elements().getTypeElement("java.util.Map").asType();

        InsnList insnList = new InsnList();
        processor.asEntries(element)
                .stream()
                .filter(entry -> !entry.erasedType().getKind().isPrimitive())
                .filter(entry -> isNotHandledByOthers(EmptyNullOptional.class, processor, entry, optionalType))
                .filter(entry -> isNotHandledByOthers(EmptyNullString.class, processor, entry, stringType))
                .filter(entry -> isNotHandledByOthers(CopyCollectionNullableEmpty.class, processor, entry, collectionType, listType, mapType))
                .filter(entry -> isNotHandledByOthers(CopyCollection.class, processor, entry, collectionType, listType, mapType))
                .filter(entry -> isNotHandledByOthers(GuavaCopyCollectionNullableEmpty.class, processor, entry, collectionType, listType, mapType))
                .filter(entry -> isNotHandledByOthers(GuavaCopyCollection.class, processor, entry, collectionType, listType, mapType))
                .forEach(entry -> enhance(insnList, entry));
        return insnList;
    }

    static void enhance(InsnList insnList, Entry entry) {
        // java.util.Objects.requireNonNull(var1, "<name> is null");
                    /*
                        ALOAD 1
                        LDC "X is null"
                        INVOKESTATIC java/util/Objects.requireNonNull (Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
                        POP
                    */
        insnList.add(new VarInsnNode(Opcodes.ALOAD, entry.parameterIndex()));
        insnList.add(new LdcInsnNode("%s is null".formatted(entry.element().getSimpleName())));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"));
        insnList.add(new InsnNode(Opcodes.POP));
    }
}
