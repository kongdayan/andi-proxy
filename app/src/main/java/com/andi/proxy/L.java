package com.andi.proxy;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small runtime localisation layer.
 *
 * <p>The Chinese source text is the translation key. Keeping the key stable means a translated
 * string can never drift away from the string it replaces, and it lets the app switch languages
 * without a full resource rebuild. English is the default for every non-Chinese system locale.</p>
 */
public final class L {
    public static final String SYSTEM = "system";
    public static final List<String> CODES = Collections.unmodifiableList(Arrays.asList(SYSTEM, "en", "zh", "ja", "ko", "ru"));

    private static final String PREFS = "andi_settings";
    private static final String KEY = "language";

    private static Context app;
    private static String language = SYSTEM;
    private static String resolved = "en";
    private static Map<String, String> table = Collections.emptyMap();
    private static final Map<String, Map<String, String>> cache = new HashMap<>();

    private L() {}

    /** Call once from {@link AndiApp}; safe to call again after a configuration change. */
    public static synchronized void init(Context context) {
        if (context != null) app = context.getApplicationContext();
        if (app == null) return;
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY, SYSTEM);
        language = CODES.contains(stored) ? stored : SYSTEM;
        reload();
    }

    public static synchronized void setLanguage(Context context, String code) {
        if (context != null) app = context.getApplicationContext();
        if (!CODES.contains(code)) code = SYSTEM;
        language = code;
        if (app != null) {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    LocaleManager manager = app.getSystemService(LocaleManager.class);
                    if (manager != null) manager.setApplicationLocales(toLocales(code));
                } catch (RuntimeException ignored) {
                    // Some OEM builds restrict this API; the in-app preference still works.
                }
            }
        }
        reload();
    }

    private static LocaleList toLocales(String code) {
        if (SYSTEM.equals(code)) return LocaleList.getEmptyLocaleList();
        if ("zh".equals(code)) return new LocaleList(Locale.forLanguageTag("zh-CN"));
        return new LocaleList(Locale.forLanguageTag(code));
    }

    private static void reload() {
        String code = language;
        if (SYSTEM.equals(code)) code = systemLanguage();
        resolved = code;
        if ("zh".equals(code)) {
            table = Collections.emptyMap();
            return;
        }
        Map<String, String> loaded = cache.get(code);
        if (loaded == null) {
            loaded = load("i18n/" + code + ".json");
            cache.put(code, loaded);
        }
        table = loaded;
    }

    private static String systemLanguage() {
        if (app != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                LocaleManager manager = app.getSystemService(LocaleManager.class);
                if (manager != null) {
                    LocaleList locales = manager.getApplicationLocales();
                    if (!locales.isEmpty()) return normalize(locales.get(0).getLanguage());
                }
            } catch (RuntimeException ignored) {
                // Fall through to the process locale.
            }
        }
        return normalize(Locale.getDefault().getLanguage());
    }

    private static String normalize(String code) {
        if (code == null) return "en";
        code = code.toLowerCase(Locale.ROOT);
        if (code.startsWith("zh")) return "zh";
        if (code.startsWith("ja")) return "ja";
        if (code.startsWith("ko")) return "ko";
        if (code.startsWith("ru")) return "ru";
        return "en";
    }

    private static Map<String, String> load(String asset) {
        if (app == null) return Collections.emptyMap();
        try (InputStream in = app.getAssets().open(asset)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            JSONObject json = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            Map<String, String> result = new HashMap<>();
            for (Iterator<String> keys = json.keys(); keys.hasNext(); ) {
                String key = keys.next();
                result.put(key, json.getString(key));
            }
            return Collections.unmodifiableMap(result);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    /** Translate a Chinese source string. Missing translations fall back to the source. */
    public static String t(String source) {
        if (source == null || source.isEmpty()) return source;
        String value = table.get(source);
        return value == null || value.isEmpty() ? source : value;
    }

    /** Translate a printf-style template and apply its arguments. */
    public static String f(String source, Object... args) {
        String pattern = t(source);
        try {
            return String.format(Locale.getDefault(), pattern, args);
        } catch (RuntimeException ignored) {
            try {
                return String.format(Locale.ROOT, source, args);
            } catch (RuntimeException ignoredAgain) {
                return source;
            }
        }
    }

    public static String language() { return language; }

    public static String resolvedLanguage() { return resolved; }

    public static String languageName(String code) {
        switch (code) {
            case SYSTEM: return t("跟随系统");
            case "en": return "English";
            case "zh": return "简体中文";
            case "ja": return "日本語";
            case "ko": return "한국어";
            case "ru": return "Русский";
            default: return code;
        }
    }

    /** Human-readable language list for the settings spinner. */
    public static String[] languageNames() {
        String[] names = new String[CODES.size()];
        for (int i = 0; i < CODES.size(); i++) names[i] = languageName(CODES.get(i));
        return names;
    }

    public static int languageIndex(String code) {
        int index = CODES.indexOf(code);
        return index < 0 ? 0 : index;
    }
}
