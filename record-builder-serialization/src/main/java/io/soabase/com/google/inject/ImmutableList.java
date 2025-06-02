package io.soabase.com.google.inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface ImmutableList {
    static <T> List<T> copyOf(T... objects) {
        return Collections.unmodifiableList(Arrays.asList(objects));
    }

    static <T> List<T> of(T... objects) {
        return Collections.unmodifiableList(Arrays.asList(objects));
    }
}
