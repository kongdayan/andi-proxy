package com.andi.proxy;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.*;

public final class AppPickerActivity extends Activity {
    private final List<AppEntry> all = new ArrayList<>(), shown = new ArrayList<>();
    private final Set<String> selected = new LinkedHashSet<>();
    private AppAdapter adapter;
    private TextView counter, empty;
    private EditText search;
    private Button clearQuery;
    private SearchQuery query = new SearchQuery("");
    private int scope;
    private boolean loading = true, dark;
    private String loadError;

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle saved) {
        Profile profile;
        try { profile = new ProfileStore(this).load(); } catch (Exception e) { profile = new Profile(); }
        dark = Theme.isDark(this, profile.theme);
        setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(saved);
        if (saved != null) {
            ArrayList<String> restored = saved.getStringArrayList("selected");
            if (restored != null) selected.addAll(restored);
            scope = Math.max(0, Math.min(3, saved.getInt("scope", 0)));
        } else for (String s : profile.bypassApps.split("\\s+")) if (!s.isEmpty()) selected.add(s);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(20) + insets.getSystemWindowInsetLeft(), dp(16) + insets.getSystemWindowInsetTop(),
                dp(20) + insets.getSystemWindowInsetRight(), dp(16) + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        TextView title = new TextView(this);
        title.setText(L.t("应用直连白名单")); title.setTextSize(26); root.addView(title);
        TextView info = new TextView(this);
        info.setText(L.t("勾选应用绕过本机 VPN；其余按规则分流。包含系统应用，Andi 自身自动排除。相同 UID 的应用共享排除结果。"));
        root.addView(info);

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        search = new EditText(this);
        search.setId(R.id.app_search);
        search.setHint(L.t("搜索应用名或包名")); search.setContentDescription(L.t("搜索应用名或包名"));
        search.setSingleLine(true); search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setText(saved == null ? "" : saved.getString("query", ""));
        searchBar.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1));
        clearQuery = new Button(this);
        clearQuery.setId(R.id.app_search_clear);
        clearQuery.setText(L.t("清除")); clearQuery.setContentDescription(L.t("清除搜索内容"));
        clearQuery.setMinWidth(0); clearQuery.setMinimumWidth(0);
        clearQuery.setOnClickListener(v -> search.setText(""));
        searchBar.addView(clearQuery, new LinearLayout.LayoutParams(dp(64), dp(48)));
        root.addView(searchBar);

        Spinner scopes = new Spinner(this);
        scopes.setId(R.id.app_search_scope); scopes.setContentDescription(L.t("搜索范围"));
        ArrayAdapter<String> scopeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{L.t("全部应用"), L.t("已选应用"), L.t("用户应用"), L.t("系统应用")});
        scopeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scopes.setAdapter(scopeAdapter); scopes.setSelection(scope);
        root.addView(scopes, new LinearLayout.LayoutParams(-1, dp(48)));
        counter = new TextView(this); counter.setId(R.id.app_search_count); root.addView(counter);

        LinearLayout actions = new LinearLayout(this);
        Button select = new Button(this);
        select.setId(R.id.app_search_select_visible); select.setText(L.t("全选当前列表"));
        select.setPadding(dp(4), 0, dp(4), 0); select.setTextSize(12);
        select.setOnClickListener(v -> { for (AppEntry a : shown) selected.add(a.info.packageName); filter(); });
        actions.addView(select, new LinearLayout.LayoutParams(0, -2, 1));
        Button clear = new Button(this);
        clear.setId(R.id.app_search_clear_selection); clear.setText(L.t("清空白名单"));
        clear.setPadding(dp(4), 0, dp(4), 0); clear.setTextSize(12);
        clear.setOnClickListener(v -> { selected.clear(); filter(); });
        actions.addView(clear, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(actions);

        FrameLayout results = new FrameLayout(this);
        ListView list = new ListView(this); list.setId(R.id.app_search_list); list.setItemsCanFocus(true);
        adapter = new AppAdapter(); list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> toggle(shown.get(pos).info.packageName));
        results.addView(list, new FrameLayout.LayoutParams(-1, -1));
        empty = new TextView(this); empty.setId(R.id.app_search_empty);
        empty.setGravity(Gravity.CENTER); empty.setPadding(dp(16), dp(16), dp(16), dp(16));
        results.addView(empty, new FrameLayout.LayoutParams(-1, -1)); list.setEmptyView(empty);
        root.addView(results, new LinearLayout.LayoutParams(-1, 0, 1));
        Button save = new Button(this); save.setText(L.t("保存白名单"));
        save.setOnClickListener(v -> {
            try {
                Profile current = new ProfileStore(this).load(); current.bypassApps = String.join("\n", selected);
                new ProfileStore(this).save(current); Toast.makeText(this, L.t("已保存"), Toast.LENGTH_SHORT).show(); finish();
            } catch (Exception e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
        root.addView(save); setContentView(root);
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            boolean compact = b - t - root.getPaddingTop() - root.getPaddingBottom() < dp(430);
            title.setVisibility(compact ? View.GONE : View.VISIBLE);
            info.setVisibility(compact ? View.GONE : View.VISIBLE);
        });

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { filter(); }
            public void afterTextChanged(Editable e) {}
        });
        scopes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) { scope = pos; filter(); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        filter();
        new Thread(() -> {
            List<AppEntry> apps = new ArrayList<>();
            String error = null;
            try {
                for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                    if (!app.packageName.equals(getPackageName()))
                        apps.add(new AppEntry(app, app.loadLabel(getPackageManager()).toString()));
                }
                apps.sort(Comparator.comparing(a -> a.label, java.text.Collator.getInstance()));
            } catch (RuntimeException e) { error = L.t("无法读取已安装的应用，请重新打开此页面。"); }
            final String failure = error;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                all.addAll(apps); loading = false; loadError = failure; filter();
            });
        }, "andi-app-search").start();
    }

    private void filter() {
        query = new SearchQuery(search.getText().toString());
        shown.clear();
        Map<AppEntry, Integer> ranks = new HashMap<>();
        for (AppEntry a : all) {
            if (scope == 1 && !selected.contains(a.info.packageName) || scope == 2 && a.system || scope == 3 && !a.system) continue;
            int rank = query.rank(a.label, a.info.packageName);
            if (rank >= 0) { shown.add(a); ranks.put(a, rank); }
        }
        shown.sort(Comparator.comparingInt(ranks::get));
        clearQuery.setVisibility(search.length() == 0 ? View.INVISIBLE : View.VISIBLE);
        counter.setText(loading ? L.t("正在加载应用…") : L.f("已选择 %1$d 个 · 找到 %2$d 个应用", selected.size(), shown.size()));
        empty.setText(loading ? L.t("正在加载应用…") : loadError != null ? loadError : query.isEmpty()
            ? (scope == 1 ? L.t("还没有选择应用\n切换到全部应用后勾选。") : L.t("此范围内没有应用"))
            : L.t("没有匹配的应用\n试试其他关键词，或清除搜索、更改范围。"));
        adapter.notifyDataSetChanged();
    }

    private void toggle(String pkg) { if (!selected.remove(pkg)) selected.add(pkg); filter(); }

    private CharSequence highlight(String value) {
        SpannableString result = new SpannableString(value);
        for (SearchQuery.Range match : query.ranges(value))
            result.setSpan(new BackgroundColorSpan(dark ? Color.rgb(100, 77, 20) : Color.rgb(255, 230, 155)),
                match.start, match.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }

    @Override public void onSaveInstanceState(Bundle b) {
        b.putStringArrayList("selected", new ArrayList<>(selected));
        b.putString("query", search.getText().toString()); b.putInt("scope", scope);
        super.onSaveInstanceState(b);
    }

    private static final class AppEntry {
        final ApplicationInfo info;
        final String label;
        final boolean system;
        AppEntry(ApplicationInfo info, String label) {
            this.info = info; this.label = label;
            system = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        }
    }

    private final class AppAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int p) { return shown.get(p); }
        public long getItemId(int p) { return p; }
        public View getView(int pos, View recycled, ViewGroup parent) {
            AppEntry a = shown.get(pos);
            LinearLayout row = new LinearLayout(AppPickerActivity.this);
            row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(4), dp(10), dp(4), dp(10));
            ImageView icon = new ImageView(AppPickerActivity.this); icon.setImageDrawable(a.info.loadIcon(getPackageManager()));
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
            LinearLayout labels = new LinearLayout(AppPickerActivity.this);
            labels.setPadding(dp(12), 0, dp(4), 0); labels.setOrientation(LinearLayout.VERTICAL);
            TextView name = new TextView(AppPickerActivity.this); name.setId(R.id.app_search_result_title);
            name.setText(highlight(a.label)); name.setTextSize(16); labels.addView(name);
            TextView pkg = new TextView(AppPickerActivity.this); pkg.setId(R.id.app_search_result_detail);
            pkg.setText(highlight(a.info.packageName)); pkg.setTextSize(11); labels.addView(pkg);
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            CheckBox box = new CheckBox(AppPickerActivity.this); box.setChecked(selected.contains(a.info.packageName));
            box.setContentDescription(L.f("%s · 直连", a.label)); box.setOnClickListener(v -> toggle(a.info.packageName)); row.addView(box);
            return row;
        }
    }
}
