package io.soabase.recordbuilder.core;

import javax.lang.model.element.Modifier;
import java.lang.annotation.*;
import java.util.function.Function;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface RecordBuilderDeconstruct {
    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.TYPE, ElementType.PACKAGE })
    @Inherited
    @interface Options {
        /**
         * The builder class name will be the name of the record (prefixed with any enclosing class) plus this suffix.
         * E.g. if the record name is "Foo", the builder will be named "FooBuilder".
         */
        String suffix() default "Helper";

        /**
         * Return the file indent to use
         */
        String fileIndent() default "    ";

        /**
         * If the record is declared inside another class, the outer class's name will be prefixed to the builder name
         * if this returns true.
         */
        boolean prefixEnclosingClassNames() default true;

        /**
         * If true, any annotations (if applicable) on record components are copied to the builder methods
         *
         * @return true/false
         */
        boolean inheritComponentAnnotations() default true;

        boolean alwaysConvertOptionalToOption() default true;

        /**
         * If true, generated classes are annotated with {@code RecordBuilderGenerated} which has a retention policy of
         * {@code CLASS}. This ensures that analyzers such as Jacoco will ignore the generated class.
         */
        boolean addClassRetainedGenerated() default false;

        /**
         * The name to use for the from-to-wither method
         */
        String fromMethodName() default "from";

        Class<? extends Function<?, ?>>[] mapperClasses() default {};

        /**
         * Any additional {@link javax.lang.model.element.Modifier} you wish to apply to the builder. For example to
         * make the builder public when the record is package protect.
         */
        Modifier[] builderClassModifiers() default {};
    }
}
