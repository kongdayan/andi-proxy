package com.andi.proxy;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.*;
public final class ProxyTile extends TileService {
    @Override public void onStartListening() {
        Tile t=getQsTile(); if(t==null) return;
        t.setState(ProxyService.active?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE); t.setLabel(L.t("Andi 代理"));
        if(Build.VERSION.SDK_INT>=29) t.setSubtitle(ProxyService.active?ProxyService.address:L.t("点击启动")); t.updateTile();
    }
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // Legacy API is required on Android 13 and below.
    @Override public void onClick() {
        if(ProxyService.active) { startService(new Intent(this,ProxyService.class).setAction(ProxyService.STOP)); onStartListening(); }
        else {
            Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("quick_action","start_default");
            if(Build.VERSION.SDK_INT>=34) startActivityAndCollapse(PendingIntent.getActivity(this,32,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            else startActivityAndCollapse(i);
        }
    }
}
