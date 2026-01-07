package com.example.supportapp.Consolidated_Pick.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionHelper {
    private static final String PREFS_NAME = "AppPrefs";
    private final SharedPreferences sp;

    public SessionHelper(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getUserId() {
        String v = sp.getString("CURRENT_USER_ID", null);
        if (v == null) v = sp.getString("LOGGED_IN_USER_ID", null);
        return v;
    }
}
