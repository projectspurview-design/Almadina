package com.example.supportapp.Pick_Consolidated;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.R;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CompletedordersummaryconsolidatedActivity extends AppCompatActivity {

    // ----- APIs -----
    private static final String GET_CONSOLIDATED_STATUS_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/api/ConsolidatedPicking/GET_CONSOLIDATED_STATUS";

    // Replaces the old GET_OUTBOUND_ALLOTED_TIME:
    private static final String COMPLETE_CONSOLIDATED_PICKING_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/api/ConsolidatedPicking/COMPLETE_CONSOLIDATED_PICKING";

    // ----- Auth -----
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";

    // ----- HTTP -----
    private OkHttpClient httpClient;
    private Call inFlightTimingCall;
    private Call inFlightCompleteCall;

    // ----- Intent / State -----
    private String loginId     = "RAHUL";
    private String batchId     = "";
    private String prinCode    = ""; // not needed for new goal API, kept for logging
    private String companyCode = "";
    private String pickUser    = "";

    // Summary (already summated in previous screen)
    private int    pickQty = 0;            // total picks
    private int    totalQty = 0;           // total quantity
    private double totalActiveMinutes = 0; // from GET_CONSOLIDATED_STATUS
    private double allocatedTime     = 0;  // from COMPLETE_CONSOLIDATED_PICKING (when complete)

    // UI
    private TextView tvPicks, tvQuantity, tvTime, tvGoal, tvPerformance;
    private Button btnNext;

    private boolean navigating = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completedordersummaryconsolidated);

        initializeUI();
        initializeHttpClient();
        readIntentExtras();

        // Fetch time (active minutes) immediately
        fetchConsolidatedStatus();

        // Attempt to complete & maybe get goal (allocated time) — only returns when all picked
        if (loginId != null && !loginId.isEmpty() && batchId != null && !batchId.isEmpty()) {
            tryCompleteAndMaybeGetGoal(loginId, batchId);
        } else {
            Log.e(TAG, "Missing loginId or batchId");
            tvGoal.setText("Goal: Missing login/batch id");
        }
    }

    private void initializeUI() {
        tvPicks       = findViewById(R.id.tvPicks);
        tvQuantity    = findViewById(R.id.tvQuantity);
        tvTime        = findViewById(R.id.tvTime);
        tvGoal        = findViewById(R.id.tvGoal);
        tvPerformance = findViewById(R.id.tvPerformanceValue);
        btnNext       = findViewById(R.id.btnNext);

        // Optional: keep Next enabled as before.
        // If you want to only allow Next after completion, set false here and enable later.
        // btnNext.setEnabled(false);

        btnNext.setOnClickListener(v -> navigateToNextOrder());
    }

    private void navigateToNextOrder() {
        if (navigating) return;
        navigating = true;
        btnNext.setEnabled(false);

        Intent nextIntent = new Intent(
                CompletedordersummaryconsolidatedActivity.this,
                NextorderconsolidatedActivity.class
        );

        nextIntent.putExtra("TRANS_BATCH_ID", batchId);
        nextIntent.putExtra("COMPANY_CODE",   companyCode);
        nextIntent.putExtra("PRIN_CODE",      prinCode);
        nextIntent.putExtra("PICK_USER",      pickUser);
        nextIntent.putExtra("LOGIN_ID",       loginId);
        nextIntent.putExtra("PDA_QUANTITY",   pickQty);
        nextIntent.putExtra("MAX_QUANTITY",   totalQty);

        startActivity(nextIntent);
        finish();
    }

    private void initializeHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "SSL setup failed: " + e.getMessage());
            httpClient = new OkHttpClient();
        }
    }

    /** Read extras and show pre-summated picks/quantity. */
    private void readIntentExtras() {
        loginId     = getIntent().getStringExtra("LOGIN_ID");
        batchId     = getIntent().getStringExtra("BATCH_ID");
        prinCode    = getIntent().getStringExtra("PRIN_CODE");
        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        pickUser    = getIntent().getStringExtra("PICK_USER");

        if (loginId == null) loginId = "RAHUL";
        if (pickUser == null || pickUser.trim().isEmpty()) pickUser = loginId;

        pickQty  = getIntent().getIntExtra("PDA_QUANTITY", 0);
        totalQty = getIntent().getIntExtra("MAX_QUANTITY", 0);

        tvPicks.setText("Picks: " + pickQty);
        tvQuantity.setText("Quantity: " + totalQty);

        Log.d(TAG, "=== Summary Screen Initialized ===");
        Log.d(TAG, "  LOGIN_ID: " + loginId);
        Log.d(TAG, "  BATCH_ID: " + batchId);
        Log.d(TAG, "  PRIN_CODE: " + prinCode);
        Log.d(TAG, "  COMPANY_CODE: " + companyCode);
        Log.d(TAG, "  ✅ SUMMATED PDA_QUANTITY (Picks): " + pickQty);
        Log.d(TAG, "  ✅ SUMMATED MAX_QUANTITY (Quantity): " + totalQty);
    }

    /**
     * GET_CONSOLIDATED_STATUS
     * as_login_id, as_trans_batch_id
     * Expects JSON with "total_active_minutes".
     */
    private void fetchConsolidatedStatus() {
        if (loginId == null || batchId == null || batchId.isEmpty()) {
            Log.e(TAG, "Missing required parameters for GET_CONSOLIDATED_STATUS");
            tvTime.setText("Time: N/A");
            tvPerformance.setText("N/A");
            return;
        }

        try {
            final String url = String.format(
                    "%s?as_login_id=%s&as_trans_batch_id=%s",
                    GET_CONSOLIDATED_STATUS_URL,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(batchId, StandardCharsets.UTF_8.toString())
            );

            Log.d(TAG, "Fetching consolidated status → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightTimingCall != null) inFlightTimingCall.cancel();
            inFlightTimingCall = httpClient.newCall(request);

            inFlightTimingCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "GET_CONSOLIDATED_STATUS failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvTime.setText("Time: Network Error");
                        tvPerformance.setText("N/A");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String resBody = response.body() != null ? response.body().string() : "";
                    final int statusCode = response.code();

                    Log.d(TAG, "GET_CONSOLIDATED_STATUS status: " + statusCode);
                    Log.d(TAG, "GET_CONSOLIDATED_STATUS response: " + resBody);

                    runOnUiThread(() -> {
                        try {
                            if (statusCode != 200) {
                                tvTime.setText("Time: API Error (" + statusCode + ")");
                                return;
                            }
                            if (resBody == null || resBody.trim().isEmpty()) {
                                tvTime.setText("Time: Empty Response");
                                return;
                            }
                            if (!resBody.trim().startsWith("{")) {
                                tvTime.setText("Time: Invalid Response");
                                return;
                            }

                            JSONObject json = new JSONObject(resBody);

                            if (json.has("total_active_minutes")) {
                                Object activeMinObj = json.get("total_active_minutes");
                                if (activeMinObj instanceof Number) {
                                    totalActiveMinutes = ((Number) activeMinObj).doubleValue();
                                } else if (activeMinObj instanceof String) {
                                    totalActiveMinutes = Double.parseDouble(((String) activeMinObj).trim());
                                }

                                tvTime.setText(String.format(Locale.US, "Time: %.2f min", totalActiveMinutes));
                                Log.d(TAG, "✅ Total Active Minutes: " + totalActiveMinutes);

                                // Only compute performance if we already obtained allocatedTime
                                if (allocatedTime > 0) {
                                    calculatePerformance();
                                } else {
                                    tvPerformance.setText("N/A");
                                }
                            } else {
                                tvTime.setText("Time: Missing Field");
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Parse error in GET_CONSOLIDATED_STATUS: " + e.getMessage(), e);
                            tvTime.setText("Time: Parse Error");
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error building GET_CONSOLIDATED_STATUS request: " + e.getMessage(), e);
            tvTime.setText("Time: Error");
        }
    }

    /**
     * Attempts to complete consolidated picking.
     * - If NOT complete → typically non-200 with plain text like:
     *     "Cannot complete - 6 items still need to be picked"
     *   We show that message in Goal and keep Performance = N/A.
     * - If complete (200) → JSON with { "outbounD_ALLOCATED_TIME": "60" }.
     *   We display Goal and (optionally) compute performance if time is known.
     */
    private void tryCompleteAndMaybeGetGoal(String loginId, String transBatchId) {
        try {
            final String url = String.format(
                    "%s?as_login_id=%s&as_trans_batch_id=%s",
                    COMPLETE_CONSOLIDATED_PICKING_URL,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(transBatchId, StandardCharsets.UTF_8.toString())
            );

            Log.d(TAG, "Trying COMPLETE_CONSOLIDATED_PICKING → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightCompleteCall != null) inFlightCompleteCall.cancel();
            inFlightCompleteCall = httpClient.newCall(request);

            inFlightCompleteCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "COMPLETE_CONSOLIDATED_PICKING failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvGoal.setText("Goal: Network Error");
                        tvPerformance.setText("N/A");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String resBody = (response.body() != null) ? response.body().string() : "";
                    final int statusCode = response.code();

                    Log.d(TAG, "COMPLETE_CONSOLIDATED_PICKING status: " + statusCode);
                    Log.d(TAG, "COMPLETE_CONSOLIDATED_PICKING response: " + resBody);

                    runOnUiThread(() -> {
                        try {
                            if (statusCode != 200) {
                                // Often plain text error when not complete yet.
                                String msg = (resBody == null || resBody.trim().isEmpty())
                                        ? ("API Error (" + statusCode + ")")
                                        : resBody.trim();
                                tvGoal.setText("Goal: " + msg);
                                tvPerformance.setText("N/A");
                                allocatedTime = 0.0;

                                // If you want to lock Next until completion, keep it disabled:
                                // btnNext.setEnabled(false);
                                return;
                            }

                            // Success → expect JSON with outbounD_ALLOCATED_TIME
                            if (resBody != null && resBody.trim().startsWith("{")) {
                                JSONObject json = new JSONObject(resBody);
                                if (json.has("outbounD_ALLOCATED_TIME")) {
                                    String allocStr = json.optString("outbounD_ALLOCATED_TIME", "0");
                                    allocatedTime = Double.parseDouble(allocStr.trim());

                                    tvGoal.setText(String.format(Locale.US, "Goal: %.2f min", allocatedTime));
                                    Log.d(TAG, "✅ Allocated Minutes: " + allocatedTime);

                                    // If time already known, compute; else leave N/A
                                    if (totalActiveMinutes > 0) {
                                        calculatePerformance();
                                    } else {
                                        tvPerformance.setText("N/A");
                                    }

                                    // If you locked Next earlier, enable it now:
                                    // btnNext.setEnabled(true);
                                } else {
                                    tvGoal.setText("Goal: Missing allocated time");
                                    tvPerformance.setText("N/A");
                                }
                            } else {
                                tvGoal.setText("Goal: Invalid Response");
                                tvPerformance.setText("N/A");
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Parse error in COMPLETE_CONSOLIDATED_PICKING: " + e.getMessage(), e);
                            tvGoal.setText("Goal: Parse Error");
                            tvPerformance.setText("N/A");
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error building COMPLETE_CONSOLIDATED_PICKING request: " + e.getMessage(), e);
            tvGoal.setText("Goal: Error");
            tvPerformance.setText("N/A");
        }
    }

    /** Performance = (Active Minutes / Allocated Minutes) * 100 */
    private void calculatePerformance() {
        if (allocatedTime > 0 && totalActiveMinutes > 0) {
            double performancePercent = (totalActiveMinutes / allocatedTime) * 100.0;
            Log.d(TAG, String.format(Locale.US,
                    "✅ Performance: %.2f%% (Active=%.2f, Allocated=%.2f)",
                    performancePercent, totalActiveMinutes, allocatedTime));
            tvPerformance.setText(String.format(Locale.US, "%.2f%%", performancePercent));
        } else {
            tvPerformance.setText("N/A");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (inFlightTimingCall != null) inFlightTimingCall.cancel();
        if (inFlightCompleteCall != null) inFlightCompleteCall.cancel();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }
}
