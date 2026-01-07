package com.example.Pickbyvision.Induvidual_Pick.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class SecureTokenStore {

    private static final String PREF = "secure_auth";
    private static final String KEY_TOKEN = "auth_token";

    private SecureTokenStore() {}

    private static SharedPreferences prefs(Context ctx) throws Exception {
        MasterKey key = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                ctx,
                PREF,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public static void saveToken(Context ctx, String token) {
        try {
            prefs(ctx).edit().putString(KEY_TOKEN, token).apply();
        } catch (Exception ignored) {}
    }

    public static String getToken(Context ctx) {
        try {
            return prefs(ctx).getString(KEY_TOKEN, null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void clear(Context ctx) {
        try {
            prefs(ctx).edit().remove(KEY_TOKEN).apply();
        } catch (Exception ignored) {}
    }
}
