package com.andi.proxy;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.*;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.LinkedHashMap;
import java.util.Map;
public final class QuickControls {
    private QuickControls() {}
    static RemoteViews view(Context c,int id,boolean expanded) {
        RemoteViews v=new RemoteViews(c.getPackageName(),expanded?R.layout.proxy_widget_expanded:R.layout.proxy_widget);
        if(expanded) {
            v.setTextViewText(R.id.widget_state,"Andi · "+L.t(ProxyService.status));
            v.setTextViewText(R.id.widget_detail,ProxyService.isSharing()?ProxyService.address+":2080":L.t(LocalVpnService.active?"本机按规则分流":"选择本机代理或热点共享"));
        }
        v.setTextViewText(R.id.widget_vpn,L.f("本机 · %s",L.t(LocalVpnService.active?"停止":"开启")));
        v.setTextViewText(R.id.widget_share,L.f("热点 · %s",L.t(ProxyService.isSharing()?"停止":"开启")));
        v.setBoolean(R.id.widget_vpn,"setEnabled",!ProxyService.busy);
        v.setBoolean(R.id.widget_share,"setEnabled",!ProxyService.busy);
        v.setOnClickPendingIntent(R.id.widget_vpn,action(c,id*3,"vpn_toggle"));
        v.setOnClickPendingIntent(R.id.widget_share,action(c,id*3+1,"share_toggle"));
        v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,id*3+2,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
    private static PendingIntent action(Context c,int code,String name) {
        return PendingIntent.getActivity(c,code,new Intent(c,MainActivity.class).putExtra("quick_action",name),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void refresh(Context c) {
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(int id:m.getAppWidgetIds(new ComponentName(c,ProxyWidget.class))) {
            RemoteViews views;
            if(Build.VERSION.SDK_INT>=31) {
                Map<SizeF,RemoteViews> sizes=new LinkedHashMap<>();
                sizes.put(new SizeF(120,48),view(c,id,false)); sizes.put(new SizeF(180,110),view(c,id,true));
                views=new RemoteViews(sizes);
            } else {
                Bundle b=m.getAppWidgetOptions(id);
                views=view(c,id,b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)>=180&&b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)>=110);
            }
            m.updateAppWidget(id,views);
        }
        TileService.requestListeningState(c,new ComponentName(c,ProxyTile.class));
    }
}
