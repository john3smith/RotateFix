package com.local.rotatefix;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

final class OrientationOverlayController {
    private static final String TAG = "RotateFixOverlay";
    private static final long RELEASE_START_DELAY_MS = 80L;
    private static final long RELEASE_DELAY_MS = 500L;

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable showReleaseOverlay = this::showReleaseOverlay;
    private final Runnable removeOverlay = this::removeImmediately;
    private View overlayView;
    private WindowManager.LayoutParams overlayParams;
    private int releaseOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private int savedAutoRotate = -1;
    private int savedUserRotation = -1;
    private boolean releasing;

    OrientationOverlayController(AccessibilityService service) {
        this.service = service;
        windowManager = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    boolean isActive() {
        return overlayView != null;
    }

    void requestPortrait() {
        handler.removeCallbacks(showReleaseOverlay);
        handler.removeCallbacks(removeOverlay);
        if (overlayView != null
                && !releasing
                && overlayParams.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            return;
        }

        if (overlayView != null) {
            removeCurrentOverlay();
        }
        if (releaseOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            int currentRotation = windowManager.getDefaultDisplay().getRotation();
            captureSystemRotationSettings();
            releaseOrientation = resolveReleaseOrientation(currentRotation);
            Log.i(TAG, "Requesting portrait; currentRotation=" + currentRotation
                    + ", releaseOrientation=" + releaseOrientation);
        }
        releasing = false;
        addOverlay(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "RotateFix portrait request");
    }

    void clearRequest() {
        if (overlayView == null || releasing) {
            return;
        }
        handler.removeCallbacks(removeOverlay);
        Log.i(TAG, "Releasing portrait; restoreOrientation=" + releaseOrientation);
        removeCurrentOverlay();
        if (restoreSystemRotationSettings()) {
            resetState();
            return;
        }
        releasing = true;
        handler.postDelayed(showReleaseOverlay, RELEASE_START_DELAY_MS);
    }

    void dispose() {
        handler.removeCallbacks(showReleaseOverlay);
        handler.removeCallbacks(removeOverlay);
        removeImmediately();
    }

    private void showReleaseOverlay() {
        if (!releasing || overlayView != null) {
            return;
        }
        if (addOverlay(releaseOrientation, "RotateFix orientation release")) {
            handler.postDelayed(removeOverlay, RELEASE_DELAY_MS);
        } else {
            resetState();
        }
    }

    private boolean addOverlay(int orientation, String title) {
        View view = new View(service);
        // A nearly invisible painted pixel keeps the orientation window attached on OEM builds
        // that discard fully transparent accessibility overlays.
        view.setBackgroundColor(Color.BLACK);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.alpha = 0.01f;
        params.screenOrientation = orientation;
        params.setTitle(title);
        try {
            windowManager.addView(view, params);
            overlayView = view;
            overlayParams = params;
            return true;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to add the orientation overlay", error);
            return false;
        }
    }

    private int resolveReleaseOrientation(int currentDisplayRotation) {
        if (savedAutoRotate != 0) {
            return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
        }
        switch (currentDisplayRotation) {
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_180:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case Surface.ROTATION_270:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            case Surface.ROTATION_0:
            default:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
    }

    private void captureSystemRotationSettings() {
        savedAutoRotate = Settings.System.getInt(service.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 1);
        savedUserRotation = Settings.System.getInt(service.getContentResolver(),
                Settings.System.USER_ROTATION, Surface.ROTATION_0);
    }

    private boolean restoreSystemRotationSettings() {
        if (savedAutoRotate < 0 || savedUserRotation < 0 || !Settings.System.canWrite(service)) {
            return false;
        }
        boolean rotationSaved = Settings.System.putInt(service.getContentResolver(),
                Settings.System.USER_ROTATION, savedUserRotation);
        boolean modeSaved = Settings.System.putInt(service.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, savedAutoRotate);
        if (!rotationSaved || !modeSaved) {
            Log.w(TAG, "Unable to restore the previous system rotation settings");
            return false;
        }
        return true;
    }

    private void removeImmediately() {
        removeCurrentOverlay();
        resetState();
    }

    private void removeCurrentOverlay() {
        if (overlayView == null) {
            return;
        }
        try {
            windowManager.removeViewImmediate(overlayView);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to remove the orientation overlay", error);
        } finally {
            overlayView = null;
            overlayParams = null;
        }
    }

    private void resetState() {
        releasing = false;
        releaseOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        savedAutoRotate = -1;
        savedUserRotation = -1;
    }
}
