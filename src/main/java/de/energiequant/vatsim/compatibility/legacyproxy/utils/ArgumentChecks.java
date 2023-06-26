package de.energiequant.vatsim.compatibility.legacyproxy.utils;

import java.util.Collection;

public class ArgumentChecks {
    private ArgumentChecks() {
        // utility class, hide constructor
    }

    public static void requireData(String what, Object o) {
        if (o == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }

        requireDataOrNull(what, o);
    }

    public static void requireDataOrNull(String what, Object o) {
        if (o instanceof String) {
            String s = (String) o;
            if (s.trim().isEmpty()) {
                throw new IllegalArgumentException(what + " must not be blank");
            }
        } else if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            if (c.isEmpty()) {
                throw new IllegalArgumentException(what + " must not be empty");
            }
        }
    }

    public static void requireAtLeast(String what, int actual, int requiredMinimum) {
        if (actual < requiredMinimum) {
            throw new IllegalArgumentException(what + " must be at least " + requiredMinimum);
        }
    }
}
