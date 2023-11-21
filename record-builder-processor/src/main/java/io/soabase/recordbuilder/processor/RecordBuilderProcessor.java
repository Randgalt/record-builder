/*
 * Copyright 2019 The original author or authors
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
package io.soabase.recordbuilder.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.soabase.recordbuilder.core.RecordBuilderGenerated;
import io.soabase.recordbuilder.core.RecordInterface;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class RecordBuilderProcessor extends AbstractProcessor {
    private static final String RECORD_BUILDER = RecordBuilder.class.getName();
    private static final String RECORD_BUILDER_INCLUDE = RecordBuilder.Include.class.getName().replace('$', '.');
    private static final String RECORD_INTERFACE = RecordInterface.class.getName();
    private static final String RECORD_INTERFACE_INCLUDE = RecordInterface.Include.class.getName().replace('$', '.');

    static final AnnotationSpec generatedRecordBuilderAnnotation = AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", RecordBuilder.class.getName()).build();
    static final AnnotationSpec generatedRecordInterfaceAnnotation = AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", RecordInterface.class.getName()).build();
    static final AnnotationSpec recordBuilderGeneratedAnnotation = AnnotationSpec.builder(RecordBuilderGenerated.class)
            .build();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation)
                .forEach(element -> process(annotation, element)));
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // we don't directly return RELEASE_14 as that may
        // not exist in prior releases
        // if we're running on an older release, returning latest()
        // is fine as we won't encounter any records anyway
        return SourceVersion.latest();
    }

    private void process(TypeElement annotation, Element element) {
        String annotationClass = annotation.getQualifiedName().toString();
        if (annotationClass.equals(RECORD_BUILDER)) {
            var typeElement = (TypeElement) element;
            processRecordBuilder(typeElement, getMetaData(typeElement), Optional.empty());
        } else if (annotationClass.equals(RECORD_INTERFACE)) {
            var typeElement = (TypeElement) element;
            processRecordInterface(typeElement, element.getAnnotation(RecordInterface.class).addRecordBuilder(),
                    getMetaData(typeElement), Optional.empty(), false);
        } else if (annotationClass.equals(RECORD_BUILDER_INCLUDE) || annotationClass.equals(RECORD_INTERFACE_INCLUDE)) {
            processIncludes(element, getMetaData(element), annotationClass);
        } else {
            var recordBuilderTemplate = annotation.getAnnotation(RecordBuilder.Template.class);
            if (recordBuilderTemplate != null) {
                if (recordBuilderTemplate.asRecordInterface()) {
                    processRecordInterface((TypeElement) element, true, recordBuilderTemplate.options(),
                            Optional.empty(), true);
                } else {
                    processRecordBuilder((TypeElement) element, recordBuilderTemplate.options(), Optional.empty());
                }
            }
        }
    }

    private RecordBuilder.Options getMetaData(Element element) {
        var recordSpecificMetaData = element.getAnnotation(RecordBuilder.Options.class);
        return (recordSpecificMetaData != null) ? recordSpecificMetaData
                : RecordBuilderOptions.build(processingEnv.getOptions());
    }

    private void processIncludes(Element element, RecordBuilder.Options metaData, String annotationClass) {
        var isRecordBuilderInclude = annotationClass.equals(RECORD_BUILDER_INCLUDE);
        var annotationMirrorOpt = ElementUtils.findAnnotationMirror(processingEnv, element, annotationClass);
        if (annotationMirrorOpt.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Could not get annotation mirror for: " + annotationClass, element);
        } else {
            var includeHelper = new IncludeHelper(processingEnv, element, annotationMirrorOpt.get(),
                    isRecordBuilderInclude);
            if (includeHelper.isValid()) {
                var packagePattern = ElementUtils.getStringAttribute(ElementUtils
                        .getAnnotationValue(includeHelper.getAnnotationValues(), "packagePattern").orElse(null), "*");
                for (var typeElement : includeHelper.getClassTypeElements()) {
                    var packageName = buildPackageName(packagePattern, element, typeElement);
                    if (packageName != null) {
                        if (isRecordBuilderInclude) {
                            processRecordBuilder(typeElement, metaData, Optional.of(packageName));
                        } else {
                            var addRecordBuilderOpt = ElementUtils
                                    .getAnnotationValue(includeHelper.getAnnotationValues(), "addRecordBuilder");
                            var addRecordBuilder = addRecordBuilderOpt.map(ElementUtils::getBooleanAttribute)
                                    .orElse(true);
                            processRecordInterface(typeElement, addRecordBuilder, metaData, Optional.of(packageName),
                                    false);
                        }
                    }
                }
            }
        }
    }

    private String buildPackageName(String packagePattern, Element builderElement, TypeElement includedClass) {
        PackageElement includedClassPackage = findPackageElement(includedClass, includedClass);
        if (includedClassPackage == null) {
            return null;
        }
        String replaced = packagePattern.replace("*", includedClassPackage.getQualifiedName().toString());
        if (builderElement instanceof PackageElement) {
            return replaced.replace("@", ((PackageElement) builderElement).getQualifiedName().toString());
        }
        return replaced.replace("@",
                ((PackageElement) builderElement.getEnclosingElement()).getQualifiedName().toString());
    }

    private PackageElement findPackageElement(Element actualElement, Element includedClass) {
        if (includedClass == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element has not package", actualElement);
            return null;
        }
        if (includedClass.getEnclosingElement() instanceof PackageElement) {
            return (PackageElement) includedClass.getEnclosingElement();
        }
        return findPackageElement(actualElement, includedClass.getEnclosingElement());
    }

    private void processRecordInterface(TypeElement element, boolean addRecordBuilder, RecordBuilder.Options metaData,
            Optional<String> packageName, boolean fromTemplate) {
        if (!element.getKind().isInterface()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "RecordInterface only valid for interfaces.", element);
            return;
        }

        validateMetaData(metaData, element);

        var internalProcessor = new InternalRecordInterfaceProcessor(processingEnv, element, addRecordBuilder, metaData,
                packageName, fromTemplate);
        if (!internalProcessor.isValid()) {
            return;
        }
        writeRecordInterfaceJavaFile(element, internalProcessor.packageName(), internalProcessor.recordClassType(),
                internalProcessor.recordType(), metaData, internalProcessor::toRecord);
    }

    private void processRecordBuilder(TypeElement record, RecordBuilder.Options metaData,
            Optional<String> packageName) {
        // we use string based name comparison for the element kind,
        // as the ElementKind.RECORD enum doesn't exist on JRE releases
        // older than Java 14, and we don't want to throw unexpected
        // NoSuchFieldErrors
        if (!"RECORD".equals(record.getKind().name())) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "RecordBuilder only valid for records.",
                    record);
            return;
        }

        validateMetaData(metaData, record);

        var internalProcessor = new InternalRecordBuilderProcessor(processingEnv, record, metaData, packageName);
        writeRecordBuilderJavaFile(record, internalProcessor.packageName(), internalProcessor.builderClassType(),
                internalProcessor.builderType(), metaData);
    }

    private void validateMetaData(RecordBuilder.Options metaData, Element record) {
        var useImmutableCollections = metaData.useImmutableCollections();
        var useUnmodifiableCollections = metaData.useUnmodifiableCollections();
        var allowNullableCollections = metaData.allowNullableCollections();

        if (useImmutableCollections && useUnmodifiableCollections) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                    "Options.useUnmodifiableCollections property is ignored as Options.useImmutableCollections is set to true",
                    record);
        } else if (!useImmutableCollections && !useUnmodifiableCollections && allowNullableCollections) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                    "Options.allowNullableCollections property will have no effect as Options.useImmutableCollections and Options.useUnmodifiableCollections are set to false",
                    record);
        }
    }

    private void writeRecordBuilderJavaFile(TypeElement record, String packageName, ClassType builderClassType,
            TypeSpec builderType, RecordBuilder.Options metaData) {
        // produces the Java file
        JavaFile javaFile = javaFileBuilder(packageName, builderType, metaData);
        Filer filer = processingEnv.getFiler();
        try {
            String fullyQualifiedName = packageName.isEmpty() ? builderClassType.name()
                    : (packageName + "." + builderClassType.name());
            JavaFileObject sourceFile = filer.createSourceFile(fullyQualifiedName);
            try (Writer writer = sourceFile.openWriter()) {
                javaFile.writeTo(writer);
            }
        } catch (IOException e) {
            handleWriteError(record, e);
        }
    }

    private void writeRecordInterfaceJavaFile(TypeElement element, String packageName, ClassType classType,
            TypeSpec type, RecordBuilder.Options metaData, Function<String, String> toRecordProc) {
        JavaFile javaFile = javaFileBuilder(packageName, type, metaData);

        String classSourceCode = javaFile.toString();
        int generatedIndex = classSourceCode.indexOf("@Generated");
        String recordSourceCode = toRecordProc.apply(classSourceCode);

        Filer filer = processingEnv.getFiler();
        try {
            String fullyQualifiedName = packageName.isEmpty() ? classType.name()
                    : (packageName + "." + classType.name());
            JavaFileObject sourceFile = filer.createSourceFile(fullyQualifiedName);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(recordSourceCode);
            }
        } catch (IOException e) {
            handleWriteError(element, e);
        }
    }

    private JavaFile javaFileBuilder(String packageName, TypeSpec type, RecordBuilder.Options metaData) {
        var javaFileBuilder = JavaFile.builder(packageName, type).skipJavaLangImports(true)
                .indent(metaData.fileIndent());
        var comment = metaData.fileComment();
        if ((comment != null) && !comment.isEmpty()) {
            javaFileBuilder.addFileComment(comment);
        }
        return javaFileBuilder.build();
    }

    private void handleWriteError(TypeElement element, IOException e) {
        String message = "Could not create source file";
        if (e.getMessage() != null) {
            message = message + ": " + e.getMessage();
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
