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
package io.soabase.recordbuilder.serialization.token;

import io.soabase.recordbuilder.serialization.token.MetaToken.StreamEndToken;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Supplier;

public final class TokenStreams {
    private TokenStreams() {
    }

    public static TokenStream of(Supplier<Token> supplier) {
        return new TokenStream(supplier);
    }

    public static TokenStream of(Token... tokens) {
        return of(Arrays.asList(tokens));
    }

    public static TokenStream of(Iterable<Token> elements) {
        Iterator<Token> iterator = elements.iterator();
        Supplier<Token> elementSupplier = () -> iterator.hasNext() ? iterator.next() : StreamEndToken.INSTANCE;
        return new TokenStream(elementSupplier);
    }
}
