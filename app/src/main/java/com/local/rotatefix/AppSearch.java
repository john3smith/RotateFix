package com.local.rotatefix;

import java.util.Locale;

final class AppSearch {
    private AppSearch() {
    }

    static boolean matches(String label, String packageName, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        return normalize(label).contains(normalizedQuery)
                || normalize(packageName).contains(normalizedQuery);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
