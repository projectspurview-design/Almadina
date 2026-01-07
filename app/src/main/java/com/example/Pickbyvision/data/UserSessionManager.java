package com.example.Pickbyvision.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class UserSessionManager {

    private static final String TAG = "UserSessionManager";

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_USER_NAME = "USER_NAME";
    private static final String KEY_LOGIN_TIME = "LOGIN_TIME";

    private UserSessionManager() {}

    public static void saveUser(Context context, String userId, String userName) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply();

        Log.d(TAG, "Session saved → USER_ID=" + userId);
    }

    public static String getUserId(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, null);
    }

    public static String getUserName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_NAME, null);
    }

    public static boolean isLoggedIn(Context context) {
        return getUserId(context) != null;
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Log.d(TAG, "Session cleared");
    }
}
