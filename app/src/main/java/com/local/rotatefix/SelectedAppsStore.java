package com.local.rotatefix;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class SelectedAppsStore {
    static final String PREFERENCES_NAME = "rotatefix_preferences";
    static final String KEY_SELECTED_PACKAGES = "selected_packages";

    private final SharedPreferences preferences;

    SelectedAppsStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    Set<String> load() {
        Set<String> stored = preferences.getStringSet(KEY_SELECTED_PACKAGES, Collections.emptySet());
        return new HashSet<>(stored == null ? Collections.emptySet() : stored);
    }

    void save(Set<String> packages) {
        preferences.edit().putStringSet(KEY_SELECTED_PACKAGES, new HashSet<>(packages)).apply();
    }
}
