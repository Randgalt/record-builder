package io.soabase.com.google.inject;

public class Checks {
    public static <T> T checkNotNull(T reference, String name) {
        if (reference != null) {
            return reference;
        }

        throw new NullPointerException(name);
    }

    public static void checkArgument(boolean test, String str) {
        if (!test) {
            throw new IllegalArgumentException(str);
        }
    }

    public static void checkArgument(boolean test, String format, Object p1) {
        if (!test) {
            throw new IllegalArgumentException(String.format(format, p1));
        }
    }

    public static void checkArgument(boolean test, String format, Object p1, Object p2) {
        if (!test) {
            throw new IllegalArgumentException(String.format(format, p1, p2));
        }
    }

    private Checks() {
    }
}
