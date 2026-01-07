package com.example.Pickbyvision.Consolidated_Pick.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class SessionStore {
    private static final String PREF = "pbv_session";
    private final SharedPreferences sp;

    public SessionStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void put(String key, String value) {
        sp.edit().putString(key, value).apply();
    }

    public String get(String key) {
        return sp.getString(key, null);
    }

    public Map<String, String> baseQuery() {
        Map<String, String> m = new HashMap<>();
        if (get("login_id") != null) m.put("as_login_id", get("login_id"));
        if (get("warehouse_id") != null) m.put("warehouse_id", get("warehouse_id"));
        if (get("company_code") != null) m.put("company_code", get("company_code"));
        if (get("branch_code") != null) m.put("branch_code", get("branch_code"));
        return m;
    }
}
