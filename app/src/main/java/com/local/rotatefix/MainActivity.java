package com.local.rotatefix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int COLOR_SURFACE = Color.rgb(245, 247, 246);
    private static final int COLOR_CONTAINER = Color.WHITE;
    private static final int COLOR_ACCENT = Color.rgb(8, 127, 103);
    private static final int COLOR_ACCENT_DARK = Color.rgb(5, 97, 79);
    private static final int COLOR_TEXT = Color.rgb(24, 32, 29);
    private static final int COLOR_SECONDARY = Color.rgb(101, 112, 107);
    private static final int COLOR_OUTLINE = Color.rgb(217, 224, 221);
    private static final int COLOR_WARNING = Color.rgb(185, 92, 21);

    private final ExecutorService loaderExecutor = Executors.newSingleThreadExecutor();

    private SelectedAppsStore selectedAppsStore;
    private Set<String> selectedPackages;
    private AppListAdapter adapter;
    private TextView selectionCount;
    private TextView clearSelection;
    private TextView serviceTitle;
    private TextView serviceDescription;
    private TextView serviceGuide;
    private View serviceDot;
    private LinearLayout servicePanel;
    private Button settingsButton;
    private ProgressBar progressBar;
    private ListView appList;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();

        selectedAppsStore = new SelectedAppsStore(this);
        selectedPackages = selectedAppsStore.load();
        adapter = new AppListAdapter(this, this::changeSelection);
        adapter.setSelectedPackages(selectedPackages);

        setContentView(buildContentView());
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureSystemBars();
        updateServiceStatus();
        selectedPackages = selectedAppsStore.load();
        adapter.setSelectedPackages(selectedPackages);
        updateSelectionSummary();
    }

    @Override
    protected void onDestroy() {
        loaderExecutor.shutdownNow();
        super.onDestroy();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_SURFACE);
        root.setPadding(dp(20), dp(18), dp(20), 0);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(dp(20), dp(18) + top, dp(20), bottom);
            return insets;
        });

        TextView title = text("RotateFix", 28, COLOR_TEXT, Typeface.BOLD);
        root.addView(title, wrap());

        TextView subtitle = text("앱별 세로 고정", 14, COLOR_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = wrap();
        subtitleParams.topMargin = dp(2);
        root.addView(subtitle, subtitleParams);

        root.addView(buildServicePanel(), marginParams(-1, -2, 0, 18, 0, 0));

        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);

        selectionCount = text("앱 선택", 18, COLOR_TEXT, Typeface.BOLD);
        sectionHeader.addView(selectionCount, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        clearSelection = text("전체 해제", 13, COLOR_ACCENT, Typeface.BOLD);
        clearSelection.setGravity(Gravity.CENTER);
        clearSelection.setMinWidth(dp(72));
        clearSelection.setMinHeight(dp(40));
        clearSelection.setBackground(selectableBackground(Color.TRANSPARENT, dp(6)));
        clearSelection.setOnClickListener(view -> clearAllSelections());
        sectionHeader.addView(clearSelection, wrap());
        root.addView(sectionHeader, wrapMatch());

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        search.setTextColor(COLOR_TEXT);
        search.setHintTextColor(Color.rgb(133, 143, 138));
        search.setHint("앱 이름 또는 패키지 검색");
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(10));
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(roundedBackground(COLOR_CONTAINER, COLOR_OUTLINE, dp(8)));
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                adapter.setQuery(value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        root.addView(search, marginParams(-1, 50, 0, 8, 0, 10));

        FrameLayout listContainer = new FrameLayout(this);
        appList = new ListView(this);
        appList.setAdapter(adapter);
        appList.setDivider(new android.graphics.drawable.ColorDrawable(COLOR_OUTLINE));
        appList.setDividerHeight(dp(1));
        appList.setBackgroundColor(COLOR_SURFACE);
        appList.setClipToPadding(false);
        appList.setPadding(0, 0, 0, dp(16));
        listContainer.addView(appList, match());

        emptyView = text("검색 결과가 없습니다.", 14, COLOR_SECONDARY, Typeface.NORMAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        listContainer.addView(emptyView, match());
        appList.setEmptyView(emptyView);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(COLOR_ACCENT));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(40), dp(40));
        progressParams.gravity = Gravity.CENTER;
        listContainer.addView(progressBar, progressParams);

        root.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        updateSelectionSummary();
        return root;
    }

    private View buildServicePanel() {
        servicePanel = new LinearLayout(this);
        servicePanel.setOrientation(LinearLayout.VERTICAL);
        servicePanel.setPadding(dp(16), dp(14), dp(14), dp(13));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        serviceDot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.setMarginEnd(dp(10));
        topRow.addView(serviceDot, dotParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        serviceTitle = text("서비스 확인 중", 16, COLOR_TEXT, Typeface.BOLD);
        labels.addView(serviceTitle, wrapMatch());
        serviceDescription = text("접근성 서비스 상태를 확인합니다.", 12, COLOR_SECONDARY,
                Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = wrapMatch();
        descriptionParams.topMargin = dp(2);
        labels.addView(serviceDescription, descriptionParams);
        topRow.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        settingsButton = new Button(this);
        settingsButton.setText(R.string.action_review_settings);
        settingsButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        settingsButton.setTextColor(Color.WHITE);
        settingsButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        settingsButton.setAllCaps(false);
        settingsButton.setMinHeight(0);
        settingsButton.setMinWidth(0);
        settingsButton.setPadding(dp(14), 0, dp(14), 0);
        settingsButton.setBackground(selectableBackground(COLOR_ACCENT, dp(7)));
        settingsButton.setOnClickListener(view -> openRequiredPermissionSettings());
        topRow.addView(settingsButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
        servicePanel.addView(topRow, wrapMatch());

        serviceGuide = text(getString(R.string.guide_ready), 12, COLOR_SECONDARY,
                Typeface.NORMAL);
        serviceGuide.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams noteParams = wrapMatch();
        noteParams.topMargin = dp(10);
        servicePanel.addView(serviceGuide, noteParams);
        return servicePanel;
    }

    private void loadApps() {
        loaderExecutor.execute(() -> {
            List<AppEntry> apps = AppRepository.loadLaunchableApps(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adapter.setApps(apps);
                progressBar.setVisibility(View.GONE);
                updateSelectionSummary();
            });
        });
    }

    private void changeSelection(String packageName, boolean selected) {
        Set<String> updated = new HashSet<>(selectedPackages);
        if (selected) {
            updated.add(packageName);
        } else {
            updated.remove(packageName);
        }
        selectedPackages = updated;
        selectedAppsStore.save(selectedPackages);
        adapter.setSelectedPackages(selectedPackages);
        updateSelectionSummary();
    }

    private void clearAllSelections() {
        if (selectedPackages.isEmpty()) {
            return;
        }
        selectedPackages = new HashSet<>();
        selectedAppsStore.save(selectedPackages);
        adapter.setSelectedPackages(selectedPackages);
        updateSelectionSummary();
    }

    private void updateSelectionSummary() {
        int count = selectedPackages == null ? 0 : selectedPackages.size();
        if (selectionCount != null) {
            selectionCount.setText(count == 0
                    ? getString(R.string.app_selection_title)
                    : getString(R.string.app_selection_count, count));
        }
        if (clearSelection != null) {
            clearSelection.setEnabled(count > 0);
            clearSelection.setAlpha(count > 0 ? 1f : 0.35f);
        }
    }

    private void updateServiceStatus() {
        if (servicePanel == null) {
            return;
        }
        boolean accessibilityEnabled = isRotationServiceEnabled();
        boolean systemSettingsEnabled = Settings.System.canWrite(this);
        boolean ready = accessibilityEnabled && systemSettingsEnabled;
        int dotColor = ready ? COLOR_ACCENT : COLOR_WARNING;
        serviceDot.setBackground(circleBackground(dotColor));
        if (!systemSettingsEnabled) {
            serviceTitle.setText(R.string.status_restore_permission_title);
            serviceDescription.setText(R.string.status_restore_permission_description);
            serviceGuide.setText(R.string.guide_restore_permission);
            settingsButton.setText(R.string.action_allow_restore_permission);
        } else if (!accessibilityEnabled) {
            serviceTitle.setText(R.string.status_accessibility_title);
            serviceDescription.setText(R.string.status_accessibility_description);
            serviceGuide.setText(R.string.guide_accessibility_path);
            settingsButton.setText(R.string.action_show_accessibility_steps);
        } else {
            serviceTitle.setText(R.string.status_ready_title);
            serviceDescription.setText(R.string.status_ready_description);
            serviceGuide.setText(R.string.guide_ready);
            settingsButton.setText(R.string.action_review_settings);
        }
        servicePanel.setBackground(roundedBackground(
                ready ? Color.rgb(232, 244, 240) : Color.rgb(255, 243, 232),
                ready ? Color.rgb(185, 218, 207) : Color.rgb(241, 205, 174),
                dp(8)));
    }

    private boolean isRotationServiceEnabled() {
        ComponentName expected = new ComponentName(this, RotationAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) {
            return false;
        }
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName enabled = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(enabled)) {
                return true;
            }
        }
        return false;
    }

    private void openRequiredPermissionSettings() {
        if (!Settings.System.canWrite(this)) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            } catch (RuntimeException ignored) {
                // Fall through to the general settings page.
            }
        }
        if (!isRotationServiceEnabled()) {
            showAccessibilityInstructions();
            return;
        }
        openAccessibilitySettings();
    }

    private void showAccessibilityInstructions() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_steps_title)
                .setMessage(R.string.accessibility_steps_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_open_accessibility_settings,
                        (dialog, which) -> openAccessibilitySettings())
                .show();
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (RuntimeException error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(COLOR_SURFACE);
        getWindow().setNavigationBarColor(COLOR_SURFACE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (android.os.Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
            int lightBars = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(lightBars, lightBars);
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private TextView text(String value, int sizeSp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private GradientDrawable roundedBackground(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable circleBackground(int fill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fill);
        return drawable;
    }

    private RippleDrawable selectableBackground(int fill, int radius) {
        GradientDrawable content = new GradientDrawable();
        content.setColor(fill);
        content.setCornerRadius(radius);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(35, 0, 0, 0)), content, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapMatch() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private ViewGroup.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams marginParams(int widthDp, int heightDp,
                                                    int leftDp, int topDp,
                                                    int rightDp, int bottomDp) {
        int width = widthDp < 0 ? widthDp : dp(widthDp);
        int height = heightDp < 0 ? heightDp : dp(heightDp);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(leftDp), dp(topDp), dp(rightDp), dp(bottomDp));
        return params;
    }
}
