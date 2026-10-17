package com.andi.proxy;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
public final class ProxyWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) { QuickControls.refresh(c); }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options) { QuickControls.refresh(c); }
}
