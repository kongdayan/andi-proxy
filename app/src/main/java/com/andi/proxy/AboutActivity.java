package com.andi.proxy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Project, source, licence and privacy information bundled with the APK. */
public final class AboutActivity extends Activity {
    private int GREEN, INK, MUTED, BACK, CARD;
    private boolean dark;
    private LinearLayout content;

    @Override public void onCreate(Bundle saved) {
        Profile profile;
        try { profile = new ProfileStore(this).load(); } catch (Exception e) { profile = new Profile(); }
        dark = Theme.isDark(this, profile.theme);
        setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(saved);
        GREEN = Color.parseColor(dark ? "#8EB9FF" : "#276CF2");
        INK = Color.parseColor(dark ? "#F4F6FA" : "#172136");
        MUTED = Color.parseColor(dark ? "#A8B0BF" : "#6B7485");
        BACK = Color.parseColor(dark ? "#111318" : "#F3F5F9");
        CARD = Color.parseColor(dark ? "#1E222A" : "#FFFFFF");

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACK);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(20), padding, dp(56));
        scroll.addView(content);
        setContentView(scroll);

        header();
        project();
        components();
        privacy();
    }

    private void header() {
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(84), dp(84));
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = dp(12);
        content.addView(icon, iconParams);

        TextView name = text("Andi Proxy", 30, INK);
        name.setTypeface(null, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        content.addView(name);

        TextView tagline = text(L.t("你的热点，你的规则。"), 15, MUTED);
        tagline.setGravity(Gravity.CENTER);
        content.addView(tagline);

        TextView version = text(L.f("版本 %1$s (%2$d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), 12, MUTED);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(6), 0, dp(16));
        content.addView(version);
    }

    private void project() {
        LinearLayout card = card(L.t("关于本项目"));
        card.addView(text(L.t("无需 root 的 Android 热点／局域网代理共享与本机 VPN 分流。上游支持 HY2 和带用户名、密码认证的 SOCKS5，并可使用 Shadowrocket 规则订阅。"), 14, INK));
        button(card, L.t("源代码"), () -> open(BuildConfig.PROJECT_URL));
        button(card, L.t("问题反馈"), () -> open(BuildConfig.PROJECT_URL + "/issues"));
        button(card, L.t("版本发布"), () -> open(BuildConfig.PROJECT_URL + "/releases"));
        button(card, L.t("开源许可 · GPL-3.0-or-later"), () -> open(BuildConfig.PROJECT_URL + "/blob/main/LICENSE"));
        button(card, L.t("第三方组件与许可"), this::showThirdPartyNotices);
    }

    private void components() {
        LinearLayout card = card(L.t("开源组件"));
        card.addView(text(L.t("分流核心：sing-box 1.14.2（GPL-3.0-or-later）。本机 VPN 转发：hev-socks5-tunnel 2.18.0（MIT）。两者都是未修改的上游官方构建。"), 13, INK));
        button(card, "sing-box ↗", () -> open("https://github.com/SagerNet/sing-box/tree/v1.14.2"));
        button(card, "hev-socks5-tunnel ↗", () -> open("https://github.com/heiher/hev-socks5-tunnel/tree/2.18.0"));
    }

    private void privacy() {
        LinearLayout card = card(L.t("隐私"));
        card.addView(text(L.t("Andi Proxy 不上传你的配置、订阅或使用数据。服务器凭据使用 Android Keystore 加密保存在本机；订阅只从你填写的地址下载。没有统计、广告或遥测。"), 13, INK));
        button(card, L.t("阅读隐私说明"), () -> open(BuildConfig.PROJECT_URL + "/blob/main/docs/PRIVACY.md"));
    }

    private void showThirdPartyNotices() {
        String notices;
        try (InputStream in = getAssets().open("licenses/THIRD_PARTY_NOTICES.md")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            notices = out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            notices = L.t("无法读取许可说明。");
        }
        new AlertDialog.Builder(this).setTitle(L.t("第三方组件与许可")).setMessage(notices)
            .setPositiveButton(L.t("关闭"), null).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(0, dp(3), 0, dp(5));
        return view;
    }

    private LinearLayout card(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(28));
        card.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(14);
        content.addView(card, params);
        TextView heading = text(title, 18, INK);
        heading.setTypeface(null, Typeface.BOLD);
        card.addView(heading);
        return card;
    }

    private void button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(GREEN);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(dark ? Color.rgb(39, 50, 71) : Color.rgb(232, 240, 255));
        shape.setCornerRadius(dp(18));
        button.setBackground(shape);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(6);
        parent.addView(button, params);
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { new AlertDialog.Builder(this).setMessage(url).setPositiveButton(L.t("关闭"), null).show(); }
    }
}
