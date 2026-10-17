package com.andi.proxy;

import android.app.Application;

public final class AndiApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        L.init(this);
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        L.init(this);
    }
}
