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
package io.soabase.recordbuilder.enhancer.spi;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public interface Processor {
    Elements elements();

    Types types();

    boolean hasEnhancer(Class<? extends RecordBuilderEnhancer> enhancer);

    boolean verboseRequested();

    void logInfo(CharSequence msg);

    void logWarning(CharSequence msg);

    void logError(CharSequence msg);

    List<Entry> asEntries(TypeElement element);
}
