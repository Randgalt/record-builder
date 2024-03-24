package io.soabase.recordbuilder.processor;

import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.recordbuilder.core.RecordBuilderDeconstruct;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.soabase.recordbuilder.processor.ElementUtils.getBuilderName;
import static io.soabase.recordbuilder.processor.ProcessorCommon.addVisibility;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;
import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.recordBuilderGeneratedAnnotation;

class InternalRecordOptionProcessor {
    private final ClassType recordClassType;
    private final String packageName;
    private final ClassType builderClassType;
    private final List<TypeVariableName> typeVariables;
    private final List<RecordClassType> recordComponents;
    private final TypeSpec.Builder builder;

    InternalRecordOptionProcessor(ProcessingEnvironment processingEnv, TypeElement record,
                                  RecordBuilderDeconstruct.Options metaData, Optional<String> packageNameOpt) {
        var recordActualPackage = ElementUtils.getPackageName(record);
        recordClassType = ElementUtils.getClassType(record, record.getTypeParameters());
        packageName = packageNameOpt.orElse(recordActualPackage);
        builderClassType = ElementUtils.getClassType(packageName,
                getBuilderName(record, recordClassType, metaData.suffix(), metaData.prefixEnclosingClassNames()), record.getTypeParameters());
        typeVariables = record.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        recordComponents = ProcessorCommon.buildRecordComponents(processingEnv, record);

        builder = TypeSpec.classBuilder(builderClassType.name()).addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(metaData.builderClassModifiers()).addTypeVariables(typeVariables);
        if (metaData.addClassRetainedGenerated()) {
            builder.addAnnotation(recordBuilderGeneratedAnnotation);
        }
        addVisibility(builder, recordActualPackage.equals(packageName), record.getModifiers());
    }

    private void
}
