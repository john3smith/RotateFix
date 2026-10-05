package com.local.rotatefix;

import android.graphics.drawable.Drawable;

final class AppEntry {
    final String label;
    final String packageName;
    final Drawable icon;

    AppEntry(String label, String packageName, Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }
}
