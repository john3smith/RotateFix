package com.local.rotatefix;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AppListAdapter extends BaseAdapter {
    interface SelectionListener {
        void onSelectionChanged(String packageName, boolean selected);
    }

    private final Context context;
    private final SelectionListener selectionListener;
    private final List<AppEntry> allApps = new ArrayList<>();
    private final List<AppEntry> visibleApps = new ArrayList<>();
    private Set<String> selectedPackages = new HashSet<>();
    private String query = "";

    AppListAdapter(Context context, SelectionListener selectionListener) {
        this.context = context;
        this.selectionListener = selectionListener;
    }

    void setApps(List<AppEntry> apps) {
        allApps.clear();
        allApps.addAll(apps);
        applyFilter();
    }

    void setSelectedPackages(Set<String> packages) {
        selectedPackages = new HashSet<>(packages);
        notifyDataSetChanged();
    }

    void setQuery(String query) {
        this.query = query == null ? "" : query;
        applyFilter();
    }

    private void applyFilter() {
        visibleApps.clear();
        for (AppEntry app : allApps) {
            if (AppSearch.matches(app.label, app.packageName, query)) {
                visibleApps.add(app);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return visibleApps.size();
    }

    @Override
    public AppEntry getItem(int position) {
        return visibleApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).packageName.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowHolder holder;
        if (convertView == null) {
            holder = createRow();
            convertView = holder.root;
            convertView.setTag(holder);
        } else {
            holder = (RowHolder) convertView.getTag();
        }

        AppEntry app = getItem(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.label);
        holder.packageName.setText(app.packageName);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedPackages.contains(app.packageName));
        holder.checkBox.setOnCheckedChangeListener((button, checked) ->
                selectionListener.onSelectionChanged(app.packageName, checked));
        holder.root.setOnClickListener(view -> {
            boolean checked = !selectedPackages.contains(app.packageName);
            selectionListener.onSelectionChanged(app.packageName, checked);
        });
        return convertView;
    }

    private RowHolder createRow() {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(4), dp(10), dp(2), dp(10));
        root.setMinimumHeight(dp(70));

        TypedValue selectable = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true)) {
            root.setBackgroundResource(selectable.resourceId);
        }

        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.setMarginEnd(dp(12));
        root.addView(icon, iconParams);

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(context);
        name.setTextColor(Color.rgb(24, 32, 29));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView packageName = new TextView(context);
        packageName.setTextColor(Color.rgb(101, 112, 107));
        packageName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        packageName.setSingleLine(true);
        packageName.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        packageParams.topMargin = dp(2);
        labels.addView(packageName, packageParams);

        CheckBox checkBox = new CheckBox(context);
        checkBox.setContentDescription("세로 고정 선택");
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{Color.rgb(8, 127, 103), Color.rgb(135, 145, 140)};
        checkBox.setButtonTintList(new ColorStateList(states, colors));
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        checkParams.setMarginStart(dp(8));
        root.addView(checkBox, checkParams);

        return new RowHolder(root, icon, name, packageName, checkBox);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class RowHolder {
        final LinearLayout root;
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final CheckBox checkBox;

        RowHolder(LinearLayout root, ImageView icon, TextView name, TextView packageName,
                  CheckBox checkBox) {
            this.root = root;
            this.icon = icon;
            this.name = name;
            this.packageName = packageName;
            this.checkBox = checkBox;
        }
    }
}
