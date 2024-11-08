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
package io.soabase.recordbuilder.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.lang.reflect.AnnotatedElement;

class TestAnnotated {
    @Test
    void testInheritComponentAnnotationsFalse() throws NoSuchMethodException {
        var method = IgnoreAnnotatedBuilder.class.getMethod("s");
        Assertions.assertNull(method.getAnnotation(NotNull.class));
    }

    @Test
    void testFiltersOutJavaXValid() throws NoSuchMethodException {
        var method = RequestWithValidBuilder.With.class.getMethod("part");
        Assertions.assertNull(method.getAnnotation(Valid.class));
    }

    @Test
    void testFiltersOutJakartaValid() throws NoSuchMethodException {
        var method = RequestWithValidBuilder.With.class.getMethod("part");
        Assertions.assertNull(method.getAnnotation(jakarta.validation.Valid.class));
    }

    @Test
    void testStaticConstructor() throws NoSuchMethodException {
        var method = AnnotatedBuilder.class.getMethod("Annotated", String.class, Integer.TYPE, Double.TYPE);
        var parameters = method.getParameters();
        Assertions.assertEquals(3, parameters.length);
        assertHey(parameters[0]);
        assertI(parameters[1]);
        assertD(parameters[2]);
    }

    @Test
    void testSetters() throws NoSuchMethodException {
        var method = AnnotatedBuilder.class.getMethod("hey", String.class);
        var parameters = method.getParameters();
        Assertions.assertEquals(1, parameters.length);
        assertHey(parameters[0]);

        method = AnnotatedBuilder.class.getMethod("i", Integer.TYPE);
        parameters = method.getParameters();
        Assertions.assertEquals(1, parameters.length);
        assertI(parameters[0]);

        method = AnnotatedBuilder.class.getMethod("d", Double.TYPE);
        parameters = method.getParameters();
        Assertions.assertEquals(1, parameters.length);
        assertD(parameters[0]);
    }

    @Test
    void testGetters() throws NoSuchMethodException {
        var method = AnnotatedBuilder.class.getMethod("hey");
        assertHey(method);

        method = AnnotatedBuilder.class.getMethod("i");
        assertI(method);

        method = AnnotatedBuilder.class.getMethod("d");
        assertD(method);
    }

    @Test
    void testWitherSetters() throws NoSuchMethodException {
        var method = AnnotatedBuilder.With.class.getMethod("withHey", String.class);
        var parameters = method.getParameters();
        Assertions.assertEquals(1, parameters.length);
        assertHey(parameters[0]);

        method = AnnotatedBuilder.With.class.getMethod("withI", Integer.TYPE);
        parameters = method.getParameters();
        Assertions.assertEquals(1, parameters.length);
        assertI(parameters[0]);

        method = AnnotatedBuilder.With.class.getMethod("withD", Double.TYPE);
        parameters = method.getParameters();
        Assertions.assertEquals(1, parameters.length);
        assertD(parameters[0]);
    }

    private void assertD(AnnotatedElement d) {
        Assertions.assertEquals(0, d.getAnnotations().length);
    }

    private void assertI(AnnotatedElement i) {
        Assertions.assertNotNull(i.getAnnotation(Min.class));
        Assertions.assertEquals(i.getAnnotation(Min.class).value(), 10);
        Assertions.assertNotNull(i.getAnnotation(Max.class));
        Assertions.assertEquals(i.getAnnotation(Max.class).value(), 100);
    }

    private void assertHey(AnnotatedElement hey) {
        Assertions.assertNotNull(hey.getAnnotation(NotNull.class));
        Assertions.assertNotNull(hey.getAnnotation(Null.class));
    }
}
