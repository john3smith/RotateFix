package com.local.rotatefix;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Set;

public final class RotationAccessibilityService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final long EVALUATION_DELAY_MS = 120L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable evaluateForegroundApp = this::evaluateForegroundApp;

    private SelectedAppsStore selectedAppsStore;
    private SharedPreferences preferences;
    private OrientationOverlayController overlayController;
    private Set<String> selectedPackages;
    private String latestEventPackage;
    private String activeTargetPackage;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        selectedAppsStore = new SelectedAppsStore(this);
        selectedPackages = selectedAppsStore.load();
        preferences = getSharedPreferences(SelectedAppsStore.PREFERENCES_NAME, MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        overlayController = new OrientationOverlayController(this);
        scheduleEvaluation();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        CharSequence packageName = event.getPackageName();
        if (packageName != null) {
            latestEventPackage = packageName.toString();
        }
        scheduleEvaluation();
    }

    @Override
    public void onInterrupt() {
        clearPortraitRequest();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SelectedAppsStore.KEY_SELECTED_PACKAGES.equals(key)) {
            selectedPackages = selectedAppsStore.load();
            scheduleEvaluation();
        }
    }

    private void scheduleEvaluation() {
        handler.removeCallbacks(evaluateForegroundApp);
        handler.postDelayed(evaluateForegroundApp, EVALUATION_DELAY_MS);
    }

    private void evaluateForegroundApp() {
        if (overlayController == null || selectedPackages == null) {
            return;
        }

        String foregroundPackage = packageFromActiveWindow();
        if (foregroundPackage == null || foregroundPackage.isEmpty()) {
            foregroundPackage = latestEventPackage;
        }
        if (foregroundPackage == null || foregroundPackage.isEmpty()) {
            return;
        }

        if (RotationPolicy.shouldForcePortrait(foregroundPackage, selectedPackages)) {
            overlayController.requestPortrait();
            activeTargetPackage = foregroundPackage;
            return;
        }

        String inputMethodPackage = getDefaultInputMethodPackage();
        if (overlayController.isActive()
                && RotationPolicy.isTransientSystemUi(foregroundPackage, inputMethodPackage)) {
            return;
        }
        clearPortraitRequest();
    }

    private String packageFromActiveWindow() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) {
            return null;
        }
        return root.getPackageName().toString();
    }

    private String getDefaultInputMethodPackage() {
        String component = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (component == null || component.isEmpty()) {
            return "";
        }
        int separator = component.indexOf('/');
        return separator > 0 ? component.substring(0, separator) : component;
    }

    private void clearPortraitRequest() {
        if (overlayController != null) {
            overlayController.clearRequest();
        }
        activeTargetPackage = null;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        disposeOverlay();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        disposeOverlay();
        super.onDestroy();
    }

    private void disposeOverlay() {
        if (overlayController != null) {
            overlayController.dispose();
        }
        activeTargetPackage = null;
    }
}
