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

import io.soabase.recordbuilder.serialization.json.JsonSerializationSink;
import io.soabase.recordbuilder.serialization.spi.PrettySerializationSink;
import io.soabase.recordbuilder.serialization.spi.SinkWriter;
import io.soabase.recordbuilder.serialization.spi.TokenSerializationSink;
import io.soabase.recordbuilder.serialization.token.MetaToken.*;
import io.soabase.recordbuilder.serialization.token.ValueToken.StringToken;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTokenStream {
    @Test
    public void testBasicStreaming() {
        Map<String, List<List<String>>> map = new HashMap<>();

        TokenStream tokenStream = testingObjectStream();
        for (tokenStream.object(); tokenStream.hasMore(); tokenStream.advance()) {
            String fieldName = tokenStream.current().as(FieldNameToken.class).fieldName();
            tokenStream.advance();

            List<List<String>> outerList = new ArrayList<>();
            map.put(fieldName, outerList);

            for (tokenStream.array(); tokenStream.hasMore(); tokenStream.advance()) {
                List<String> values = new ArrayList<>();
                outerList.add(values);

                for (tokenStream.array(); tokenStream.hasMore(); tokenStream.advance()) {
                    String value = tokenStream.current().as(StringToken.class).value();
                    values.add(value);
                }
            }
        }

        assertThat(map).containsExactly(Map.entry("names", List.of(List.of("a1.1", "a1.2"), List.of("a2.1", "a2.2"))),
                Map.entry("otherNames", List.of(List.of("b1.1", "b1.2"), List.of("b2.1", "b2.2"))));
    }

    @Test
    public void testBasicSerialization() {
        StringBuilder json = new StringBuilder();
        SinkWriter sinkWriter = json::append;
        TokenSerializationSink sink = new TokenSerializationSink(new JsonSerializationSink(sinkWriter));
        // new PrettySerializationSink(sink, sinkWriter);
        testingTokens().forEach(sink);
        System.out.println(json);
    }

    private static List<Token> testingTokens() {
        List<Token> work = new ArrayList<>();

        work.add(ObjectStartToken.INSTANCE);
        work.add(FieldNameToken.of("names"));
        work.add(ArrayStartToken.INSTANCE);
        work.addAll(List.of(ArrayStartToken.INSTANCE, StringToken.of("a1.1"), SeparatorToken.INSTANCE,
                StringToken.of("a1.2"), ArrayEndToken.INSTANCE, SeparatorToken.INSTANCE, ArrayStartToken.INSTANCE,
                StringToken.of("a2.1"), SeparatorToken.INSTANCE, StringToken.of("a2.2"), ArrayEndToken.INSTANCE));
        work.add(ArrayEndToken.INSTANCE);
        work.add(SeparatorToken.INSTANCE);
        work.add(FieldNameToken.of("otherNames"));
        work.add(ArrayStartToken.INSTANCE);
        work.addAll(List.of(ArrayStartToken.INSTANCE, StringToken.of("b1.1"), SeparatorToken.INSTANCE,
                StringToken.of("b1.2"), ArrayEndToken.INSTANCE, SeparatorToken.INSTANCE, ArrayStartToken.INSTANCE,
                StringToken.of("b2.1"), SeparatorToken.INSTANCE, StringToken.of("b2.2"), ArrayEndToken.INSTANCE));
        work.add(ArrayEndToken.INSTANCE);
        work.add(ObjectEndToken.INSTANCE);

        return work;
    }

    private static TokenStream testingObjectStream() {
        return TokenStreams.of(testingTokens());
    }
}
