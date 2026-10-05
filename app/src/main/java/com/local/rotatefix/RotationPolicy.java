package com.local.rotatefix;

import java.util.Set;

final class RotationPolicy {
    private RotationPolicy() {
    }

    static boolean shouldForcePortrait(String packageName, Set<String> selectedPackages) {
        return packageName != null
                && !packageName.trim().isEmpty()
                && selectedPackages.contains(packageName);
    }

    static boolean isTransientSystemUi(String packageName, String inputMethodPackage) {
        if (packageName == null) {
            return false;
        }
        return packageName.equals("com.android.systemui")
                || packageName.equals("android")
                || packageName.equals("com.google.android.permissioncontroller")
                || packageName.equals("com.android.permissioncontroller")
                || packageName.equals(inputMethodPackage);
    }
}
