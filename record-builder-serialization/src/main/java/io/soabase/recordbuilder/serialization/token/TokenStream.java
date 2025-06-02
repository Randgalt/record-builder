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

import io.soabase.recordbuilder.serialization.token.MetaToken.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class TokenStream {
    private final Supplier<Token> supplier;
    private final List<State> stateStack = new ArrayList<>();
    private Token current;

    private enum State {
        ROOT, VALUE, VALUE_OR_ARRAY_END, VALUE_OR_SEPARATOR_OR_ARRAY_END, VALUE_OR_OBJECT_END,
        VALUE_OR_SEPARATOR_OR_OBJECT_END,
    }

    public TokenStream(Supplier<Token> supplier) {
        this.supplier = supplier;
        pushState(State.ROOT);
        current = supplier.get();
    }

    public void singleValue() {
        pushState(State.VALUE);
    }

    public void object() {
        if (!current.equals(ObjectStartToken.INSTANCE)) {
            // TODO
            throw new NoSuchElementException("Expected ObjectStartElement, but found: " + current);
        }
        pushState(State.VALUE_OR_OBJECT_END);
        advance();
    }

    public void array() {
        if (!current.equals(ArrayStartToken.INSTANCE)) {
            // TODO
            throw new NoSuchElementException("Expected ObjectStartElement, but found: " + current);
        }
        pushState(State.VALUE_OR_ARRAY_END);
        advance();
    }

    public Token current() {
        return current;
    }

    public boolean hasMore() {
        if (currentState() == State.ROOT) {
            popState();
            return false;
        }
        return true;
    }

    public void advance() {
        current = supplier.get();

        switch (currentState()) {
        case ROOT -> throw new NoSuchElementException(); // TODO

        case VALUE -> replaceState(State.ROOT);

        case VALUE_OR_ARRAY_END -> {
            if (current.equals(ArrayEndToken.INSTANCE)) {
                replaceState(State.ROOT);
            } else if (current.equals(SeparatorToken.INSTANCE) || current.equals(StreamEndToken.INSTANCE)) {
                // TODO
                throw new NoSuchElementException("Expected value or array end, but found separator: " + current);
            } else {
                replaceState(State.VALUE_OR_SEPARATOR_OR_ARRAY_END);
            }
        }

        case VALUE_OR_SEPARATOR_OR_ARRAY_END -> {
            if (current.equals(ArrayEndToken.INSTANCE)) {
                replaceState(State.ROOT);
            } else if (current.equals(StreamEndToken.INSTANCE)) {
                // TODO
                throw new NoSuchElementException("Expected value or array end, but found separator: " + current);
            } else if (current.equals(SeparatorToken.INSTANCE)) {
                current = supplier.get();
            }
        }

        case VALUE_OR_OBJECT_END -> {
            if (current.equals(ObjectEndToken.INSTANCE)) {
                replaceState(State.ROOT);
            } else if (current.equals(SeparatorToken.INSTANCE) || current.equals(StreamEndToken.INSTANCE)) {
                // TODO
                throw new NoSuchElementException("Expected value or object end, but found separator: " + current);
            } else {
                replaceState(State.VALUE_OR_SEPARATOR_OR_OBJECT_END);
            }
        }

        case VALUE_OR_SEPARATOR_OR_OBJECT_END -> {
            if (current.equals(ObjectEndToken.INSTANCE)) {
                replaceState(State.ROOT);
            } else if (current.equals(StreamEndToken.INSTANCE)) {
                // TODO
                throw new NoSuchElementException("Expected value or object end, but found separator: " + current);
            } else if (current.equals(SeparatorToken.INSTANCE)) {
                current = supplier.get();
            }
        }
        }
    }

    private State currentState() {
        return stateStack.getLast();
    }

    private void pushState(State state) {
        stateStack.add(state);
    }

    private void replaceState(State state) {
        stateStack.set(stateStack.size() - 1, state);
    }

    private void popState() {
        stateStack.removeLast();
    }
}
