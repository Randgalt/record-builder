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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class TestNoGeneratedAnnotation {

    @Test
    void assertNoGeneratedAnnotationPresent() throws IOException {
        // given
        Class<NoGeneratedAnnotationBuilder> builderClass = NoGeneratedAnnotationBuilder.class;
        Path path = Paths.get("target/generated-sources/annotations",
                builderClass.getPackageName().replace(".", File.separator), builderClass.getSimpleName() + ".java");

        // when
        String source = Files.readString(path);

        // then expect
        assertThat(source).as("generated source file should not contain @Generated annotation nor import.").isNotEmpty()
                .doesNotContain("@Generated").doesNotContain("import javax.annotation.processing.Generated");
    }
}
