package com.andi.proxy;
import android.content.Context;
import android.content.res.Configuration;
public final class Theme {
    private Theme() {}
    public static boolean isDark(Context c,int mode) { return mode==2||(mode==0&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES); }
}
