package com.example.Pickbyvision.Induvidual_Pick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.example.Pickbyvision.Induvidual_Pick.network.UnsafeOkHttpClient;

import com.example.Pickbyvision.data.UserSessionManager;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.example.Pickbyvision.voice.VoiceCommandCenter.Actions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrderSummaryActivity extends AppCompatActivity {

    private static final String TAG = "OrderSummaryActivity";
    private static final String PREFS_NAME = "AppPrefs";

    private static final String EXTRA_USER_ID = "USER_ID";
    private static final String EXTRA_USER_NAME = "USER_NAME";
    private static final String EXTRA_JOB_NUMBER = "JOB_NUMBER";
    private static final String EXTRA_ORDER_NUMBER = "ORDER_NUMBER";
    private static final String EXTRA_PRIN_CODE = "PRIN_CODE";


    private static final String EXTRA_INITIAL_API_TOTAL_PICKS = "INITIAL_API_TOTAL_PICKS";
    private static final String EXTRA_INITIAL_API_TOTAL_QUANTITY = "INITIAL_API_TOTAL_QUANTITY";


    private static final String PREF_JOB_INITIAL_TOTAL_PICKS_PREFIX = "job_initial_total_picks_";
    private static final String PREF_JOB_INITIAL_TOTAL_QTY_PREFIX = "job_initial_total_qty_";

    private TextView orderNumberText, skuText, quantityText;
    private AppCompatButton btnBack, btnNext;

    private OkHttpClient client;

    private String prinCode;
    private String jobNumber;
    private String orderNumber;
    private String loginId;
    private String userName;

    private int initialTotalPicks = 0;
    private int initialTotalQty = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ordersummaryactivity);

        VoiceCommandCenter.init(this);

        orderNumberText = findViewById(R.id.orderNumberText);
        skuText = findViewById(R.id.skuText);
        quantityText = findViewById(R.id.quantityText);
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);

        btnBack.setOnClickListener(v -> finish());
        btnNext.setOnClickListener(v -> goNext());

        findViewById(R.id.logoutButton)
                .setOnClickListener(v -> LogoutManager.performLogout(this));

        client = UnsafeOkHttpClient.get();

        loadParams();
        loadCachedTotalsIfAny();
        updateTotalsUI();
        fetchOrderSummary();
    }


    private void loadParams() {
        Intent i = getIntent();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String iPrin = i.getStringExtra(EXTRA_PRIN_CODE);
        String iJob = i.getStringExtra(EXTRA_JOB_NUMBER);
        String iOrder = i.getStringExtra(EXTRA_ORDER_NUMBER);
        String iUser = i.getStringExtra(EXTRA_USER_ID);
        String iName = i.getStringExtra(EXTRA_USER_NAME);

        String pPrin = prefs.getString(EXTRA_PRIN_CODE, "");
        String pJob = prefs.getString(EXTRA_JOB_NUMBER, "");
        String pOrder = prefs.getString(EXTRA_ORDER_NUMBER, "");
        String pUser = prefs.getString(EXTRA_USER_ID, null);
        String pName = prefs.getString(EXTRA_USER_NAME, "User");

        String sUser = UserSessionManager.getUserId(this);
        String sName = UserSessionManager.getUserName(this);

        prinCode = firstNonBlank(iPrin, pPrin);
        jobNumber = firstNonBlank(iJob, pJob);
        orderNumber = firstNonBlank(iOrder, pOrder);
        loginId = firstNonBlank(iUser, sUser, pUser);
        userName = firstNonBlank(iName, sName, pName, "User");

        Log.d(TAG, "PARAMS → PRIN=" + prinCode +
                ", JOB=" + jobNumber +
                ", ORDER=" + orderNumber +
                ", USER=" + loginId);

        if (isBlank(prinCode) || isBlank(jobNumber) || isBlank(orderNumber) || isBlank(loginId)) {
            Toast.makeText(this, "Missing order data. Please retry.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        prefs.edit()
                .putString(EXTRA_PRIN_CODE, prinCode)
                .putString(EXTRA_JOB_NUMBER, jobNumber)
                .putString(EXTRA_ORDER_NUMBER, orderNumber)
                .putString(EXTRA_USER_ID, loginId)
                .putString(EXTRA_USER_NAME, userName)
                .apply();
    }

    private void loadCachedTotalsIfAny() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!isBlank(jobNumber)) {
            initialTotalPicks = prefs.getInt(PREF_JOB_INITIAL_TOTAL_PICKS_PREFIX + jobNumber, 0);
            initialTotalQty = prefs.getInt(PREF_JOB_INITIAL_TOTAL_QTY_PREFIX + jobNumber, 0);
            Log.d(TAG, "Cached totals → picks=" + initialTotalPicks + ", qty=" + initialTotalQty);
        }
    }

    private void cacheTotals(int picks, int qty) {
        if (isBlank(jobNumber)) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putInt(PREF_JOB_INITIAL_TOTAL_PICKS_PREFIX + jobNumber, picks)
                .putInt(PREF_JOB_INITIAL_TOTAL_QTY_PREFIX + jobNumber, qty)
                .apply();
    }

    private void updateTotalsUI() {
        orderNumberText.setText("Order#: " + (orderNumber != null ? orderNumber : "-"));
        skuText.setText("SKU: " + initialTotalPicks);
        quantityText.setText("Quantity: " + initialTotalQty);
    }

    private void fetchOrderSummary() {
        try {
            String url = ApiConfig.ORDER_SUMMARY
                    + "?as_prin_code=" + URLEncoder.encode(prinCode, "UTF-8")
                    + "&as_job_no=" + URLEncoder.encode(jobNumber, "UTF-8")
                    + "&as_order_no=" + URLEncoder.encode(orderNumber, "UTF-8")
                    + "&as_login_id=" + URLEncoder.encode(loginId, "UTF-8");

            Log.d(TAG, "ORDER_SUMMARY URL → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "ORDER_SUMMARY onFailure", e);
                    runOnUiThread(() ->
                            Toast.makeText(OrderSummaryActivity.this,
                                    "Failed to load order summary",
                                    Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "ORDER_SUMMARY failed code=" + response.code());
                        return;
                    }

                    String body = response.body().string();
                    Log.d(TAG, "ORDER_SUMMARY body=" + body);

                    try {
                        JSONArray arr = new JSONArray(body);
                        if (arr.length() == 0) return;

                        JSONObject obj = arr.getJSONObject(0);

                        initialTotalPicks = obj.optInt("total_sku", 0);
                        initialTotalQty = obj.optInt("total_qty", 0);

                        cacheTotals(initialTotalPicks, initialTotalQty);

                        runOnUiThread(() -> {
                            orderNumberText.setText("Order#: " + obj.optString("order_no", orderNumber));
                            skuText.setText("SKU: " + initialTotalPicks);
                            quantityText.setText("Quantity: " + initialTotalQty);
                        });

                        Log.d(TAG, "Totals resolved → picks=" + initialTotalPicks + ", qty=" + initialTotalQty);

                    } catch (Exception e) {
                        Log.e(TAG, "ORDER_SUMMARY JSON parse error", e);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "fetchOrderSummary error", e);
        }
    }

    private void goNext() {
        Intent intent = new Intent(this, InduvidualJobActivity.class);


        intent.putExtra(EXTRA_USER_ID, loginId);
        intent.putExtra(EXTRA_USER_NAME, userName);

        intent.putExtra(EXTRA_PRIN_CODE, prinCode);
        intent.putExtra(EXTRA_JOB_NUMBER, jobNumber);
        intent.putExtra(EXTRA_ORDER_NUMBER, orderNumber);

        intent.putExtra(EXTRA_INITIAL_API_TOTAL_PICKS, initialTotalPicks);
        intent.putExtra(EXTRA_INITIAL_API_TOTAL_QUANTITY, initialTotalQty);

        startActivity(intent);
    }

    private final Actions voiceActions = new Actions() {
        @Override public void onNext() { btnNext.performClick(); }
        @Override public void onBack() { finish(); }
        @Override public void onScrollUp() {}
        @Override public void onScrollDown() {}
        @Override public void onSelect() {}
        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}
        @Override public void onLogout() {
            LogoutManager.performLogout(OrderSummaryActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)
                || super.onKeyDown(keyCode, event);
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()
                    && !v.equalsIgnoreCase("null")
                    && !v.equalsIgnoreCase("n/a")) {
                return v.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
