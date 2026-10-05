package com.local.rotatefix;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AppRepository {
    private AppRepository() {
    }

    static List<AppEntry> loadLaunchableApps(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> activities = packageManager.queryIntentActivities(launcherIntent, 0);
        Map<String, AppEntry> uniqueApps = new LinkedHashMap<>();
        for (ResolveInfo info : activities) {
            String packageName = info.activityInfo.packageName;
            if (context.getPackageName().equals(packageName) || uniqueApps.containsKey(packageName)) {
                continue;
            }
            CharSequence loadedLabel = info.loadLabel(packageManager);
            String label = loadedLabel == null ? packageName : loadedLabel.toString().trim();
            uniqueApps.put(packageName, new AppEntry(label, packageName, info.loadIcon(packageManager)));
        }

        List<AppEntry> result = new ArrayList<>(uniqueApps.values());
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        result.sort((left, right) -> collator.compare(left.label, right.label));
        return result;
    }
}
