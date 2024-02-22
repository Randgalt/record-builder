package io.soabase.recordbuilder.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.recordbuilder.processor.options.InternalWitherOptions;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

record BuilderContext(ClassType recordClassType, ProcessingEnvironment processingEnv, TypeElement record,
                      InternalWitherOptions metaData,
                      Optional<String> packageNameOpt, List<TypeVariableName> typeVariables,
                      List<RecordClassType> recordComponents) {
    void addAccessorAnnotations(RecordClassType component, MethodSpec.Builder methodSpecBuilder,
                                Predicate<AnnotationSpec> additionalFilter) {
/*
        if (metaData.inheritComponentAnnotations()) {
            component.getAccessorAnnotations().stream().map(AnnotationSpec::get).filter(this::filterOutOverride)
                    .filter(additionalFilter).forEach(methodSpecBuilder::addAnnotation);
        }
*/

        Foo f = Foo.A;
    }

    public enum Foo
    {A, B}
}
