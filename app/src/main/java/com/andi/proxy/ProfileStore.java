package com.andi.proxy;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class ProfileStore {
    private static final String ALIAS="andi-profile-v1";
    private final Context context;
    public ProfileStore(Context c) { context=c.getApplicationContext(); }
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (!ks.containsAlias(ALIAS)) {
            KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            g.generateKey();
        }
        return (SecretKey)ks.getKey(ALIAS,null);
    }
    public void save(Profile p) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key());
        String iv=Base64.encodeToString(c.getIV(),Base64.NO_WRAP);
        String data=Base64.encodeToString(c.doFinal(p.toJson().toString().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if (!context.getSharedPreferences("profile",0).edit().putString("iv",iv).putString("data",data).commit()) throw new IllegalStateException(L.t("配置保存失败"));
    }
    public Profile load() throws Exception {
        var prefs=context.getSharedPreferences("profile",0);
        String data=prefs.getString("data",null); if (data==null) return new Profile();
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
        return Profile.fromJson(new JSONObject(new String(c.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8)));
    }
}
