package com.andi.proxy;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.net.VpnService;
import android.appwidget.AppWidgetManager;
import android.service.quicksettings.TileService;
import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.net.NetworkInterface;
import java.util.*;

public final class MainActivity extends Activity {
    private int GREEN, INK, MUTED, BACK, CARD;
    private boolean dark, pendingCreate, pendingDeviceOnly, updating;
    private TextView phoneState, shareState;
    private Button phoneButton, shareButton, connectButton, testButton;
    private TextView subscriptionStatus;
    private Switch subscriptionEnabled,autoUpdate,customHotspot,vpnEnabled;
    private Spinner hotspotBand,theme,language;
    private final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private Profile draft=new Profile();
    private LinearLayout content;
    private ScrollView scroll;
    private final List<Button> navButtons=new ArrayList<>();
    private TextView state,description;
    private int tab=0;
    private final Map<String,EditText> fields=new HashMap<>();
    private Spinner protocol,defaultRoute,network;
    private Switch insecure,socksUdp,localAuth;
    private final List<String> interfaceNames=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable poll=new Runnable() { public void run() { refresh(); handler.postDelayed(this,1500); } };

    @Override public void onCreate(Bundle saved) {
        try { draft=new ProfileStore(this).load(); } catch(Exception e) { toast(L.t("保存的配置无法读取，请重新导入")); }
        if(saved!=null) try { draft=Profile.fromJson(new org.json.JSONObject(saved.getString("draft","{}"))); tab=saved.getInt("tab",0); pendingCreate=saved.getBoolean("pendingCreate"); pendingDeviceOnly=saved.getBoolean("pendingDeviceOnly"); } catch(Exception ignored) {}
        dark=Theme.isDark(this,draft.theme); setTheme(dark?R.style.AppThemeDark:R.style.AppTheme); super.onCreate(saved);
        GREEN=Color.parseColor(dark?"#8EB9FF":"#276CF2"); INK=Color.parseColor(dark?"#F4F6FA":"#172136"); MUTED=Color.parseColor(dark?"#A8B0BF":"#6B7485"); BACK=Color.parseColor(dark?"#111318":"#F3F5F9"); CARD=Color.parseColor(dark?"#1E222A":"#FFFFFF");
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(14),dp(20),dp(8)); root.setBackgroundColor(BACK);
        final int[] windowInsets=new int[4],windowStyle={-1};
        root.setOnApplyWindowInsetsListener((v,insets)->{ windowInsets[0]=insets.getSystemWindowInsetLeft(); windowInsets[1]=insets.getSystemWindowInsetTop(); windowInsets[2]=insets.getSystemWindowInsetRight(); windowInsets[3]=insets.getSystemWindowInsetBottom(); windowStyle[0]=-1; v.setPadding(dp(20)+windowInsets[0],dp(14)+windowInsets[1],dp(20)+windowInsets[2],dp(8)+windowInsets[3]); return insets.consumeSystemWindowInsets(); });
        TextView brand=text("Andi",getResources().getConfiguration().screenHeightDp<500?26:40,INK); brand.setTypeface(null,Typeface.BOLD); root.addView(brand);
        LinearLayout nav=new LinearLayout(this); nav.setPadding(0,dp(16),0,dp(10));
        String[] tabs={L.t("首页"),L.t("服务器"),L.t("规则"),L.t("设置")};
        for(int i=0;i<tabs.length;i++) { final int index=i; Button b=new Button(this); b.setText(tabs[i]); b.setTextColor(GREEN); b.setAllCaps(false); b.setSingleLine(true); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(dp(4),0,dp(4),0); b.setOnClickListener(v->{ collect(); tab=index; render(); }); b.setTextSize(14); navButtons.add(b); nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1)); }
        root.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
            int availableHeight=bottom-top-windowInsets[1]-windowInsets[3];
            int mode=availableHeight<dp(360)?2:availableHeight<dp(520)?1:0;
            boolean narrow=right-left-windowInsets[0]-windowInsets[2]<dp(320);
            int style=mode*2+(narrow?1:0); if(windowStyle[0]==style) return; windowStyle[0]=style;
            brand.setVisibility(mode==2?View.GONE:View.VISIBLE); brand.setTextSize(mode==0?32:26);
            root.setPadding(dp(narrow?12:20)+windowInsets[0],dp(mode==2?4:14)+windowInsets[1],dp(narrow?12:20)+windowInsets[2],dp(8)+windowInsets[3]);
            nav.setPadding(0,dp(mode==0?16:4),0,dp(mode==0?10:2));
            for(Button b:navButtons) { b.setTextSize(narrow?12:14); LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)b.getLayoutParams(); lp.height=dp(mode==0?48:40); b.setLayoutParams(lp); }
        });

        scroll=new ScrollView(this); scroll.setFillViewport(true); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); root.addView(nav); setContentView(root); render();
        handleQuickAction(getIntent());
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
    }
    @Override protected void onResume() {
        super.onResume(); try { String updated=new ProfileStore(this).load().bypassApps; if(!updated.equals(draft.bypassApps)) { collect(); draft.bypassApps=updated; if(ProxyService.active) startService(new Intent(this,ProxyService.class).setAction(ProxyService.APPLY)); render(); } } catch(Exception ignored) {} handler.post(poll);
    }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(poll); }
    @Override protected void onSaveInstanceState(Bundle out) {
        collect(); try { out.putString("draft",draft.toJson().toString()); } catch(Exception ignored) {} out.putInt("tab",tab); out.putBoolean("pendingCreate",pendingCreate); out.putBoolean("pendingDeviceOnly",pendingDeviceOnly); super.onSaveInstanceState(out);
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(0,dp(4),0,dp(6)); return t; }
    private LinearLayout card(String title) {
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(12),dp(16),dp(14));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(CARD); bg.setCornerRadius(dp(28)); c.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(14); content.addView(c,lp);
        TextView heading=text(title,19,INK); heading.setTypeface(null,Typeface.BOLD); c.addView(heading); return c;
    }
    private Button button(LinearLayout c,String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextColor(GREEN); b.setAllCaps(false); b.setMinHeight(dp(52)); b.setBackgroundTintList(null); GradientDrawable shape=new GradientDrawable(); shape.setColor(dark?Color.rgb(39,50,71):Color.rgb(232,240,255)); shape.setCornerRadius(dp(20)); b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33276CF2),shape,null)); b.setPadding(dp(12),dp(8),dp(12),dp(8)); b.setOnClickListener(v->action.run()); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2); bp.topMargin=dp(8); c.addView(b,bp); return b;
    }
    private EditText field(LinearLayout c,String key,String label,String value,boolean password,boolean multi) {
        c.addView(text(label,13,MUTED)); EditText e=new EditText(this); e.setText(value); e.setTextSize(16); e.setSingleLine(!multi);
        e.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:multi?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE:InputType.TYPE_CLASS_TEXT);
        if(key.equals("port")) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setPadding(dp(8),dp(8),dp(8),dp(8)); e.setMinHeight(dp(48)); if(multi) e.setMinLines(3); c.addView(e,new LinearLayout.LayoutParams(-1,-2)); fields.put(key,e); return e;
    }
    private Switch toggle(LinearLayout c,String label,boolean checked) { Switch s=new Switch(this); s.setText(label); s.setTextColor(INK); s.setChecked(checked); s.setPadding(0,dp(8),0,dp(8)); c.addView(s); return s; }
    private Spinner spinner(LinearLayout c,String[] labels,int selected) {
        Spinner s=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(adapter); s.setSelection(selected); c.addView(s,new LinearLayout.LayoutParams(-1,dp(48))); return s;
    }
    private void render() {
        content.removeAllViews(); fields.clear(); protocol=null; defaultRoute=null; network=null; insecure=null; socksUdp=null; localAuth=null; state=null; description=null; phoneState=null; shareState=null; phoneButton=null; shareButton=null; connectButton=null; testButton=null; subscriptionEnabled=null; autoUpdate=null; customHotspot=null; vpnEnabled=null; hotspotBand=null; theme=null; language=null; subscriptionStatus=null;
        for(int i=0;i<navButtons.size();i++) { Button b=navButtons.get(i); GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(dp(20)); bg.setColor(i==tab?(dark?Color.rgb(39,50,71):Color.rgb(226,236,255)):Color.TRANSPARENT); b.setBackgroundTintList(null); b.setBackground(bg); b.setTextColor(i==tab?GREEN:MUTED); }
        if(tab==0) home(); else if(tab==1) server(); else if(tab==2) rules(); else settings();
        handler.post(()->scroll.scrollTo(0,0));
    }
    private void home() {
        LinearLayout top=card(L.t("连接")); top.removeViewAt(0); state=text(L.t(ProxyService.status),20,GREEN); top.addView(state);
        description=text("",13,MUTED); top.addView(description);
        if(draft.host.isEmpty()) {
            top.addView(text(L.t("先添加一台服务器，就可以开始。"),14,MUTED));
            button(top,L.t("添加服务器"),()->go(1));
        }
        LinearLayout phone=card(L.t("这台手机")); phoneState=text("",14,MUTED); phone.addView(phoneState);
        phoneButton=button(phone,L.t("开启本机代理"),this::togglePhone);
        button(phone,L.f("不代理的应用 · %d 个",bypassCount()),this::openPicker);
        LinearLayout share=card(L.t("分享给其他设备")); shareState=text("",14,MUTED); share.addView(shareState);
        shareButton=button(share,L.t("开启 Wi-Fi 共享"),()->{ if(ProxyService.isSharing()) stopSharing(); else begin(true,false); });
        connectButton=button(share,L.t("连接其他设备"),this::connectionGuide);
        LinearLayout extra=fold(share,L.t("使用系统热点或已有 Wi-Fi"));
        button(extra,L.t("系统热点设置"),this::systemHotspot);
        extra.addView(text(L.t("打开系统热点后，选择它的地址。"),13,MUTED));
        interfaceNames.clear(); interfaceNames.add(""); List<String> labels=new ArrayList<>(); labels.add(L.t("请选择网络地址"));
        try { for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if(ni.getName().startsWith("tun")) continue;
            try { String ip=DeviceDiagnostics.address(ni.getName()); interfaceNames.add(ni.getName()); labels.add(ip+" · "+ni.getName()); } catch(Exception ignored) {}
        } } catch(Exception ignored) {}
        network=spinner(extra,labels.toArray(new String[0]),Math.max(0,interfaceNames.indexOf(draft.iface)));
        button(extra,L.t("共享此网络"),()->{ collect(); if(draft.iface.isEmpty()) { toast(L.t("请选择一个网络地址")); return; } begin(false,false); });
        button(extra,L.t("刷新地址"),()->{ collect(); render(); });
        LinearLayout help=fold(content,L.t("帮助与连接检查"));
        button(help,L.t("怎么使用"),this::usageGuide);
        testButton=button(help,L.t("检测代理出口"),()->startService(new Intent(this,ProxyService.class).setAction(ProxyService.TEST)));
        button(help,L.t("详细状态"),()->new AlertDialog.Builder(this).setTitle(L.t("详细状态")).setMessage((ProxyService.detail.isEmpty()?L.t("尚未连接"):ProxyService.detail)+"\n"+L.t(LocalVpnService.state)).setPositiveButton(L.t("关闭"),null).show());
        refresh();
    }
    private void go(int page) { collect(); tab=page; render(); }
    private int bypassCount() { return (int)Arrays.stream(draft.bypassApps.split("\\s+")).filter(x->!x.isEmpty()).count(); }
    private LinearLayout fold(LinearLayout parent,String title) {
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setVisibility(View.GONE);
        Button trigger=button(parent,title+"  ▾",()->{});
        trigger.setOnClickListener(v->{ boolean show=body.getVisibility()!=View.VISIBLE; body.setVisibility(show?View.VISIBLE:View.GONE); trigger.setText(title+(show?"  ▴":"  ▾")); });
        parent.addView(body); return body;
    }
    private void server() {
        LinearLayout c=card(L.t("服务器"));
        button(c,L.t("导入配置"),this::importProfile);
        protocol=spinner(c,new String[]{"SOCKS5","HY2 / Hysteria2"},draft.protocol.equals("hysteria2")?1:0);
        field(c,"host",L.t("地址"),draft.host,false,false); field(c,"port",L.t("端口"),Integer.toString(draft.port),false,false);
        LinearLayout socks=new LinearLayout(this); socks.setOrientation(LinearLayout.VERTICAL); c.addView(socks);
        field(socks,"username",L.t("用户名"),draft.username,false,false);
        field(c,"password",L.t("认证密码"),draft.password,true,false);
        LinearLayout hy=new LinearLayout(this); hy.setOrientation(LinearLayout.VERTICAL); c.addView(hy);
        field(hy,"sni",L.t("TLS 域名（SNI）"),draft.sni,false,false);
        LinearLayout hyExtra=fold(hy,L.t("HY2 高级设置"));
        field(hyExtra,"obfs",L.t("混淆密码（可选）"),draft.obfs,true,false);
        insecure=toggle(hyExtra,L.t("跳过证书校验"),draft.insecure);
        LinearLayout socksExtra=fold(socks,L.t("SOCKS5 高级设置")); socksUdp=toggle(socksExtra,L.t("服务端支持 UDP"),draft.socksUdp);
        protocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View v,int pos,long id) { socks.setVisibility(pos==0?View.VISIBLE:View.GONE); hy.setVisibility(pos==1?View.VISIBLE:View.GONE); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        button(c,L.t("保存服务器"),()->save(true));
    }
    private void rules() {
        LinearLayout sub=card(L.t("规则订阅"));
        subscriptionEnabled=toggle(sub,L.t("使用订阅分流"),draft.subscriptionEnabled);
        field(sub,"subscriptionUrl",L.t("订阅地址"),draft.subscriptionUrl,false,false);
        subscriptionStatus=text(updating?L.t("正在更新…"):new SubscriptionStore(this).describe(draft.subscriptionUrl),13,MUTED); sub.addView(subscriptionStatus);
        button(sub,L.t("立即更新"),this::updateSubscription);
        button(sub,L.t("保存规则"),()->save(true));
        LinearLayout extra=fold(sub,L.t("订阅选项"));
        autoUpdate=toggle(extra,L.t("启动时自动更新过期规则"),draft.subscriptionAutoUpdate);
        button(extra,L.t("恢复国内直连订阅"),()->{ fields.get("subscriptionUrl").setText(RuleSubscription.EXAMPLE); subscriptionEnabled.setChecked(true); });
        button(extra,L.t("订阅说明"),()->help(L.t("订阅说明"),L.t("支持 Shadowrocket 规则列表与配置文件。只使用分流规则，不执行网页重写或证书配置。更新失败继续使用上次缓存。\n\n手动规则优先；订阅里的 FINAL 决定最终出口。")));
        LinearLayout manual=card(L.t("手动分流")); defaultRoute=spinner(manual,new String[]{L.t("其余连接走代理"),L.t("其余连接直连")},draft.defaultRoute.equals("direct")?1:0);
        LinearLayout fieldsPanel=fold(manual,L.t("编辑域名与 IP 规则"));
        field(fieldsPanel,"proxyDomains",L.t("走代理的域名"),draft.proxyDomains,false,true);
        field(fieldsPanel,"directDomains",L.t("直连域名"),draft.directDomains,false,true);
        field(fieldsPanel,"directCidrs",L.t("直连网段"),draft.directCidrs,false,true);
        fieldsPanel.addView(text(L.t("每行一个，如 example.com 或 192.168.0.0/16。"),13,MUTED));
        button(fieldsPanel,L.t("保存规则"),()->save(true));
    }
    private boolean canNameHotspot() { return Build.VERSION.SDK_INT>=36&&Build.VERSION.SDK_INT_FULL>=3600001; }
    private String deviceName() { String n=Settings.Global.getString(getContentResolver(),"device_name"); return n==null||n.trim().isEmpty()?Build.MODEL:n.trim(); }
    private void settings() {
        LinearLayout hot=card(L.t("Wi-Fi 热点"));
        if(canNameHotspot()) {
            customHotspot=toggle(hot,L.t("指定名称与密码"),draft.customHotspot);
            field(hot,"hotspotName",L.t("热点名称"),draft.hotspotName,false,false);
            button(hot,L.t("使用设备名称"),()->fields.get("hotspotName").setText(deviceName()));
            field(hot,"hotspotPassword",L.t("Wi-Fi 密码（8–63 位）"),draft.hotspotPassword,true,false);
        } else {
            hot.addView(text(L.t("App 热点：名称与密码由系统生成。"),14,INK));
            button(hot,L.t("自定义名称和密码"),()->new AlertDialog.Builder(this).setTitle(L.t("用系统热点自定义"))
                .setMessage(L.f("在系统设置 → 移动网络 → 个人热点，可修改名称、密码。名称可以填设备名称：%s。\n\n设置后回到首页，展开“使用系统热点或已有 Wi-Fi”，选择热点地址并共享。\n\nApp 热点的名称与密码由系统生成；系统热点设置不会修改 App 热点。",deviceName()))
                .setNegativeButton(L.t("关闭"),null).setPositiveButton(L.t("复制设备名称并打开设置"),(d,w)->{ copy(L.t("设备名称"),deviceName()); systemHotspot(); }).show());
        }
        hot.addView(text(L.t("App 热点频段"),13,MUTED));
        hotspotBand=spinner(hot,new String[]{L.t("自动"),L.t("2.4 GHz · 兼容优先"),L.t("5 GHz · 速度优先")},draft.hotspotBand);
        if(Build.VERSION.SDK_INT<36) { hotspotBand.setEnabled(false); hotspotBand.setSelection(0); }
        button(hot,L.t("保存热点设置"),()->save(false));
        hot.addView(text(L.t("下次开启 App 热点时使用。"),12,MUTED));
        LinearLayout appearance=card(L.t("外观"));
        theme=spinner(appearance,new String[]{L.t("跟随系统"),L.t("浅色"),L.t("深色")},draft.theme);
        button(appearance,L.t("应用主题"),()->{ if(save(false)) { QuickControls.refresh(this); recreate(); } });
        LinearLayout languageCard=card(L.t("语言"));
        languageCard.addView(text(L.t("选择应用界面语言。系统语言为中文时显示中文；其他语言默认英文。"),13,MUTED));
        language=spinner(languageCard,L.languageNames(),L.languageIndex(L.language()));
        button(languageCard,L.t("应用语言"),()->{ int selected=language.getSelectedItemPosition(); L.setLanguage(this,L.CODES.get(selected)); recreate(); });
        languageCard.addView(text(L.t("Android 13 及以上也可以在系统设置 → 应用 → Andi Proxy → 语言中修改。"),12,MUTED));
        LinearLayout shortcuts=card(L.t("快捷操作"));
        button(shortcuts,L.t("添加桌面小组件"),()->{ AppWidgetManager m=getSystemService(AppWidgetManager.class); if(m.isRequestPinAppWidgetSupported()) m.requestPinAppWidget(new ComponentName(this,ProxyWidget.class),null,null); else toast(L.t("长按桌面 → 小组件 → Andi")); });
        shortcuts.addView(text(L.t("长按小组件后拖动边框，可调整大小。"),13,MUTED));
        button(shortcuts,L.t("添加快捷设置按钮"),()->{
            if(Build.VERSION.SDK_INT>=33) getSystemService(StatusBarManager.class).requestAddTileService(new ComponentName(this,ProxyTile.class),L.t("Andi 代理"),Icon.createWithResource(this,R.drawable.ic_launcher),getMainExecutor(),r->toast(L.t("可在下拉快捷设置中使用")));
            else toast(L.t("下拉快捷设置 → 编辑 → 添加 Andi"));
        });
        LinearLayout advanced=fold(content,L.t("高级设置"));
        localAuth=toggle(advanced,L.t("共享代理需要认证"),draft.localAuth);
        field(advanced,"localUsername",L.t("代理用户名"),draft.localUsername,false,false);
        field(advanced,"localPassword",L.t("代理密码（至少 8 位）"),draft.localPassword,true,false);
        advanced.addView(text(L.t("这是代理认证，与 Wi-Fi 密码不同。"),13,MUTED));
        button(advanced,L.t("保存认证设置"),()->save(true));
        button(advanced,L.t("设备诊断"),()->worker.execute(()->{ String r=DeviceDiagnostics.report(CoreRunner.binary(this)); runOnUiThread(()->help(L.t("设备诊断"),r)); }));
        LinearLayout about=card(L.t("关于"));
        about.addView(text(L.t("Andi Proxy · 无需 root 的热点代理共享。"),14,INK));
        button(about,L.t("开源信息"),()->startActivity(new Intent(this,AboutActivity.class)));
        button(about,L.t("查看源代码"),()->openUrl(BuildConfig.PROJECT_URL));
        button(content,L.t("怎么使用"),this::usageGuide);
    }
    private void help(String title,String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(L.t("知道了"),null).show(); }
    private void usageGuide() {
        new AlertDialog.Builder(this).setTitle(L.t("你想怎么用？")).setItems(new String[]{L.t("只代理这台手机"),L.t("让其他设备通过手机上网"),L.t("Wi-Fi 名称和密码")},(d,w)->{
            if(w==0) help(L.t("本机代理 · 两步"),L.t("1. 在“服务器”添加或导入配置。\n2. 回到首页，点击“开启本机代理”，允许 VPN 连接。\n\n“规则”决定哪些连接直连或走代理。“不代理的应用”可排除整款应用。无需开启热点。设置保存后会自动应用。 "));
            else if(w==1) connectionGuide();
            else { tab=3; render(); }
        }).show();
    }
    private void connectionGuide() {
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(8),dp(20),dp(8));
        body.addView(text(L.t("1  连接 Wi-Fi"),18,INK));
        String wifi=ProxyService.wifiName;
        body.addView(text(ProxyService.isSharing()?(wifi.isEmpty()?L.t("连接手机的系统热点，或与手机处于同一 Wi-Fi。"):L.f("热点名称：%s",wifi)):L.t("先在首页开启 Wi-Fi 共享。"),14,MUTED));
        if(!wifi.isEmpty()) {
            TextView pass=text(L.t("密码：••••••••"),14,INK); body.addView(pass);
            Switch show=toggle(body,L.t("显示 Wi-Fi 密码"),false); show.setOnCheckedChangeListener((v,checked)->pass.setText(L.f("密码：%s",checked?ProxyService.wifiPassword:"••••••••")));
            button(body,L.t("复制 Wi-Fi 密码"),()->copy(L.t("Wi-Fi 密码"),ProxyService.wifiPassword));
        }
        body.addView(text(L.t("2  设置代理"),18,INK));
        body.addView(text(L.t("客户端 Wi-Fi 详情 → 代理 → 手动"),14,MUTED));
        if(ProxyService.isSharing()) {
            body.addView(text(L.f("服务器：%s\n端口：2080",ProxyService.address),18,INK));
            button(body,L.t("复制代理地址"),()->copy(L.t("代理地址"),ProxyService.address+":2080"));
        }
        body.addView(text(L.t("3  打开网页"),18,INK));
        body.addView(text(L.t("连接会按“规则”直连或走代理。Wi-Fi 提示无互联网时，选择保持连接。"),14,MUTED));
        LinearLayout more=fold(body,L.t("其他客户端")); more.addView(text(L.t("支持 SOCKS5 的客户端使用相同地址和端口。系统 HTTP 代理只对遵守此设置的应用生效；游戏和全设备代理需要客户端代理工具。"),13,MUTED));
        ScrollView wrapper=new ScrollView(this); wrapper.addView(body);
        new AlertDialog.Builder(this).setTitle(L.t("连接其他设备")).setView(wrapper).setPositiveButton(L.t("完成"),null).show();
    }
    private void systemHotspot() {
        try { startActivity(new Intent("android.settings.WIFI_AP_SETTINGS")); }
        catch(Exception e) {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            toast(L.t("系统设置 → 移动网络 → 个人热点"));
        }
    }
    private void openPicker() { if(save(false)) startActivity(new Intent(this,AppPickerActivity.class)); }
    private void stopSharing() { startService(new Intent(this,ProxyService.class).setAction(ProxyService.STOP_SHARE)); }
    private void togglePhone() {
        if(ProxyService.busy) return;
        collect(); draft.vpnEnabled=!LocalVpnService.active;
        if(!save(false)) return;
        if(draft.vpnEnabled) begin(false,true);
        else startService(new Intent(this,ProxyService.class).setAction(ProxyService.VPN_CHANGED));
    }
    private void updateSubscription() {
        if(updating) { toast(L.t("订阅正在更新")); return; }
        if(!save(false)) return;
        updating=true; if(subscriptionStatus!=null) subscriptionStatus.setText(L.t("正在下载规则与地区库…"));
        final Profile current; try { current=Profile.fromJson(draft.toJson()); } catch(Exception e) { updating=false; return; }
        worker.execute(()->{ String message; boolean success;
            try { SubscriptionStore.Snapshot result=new SubscriptionStore(this).update(current); message=L.f("更新成功 · %s",result.rules.summary()); success=true; }
            catch(Exception e) { message=L.f("更新失败，原缓存保留：%s",e.getMessage()); success=false; }
            final String output=message; final boolean ok=success;
            runOnUiThread(()->{ updating=false; if(subscriptionStatus!=null) subscriptionStatus.setText(output); if(ok&&ProxyService.active) startService(new Intent(this,ProxyService.class).setAction(ProxyService.APPLY)); toast(ok?L.t("规则已更新"):output); });
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data) { super.onActivityResult(request,result,data); if(request==9) { if(result==RESULT_OK) begin(pendingCreate,pendingDeviceOnly); else toast(L.t("本机代理需要系统 VPN 授权")); } }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); handleQuickAction(intent); }
    private void handleQuickAction(Intent intent) {
        String action=intent.getStringExtra("quick_action"); intent.removeExtra("quick_action");
        if(action==null) return;
        handler.post(()->{ if(action.equals("stop")) startService(new Intent(this,ProxyService.class).setAction(ProxyService.STOP));
            else if(action.equals("start_hotspot")||action.equals("share_toggle")) { if(ProxyService.isSharing()) stopSharing(); else begin(true,false); }
            else if(action.equals("vpn_toggle")) togglePhone();
            else if(action.equals("start_default")) { if(!LocalVpnService.active) togglePhone(); } });
    }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
    private String value(String key,String fallback) { EditText e=fields.get(key); return e==null?fallback:e.getText().toString(); }
    private void collect() {
        if(protocol!=null) draft.protocol=protocol.getSelectedItemPosition()==1?"hysteria2":"socks";
        draft.host=value("host",draft.host).trim(); try { draft.port=Integer.parseInt(value("port",Integer.toString(draft.port))); } catch(Exception e) { draft.port=0; }
        draft.username=value("username",draft.username); draft.password=value("password",draft.password); draft.sni=value("sni",draft.sni).trim(); draft.obfs=value("obfs",draft.obfs);
        draft.localUsername=value("localUsername",draft.localUsername); draft.localPassword=value("localPassword",draft.localPassword);
        draft.directDomains=value("directDomains",draft.directDomains); draft.proxyDomains=value("proxyDomains",draft.proxyDomains); draft.directCidrs=value("directCidrs",draft.directCidrs);
        draft.subscriptionUrl=value("subscriptionUrl",draft.subscriptionUrl).trim(); draft.hotspotName=value("hotspotName",draft.hotspotName); draft.hotspotPassword=value("hotspotPassword",draft.hotspotPassword);
        if(subscriptionEnabled!=null) draft.subscriptionEnabled=subscriptionEnabled.isChecked(); if(autoUpdate!=null) draft.subscriptionAutoUpdate=autoUpdate.isChecked(); if(customHotspot!=null) draft.customHotspot=customHotspot.isChecked(); if(vpnEnabled!=null) draft.vpnEnabled=vpnEnabled.isChecked();
        if(hotspotBand!=null) draft.hotspotBand=hotspotBand.getSelectedItemPosition(); if(theme!=null) draft.theme=theme.getSelectedItemPosition();
        if(insecure!=null) draft.insecure=insecure.isChecked(); if(socksUdp!=null) draft.socksUdp=socksUdp.isChecked(); if(localAuth!=null) draft.localAuth=localAuth.isChecked();
        if(defaultRoute!=null) draft.defaultRoute=defaultRoute.getSelectedItemPosition()==1?"direct":"proxy";
        if(network!=null) draft.iface=interfaceNames.get(network.getSelectedItemPosition());
    }
    private boolean save(boolean apply) {
        collect(); try {
            if(tab==1) draft.validateServer(); else if(tab==2) draft.validateRules();
            draft.validateSettings(); if(apply&&ProxyService.active) draft.validate(); new ProfileStore(this).save(draft);
            if(apply&&ProxyService.active) { startService(new Intent(this,ProxyService.class).setAction(ProxyService.APPLY)); toast(L.t("已保存，正在应用")); }
            else toast(L.t("已保存"));
            return true;
        } catch(Exception e) { toast(e.getMessage()); return false; }
    }
    private void begin(boolean create,boolean deviceOnly) {
        if(ProxyService.busy) { toast(L.t("正在连接，请稍候")); return; }
        collect();
        if(deviceOnly) draft.vpnEnabled=true;
        else draft.vpnEnabled=LocalVpnService.active;
        try { draft.validate(); new ProfileStore(this).save(draft); }
        catch(Exception e) { toast(e.getMessage()); if(draft.host.isEmpty()) go(1); return; }
        pendingCreate=create; pendingDeviceOnly=deviceOnly;
        if(draft.vpnEnabled) { Intent permission=VpnService.prepare(this); if(permission!=null) { startActivityForResult(permission,9); return; } }
        if(create) {
            String permission=Build.VERSION.SDK_INT>=33?Manifest.permission.NEARBY_WIFI_DEVICES:Manifest.permission.ACCESS_FINE_LOCATION;
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{permission}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},8); return; }
        }
        if(deviceOnly&&ProxyService.active) startService(new Intent(this,ProxyService.class).setAction(ProxyService.VPN_CHANGED));
        else startForegroundService(new Intent(this,ProxyService.class).setAction(ProxyService.START).putExtra("createHotspot",create).putExtra("deviceOnly",deviceOnly));
        handler.postDelayed(this::refresh,300);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==8) { if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED) begin(pendingCreate,pendingDeviceOnly); else toast(L.t("创建热点需要附近设备或位置权限")); }
    }
    private void importProfile() {
        EditText input=new EditText(this); input.setHint(L.t("粘贴 HY2 / SOCKS5 JSON 配置")); input.setMinLines(5); input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(L.t("导入服务器配置")).setView(input).setNegativeButton(L.t("取消"),null).setPositiveButton(L.t("导入"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try { collect(); Profile remote=ProfileImporter.parse(input.getText().toString()); org.json.JSONObject existing=draft.toJson(), incoming=remote.toJson();
                for(String key:new String[]{"protocol","host","port","username","password","sni","obfs","socksUdp","insecure"}) existing.put(key,incoming.get(key));
                Profile imported=Profile.fromJson(existing);
                draft=imported; new ProfileStore(this).save(draft); dialog.dismiss(); render(); toast(L.t("配置已导入并加密保存"));
            } catch(Exception e) { toast(L.f("导入失败：%s",e.getMessage())); }
        })); dialog.show();
    }
    private void copy(String label,String value) { getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(label,value)); toast(L.t("已复制")); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url))); } catch(Exception e) { toast(url); } }
    private void refresh() {
        if(state!=null) state.setText(L.t(ProxyService.status));
        if(description!=null) {
            String desc;
            if(ProxyService.failure) desc=ProxyService.detail;
            else if(!ProxyService.exitIp.isEmpty()) desc=L.f("出口 · %s",ProxyService.exitIp);
            else if(draft.host.isEmpty()) desc=L.t("未添加服务器");
            else desc=(draft.protocol.equals("hysteria2")?"HY2":"SOCKS5")+" · "+draft.host;
            description.setText(desc);
        }
        if(phoneState!=null) phoneState.setText(L.t(LocalVpnService.active?"已开启 · 按规则分流":"关闭 · 使用原来的网络"));
        if(shareState!=null) shareState.setText(ProxyService.isSharing()?(ProxyService.wifiName.isEmpty()?L.t("已有网络"):ProxyService.wifiName)+"\n"+ProxyService.address+":2080":L.t("开启后，其他设备可以连接并设置代理。"));
        if(phoneButton!=null) { phoneButton.setText(L.t(LocalVpnService.active?"停止本机代理":"开启本机代理")); phoneButton.setEnabled(!ProxyService.busy); }
        if(shareButton!=null) { shareButton.setText(L.t(ProxyService.isSharing()?"停止 Wi-Fi 共享":"开启 Wi-Fi 共享")); shareButton.setEnabled(!ProxyService.busy); }
        if(connectButton!=null) connectButton.setVisibility(ProxyService.isSharing()?View.VISIBLE:View.GONE);
        if(testButton!=null) testButton.setEnabled(ProxyService.active&&!ProxyService.busy);
    }
}
