package com.andi.probe;
import android.app.Activity;
import android.os.Bundle;
import android.net.*;
import android.widget.TextView;
import java.net.*;
import java.nio.charset.StandardCharsets;
/** Independent UID with no SOCKS settings: verifies Android's actual per-app VPN routing. */
public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); TextView result=new TextView(this); result.setTextSize(22); result.setPadding(40,160,40,40); result.setText("Checking the system network and HTTPS exit…"); setContentView(result);
        new Thread(()->{ String text;
            try {
                ConnectivityManager cm=getSystemService(ConnectivityManager.class); NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());
                boolean vpn=caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                HttpURLConnection conn=(HttpURLConnection)new URL("https://api.ipify.org").openConnection(); conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
                String ip; try(var in=conn.getInputStream()) { ip=new String(in.readAllBytes(),StandardCharsets.UTF_8).trim(); } finally { conn.disconnect(); }
                text="HTTPS exit: "+ip+"\nVPN transport: "+vpn+"\nNo explicit proxy configured";
            } catch(Exception e) { text="FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage(); }
            final String output=text; runOnUiThread(()->result.setText(output));
        }).start();
    }
}
