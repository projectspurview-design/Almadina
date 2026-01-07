package com.example.Pickbyvision.Consolidated_Pick;


import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Induvidual_Pick.network.UnsafeOkHttpClient;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CompletedordersummaryconsolidatedActivity extends AppCompatActivity {


    private OkHttpClient httpClient;
    private Call inFlightTimingCall;
    private Call inFlightAllottedCall;

    private static final String TAG = "SummaryActivity";

    private String loginId;
    private String batchId = "";
    private String prinCode = "";
    private String companyCode = "";
    private String pickUser = "";

    private int pickQty = 0;
    private int totalQty = 0;

    private double totalActiveMinutes = 0.0;
    private double allocatedTime = 0.0;

    private boolean timeLoaded = false;
    private boolean goalLoaded = false;

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


        fetchConsolidatedStatus();
        if (prinCode != null && !prinCode.isEmpty()) {
            fetchAllottedTime(prinCode);
        } else {
            Log.e(TAG, "Missing prin_code from Intent");
            tvGoal.setText("Goal: Missing prin_code");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceCommandCenter.initConsolidatedTransaction(this);
        VoiceCommandCenter.initCompletedOrderSummaryConsolidated(this);
    }

    private void initializeUI() {
        tvPicks       = findViewById(R.id.tvPicks);
        tvQuantity    = findViewById(R.id.tvQuantity);
        tvTime        = findViewById(R.id.tvTime);
        tvGoal        = findViewById(R.id.tvGoal);
        tvPerformance = findViewById(R.id.tvPerformanceValue);
        btnNext       = findViewById(R.id.btnNext);
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

        nextIntent.putExtra("PDA_QUANTITY", pickQty);
        nextIntent.putExtra("MAX_QUANTITY", totalQty);

        startActivity(nextIntent);
        finish();
    }

    private void initializeHttpClient() {
        httpClient = UnsafeOkHttpClient.get();
    }


    private double parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0.0;
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length == 3) {
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int s = Integer.parseInt(parts[2]);
                return (h * 60.0) + m + (s / 60.0);
            } else if (parts.length == 2) {
                int m = Integer.parseInt(parts[0]);
                int s = Integer.parseInt(parts[1]);
                return m + (s / 60.0);
            } else {
                return Double.parseDouble(timeStr.trim());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse time '" + timeStr + "'", e);
            return 0.0;
        }
    }

    /** Read extras & show picks/quantity immediately. */
    private void readIntentExtras() {
        Intent intent = getIntent();
        loginId = intent.getStringExtra("LOGIN_ID");
        batchId = intent.getStringExtra("BATCH_ID");
        prinCode = intent.getStringExtra("PRIN_CODE");
        companyCode = intent.getStringExtra("COMPANY_CODE");
        pickUser = intent.getStringExtra("PICK_USER");
        if (pickUser == null || pickUser.trim().isEmpty()) pickUser = loginId;


        int successfulPicks = intent.getIntExtra("SUCCESSFUL_PICKS_COUNT",
                intent.getIntExtra("PDA_QUANTITY", 0));
        int totalQuantitySum = intent.getIntExtra("TOTAL_QUANTITY_SUM",
                intent.getIntExtra("MAX_QUANTITY", 0));

        pickQty = successfulPicks;
        totalQty = totalQuantitySum;

        tvPicks.setText("Picks: " + pickQty);
        tvQuantity.setText("Quantity: " + totalQty);

        Log.d(TAG, "=== Summary Init === login=" + loginId + " batch=" + batchId +
                " prin=" + prinCode + " company=" + companyCode +
                " picks(SUCCESSFUL_PICKS_COUNT)=" + pickQty +
                " totalQuantitySum=" + totalQty);
    }

    private void fetchConsolidatedStatus() {
        if (loginId == null || batchId == null || batchId.isEmpty()) {
            Log.e(TAG, "Missing params for GET_CONSOLIDATED_STATUS loginId=" + loginId + " batchId=" + batchId);
            tvTime.setText("Time: N/A");
            tvPerformance.setText("N/A");
            return;
        }

        try {
            final String url = String.format(
                    "%s?as_login_id=%s&as_trans_batch_id=%s",
                    ApiConfig.GET_CONSOLIDATED_STATUS,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(batchId, StandardCharsets.UTF_8.toString())
            );


            Log.d(TAG, "GET_CONSOLIDATED_STATUS → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_JSON)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            if (inFlightTimingCall != null) inFlightTimingCall.cancel();
            inFlightTimingCall = httpClient.newCall(request);

            inFlightTimingCall.enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Timing API failed: " + e.getMessage());
                    runOnUiThread(() -> tvTime.setText("Time: Network Error"));
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String body = response.body() != null ? response.body().string() : "";
                    final int code = response.code();
                    Log.d(TAG, "Timing API code=" + code + " body=" + body);

                    runOnUiThread(() -> {
                        try {
                            if (code != 200 || body == null || body.trim().isEmpty() || !body.trim().startsWith("{")) {
                                tvTime.setText("Time: API Error");
                                return;
                            }
                            JSONObject json = new JSONObject(body);

                            Object o = json.opt("total_active_minutes");
                            if (o instanceof Number) {
                                totalActiveMinutes = ((Number) o).doubleValue();
                            } else {
                                String s = json.optString("total_active_minutes", "00:00:00");
                                totalActiveMinutes = parseTimeToMinutes(s);
                            }

                            timeLoaded = true;
                            tvTime.setText(String.format(Locale.US, "Time: %.2f min", totalActiveMinutes));
                            maybeCalc();

                        } catch (Exception e) {
                            Log.e(TAG, "Timing parse error", e);
                            tvTime.setText("Time: Parse Error");
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Timing build error", e);
            tvTime.setText("Time: Error");
        }
    }

    /** GET_OUTBOUND_ALLOTED_TIME → outbounD_ALLOCATED_TIME (string minutes) */
    private void fetchAllottedTime(String prin) {
        try {
            final String url = String.format(
                    "%s?as_prin_code=%s",
                    ApiConfig.GET_OUTBOUND_ALLOTED_TIME,
                    URLEncoder.encode(prin, StandardCharsets.UTF_8.toString())
            );
            Log.d(TAG, "GET_OUTBOUND_ALLOTED_TIME → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_JSON)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            if (inFlightAllottedCall != null) inFlightAllottedCall.cancel();
            inFlightAllottedCall = httpClient.newCall(request);

            inFlightAllottedCall.enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Goal API failed: " + e.getMessage());
                    runOnUiThread(() -> tvGoal.setText("Goal: Network Error"));
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String body = response.body() != null ? response.body().string() : "";
                    final int code = response.code();
                    Log.d(TAG, "Goal API code=" + code + " body=" + body);

                    runOnUiThread(() -> {
                        try {
                            if (code != 200 || body == null || body.trim().isEmpty() || !body.trim().startsWith("{")) {
                                tvGoal.setText("Goal: API Error");
                                return;
                            }
                            JSONObject json = new JSONObject(body);

                            String allocStr = json.optString("outbounD_ALLOCATED_TIME",
                                    json.optString("outbound_ALLOCATED_TIME",
                                            json.optString("OUTBOUND_ALLOCATED_TIME", "0")));

                            allocatedTime = Double.parseDouble(allocStr.trim());
                            goalLoaded = true;

                            tvGoal.setText(String.format(Locale.US, "Goal: %.2f min", allocatedTime));
                            maybeCalc();

                        } catch (Exception e) {
                            Log.e(TAG, "Goal parse error", e);
                            tvGoal.setText("Goal: Parse Error");
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Goal build error", e);
            tvGoal.setText("Goal: Error");
        }
    }

    /** Only calculate when BOTH time & goal are loaded. */
    private void maybeCalc() {
        Log.d(TAG, "maybeCalc timeLoaded=" + timeLoaded + " goalLoaded=" + goalLoaded);
        if (timeLoaded && goalLoaded) {
            calculatePerformance();
        }
    }

    private void calculatePerformance() {
        final double active = totalActiveMinutes;
        final double goal   = allocatedTime;

        // Create a formatter for 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");

        Log.d(TAG, "calculatePerformance (aligned) active=" + active + " goal=" + goal);
        if (active > 0 && goal > 0) {
            float performance;
            if (active <= goal) {
                performance = 100;
            } else {
                float ratio = (float) goal / (float) active;
                performance = (ratio * 100f);
            }
            tvPerformance.setText(df.format(performance) + "%");
        } else {
            tvPerformance.setText("N/A");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (inFlightTimingCall != null) inFlightTimingCall.cancel();
        if (inFlightAllottedCall != null) inFlightAllottedCall.cancel();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown received in summary: keyCode=" + keyCode);

        VoiceCommandCenter.Actions actions = new VoiceCommandCenter.Actions() {
            @Override
            public void onNext() {
                if (btnNext != null && btnNext.isEnabled() && !navigating) {
                    Log.i(TAG, "Voice command triggered - navigating to next order");
                    btnNext.performClick();
                } else {
                    Log.w(TAG, "Next button not available or navigation in progress");
                    Toast.makeText(CompletedordersummaryconsolidatedActivity.this,
                            "Next not available yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLogout() {
                Log.i(TAG, "Voice logout triggered");
                LogoutManager.performLogout(CompletedordersummaryconsolidatedActivity.this);
            }

            @Override public void onBack() {}
            @Override public void onScrollUp() {}
            @Override public void onScrollDown() {}
            @Override public void onSelect() {}
            @Override public void onInbound() {}
            @Override public void onOutbound() {}
            @Override public void onInventory() {}
            @Override public void onIndividual() {}
            @Override public void onConsolidated() {}
        };

        if (VoiceCommandCenter.handleKeyDownCompletedOrderSummaryConsolidated(keyCode, actions)) return true;

        if (VoiceCommandCenter.handleKeyDown(keyCode, actions)) return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back pressed – blocked (no navigation)");
        Toast.makeText(this, "Back disabled on this screen", Toast.LENGTH_SHORT).show();
    }

    private static String n(String s) { return s == null ? "" : s; }

}