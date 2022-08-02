package io.soabase.recordbuilder.enhancer.spi;

import java.util.List;

public interface Model {
    interface TypeModel {
        String fullyQualifiedName();

        String simpleName();

        List<RecordModel> getRecordComponents();
    }

    interface AnnotationModel {

    }

    interface RecordModel extends TypeModel {
        String componentName();
    }
}
