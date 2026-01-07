package com.example.Pickbyvision.Consolidated_Pick;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Pickbyvision.R;

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

    // ===================== API endpoints & key =====================
    private static final String GET_CONSOLIDATED_STATUS_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/api/ConsolidatedPicking/GET_CONSOLIDATED_STATUS";
    private static final String OUTBOUND_ALLOTED_TIME_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/GET_OUTBOUND_ALLOTED_TIME";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";

    // ===================== HTTP =====================
    private OkHttpClient httpClient;
    private Call inFlightTimingCall;
    private Call inFlightAllottedCall;

    // ===================== Intent / IDs =====================
    private static final int KEYCODE_NEXT_SUMMARY = KeyEvent.KEYCODE_F9;
    private static final int KEYCODE_VOICE_NEXT   = KeyEvent.KEYCODE_F7;
    private static final String TAG = "SummaryActivity";

    private com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient vuzixSpeechClient;

    private String loginId;     // from Intent (fallback RAHUL)
    private String batchId = "";
    private String prinCode = "";
    private String companyCode = "";
    private String pickUser = "";

    // ===================== Summarized values from previous screen =====================
    private int pickQty = 0;              // total picks (PDA scans)
    private int totalQty = 0;             // max total quantity (original total)

    // ===================== Timing/Goal =====================
    private double totalActiveMinutes = 0.0;  // from GET_CONSOLIDATED_STATUS
    private double allocatedTime = 0.0;       // from GET_OUTBOUND_ALLOTED_TIME

    // We only compute when BOTH have arrived.
    private boolean timeLoaded = false;
    private boolean goalLoaded = false;

    // ===================== UI =====================
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

        setupVoiceCommands();

        // Kick both calls in parallel
        fetchConsolidatedStatus();
        if (prinCode != null && !prinCode.isEmpty()) {
            fetchAllottedTime(prinCode);
        } else {
            Log.e(TAG, "Missing prin_code from Intent");
            tvGoal.setText("Goal: Missing prin_code");
        }
    }

    private void setupVoiceCommands() {
        try {
            vuzixSpeechClient = new com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient(this);

            // Clean up any existing phrases first
            vuzixSpeechClient.deletePhrase("Next");
            vuzixSpeechClient.deletePhrase("Next Order");
            vuzixSpeechClient.deletePhrase("Go to Next");
            vuzixSpeechClient.deletePhrase("OK");
            vuzixSpeechClient.deletePhrase("Ok");
            vuzixSpeechClient.deletePhrase("Okay");
            vuzixSpeechClient.deletePhrase("CLOSE");
            vuzixSpeechClient.deletePhrase("Close");
            vuzixSpeechClient.deletePhrase("GO BACK");
            vuzixSpeechClient.deletePhrase("BACK");
            vuzixSpeechClient.deletePhrase("Go Back");

            // Register voice commands for next button
            vuzixSpeechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_SUMMARY);
            vuzixSpeechClient.insertKeycodePhrase("Next Order", KEYCODE_NEXT_SUMMARY);
            vuzixSpeechClient.insertKeycodePhrase("Go to Next", KEYCODE_NEXT_SUMMARY);

            Log.i(TAG, "Voice commands registered successfully for summary screen");
            // Optional: show a quick toast to confirm voice commands are ready
            // Toast.makeText(this, "Voice commands ready - say 'Next'", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient in summary", e);
            // Don't show toast here to avoid interrupting the summary display
        }
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

        // PDA_QUANTITY must be the scan count (pickQty) and MAX_QUANTITY must be original total (totalQty)
        nextIntent.putExtra("PDA_QUANTITY", pickQty);
        nextIntent.putExtra("MAX_QUANTITY", totalQty);

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

    /** Parse "HH:MM:SS" or "MM:SS" or a decimal string to minutes. */
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
        if (loginId == null) loginId = "RAHUL";
        if (pickUser == null || pickUser.trim().isEmpty()) pickUser = loginId;

        // New: read PDA_QUANTITY (scan count) and MAX_QUANTITY (original total).
        // Backward-compatible: fall back to TOTAL_QUANTITY_SUM when MAX_QUANTITY absent.
        // ✅ Use SUCCESSFUL_PICKS_COUNT as total picks
// ✅ Use TOTAL_QUANTITY_SUM as total quantity
        int successfulPicks = intent.getIntExtra("SUCCESSFUL_PICKS_COUNT",
                intent.getIntExtra("PDA_QUANTITY", 0));        // optional fallback to old key
        int totalQuantitySum = intent.getIntExtra("TOTAL_QUANTITY_SUM",
                intent.getIntExtra("MAX_QUANTITY", 0));        // optional fallback to old key

        pickQty = successfulPicks;
        totalQty = totalQuantitySum;

        tvPicks.setText("Picks: " + pickQty);
        tvQuantity.setText("Quantity: " + totalQty);

        Log.d(TAG, "=== Summary Init === login=" + loginId + " batch=" + batchId +
                " prin=" + prinCode + " company=" + companyCode +
                " picks(SUCCESSFUL_PICKS_COUNT)=" + pickQty +
                " totalQuantitySum=" + totalQty);
    }


        /** GET_CONSOLIDATED_STATUS → total_active_minutes */
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
                    GET_CONSOLIDATED_STATUS_URL,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(batchId, StandardCharsets.UTF_8.toString())
            );

            Log.d(TAG, "GET_CONSOLIDATED_STATUS → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightTimingCall != null) inFlightTimingCall.cancel();
            inFlightTimingCall = httpClient.newCall(request);

            inFlightTimingCall.enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Timing API failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvTime.setText("Time: Network Error");
                        // don't compute
                    });
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

                            // Handle string "HH:MM:SS" or numeric minutes
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
                    OUTBOUND_ALLOTED_TIME_URL,
                    URLEncoder.encode(prin, StandardCharsets.UTF_8.toString())
            );
            Log.d(TAG, "GET_OUTBOUND_ALLOTED_TIME → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightAllottedCall != null) inFlightAllottedCall.cancel();
            inFlightAllottedCall = httpClient.newCall(request);

            inFlightAllottedCall.enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Goal API failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvGoal.setText("Goal: Network Error");
                    });
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

                            // Be robust to casing typos:
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

    /**
     * Performance (%)
     * - If goal <= 0: show 0.00% when active==0, otherwise N/A (cannot score yet)
     * - Else if |active-goal| ≤ ε: 100%
     * - Else (active/goal)*100
     */
    private void calculatePerformance() {
        final double active = totalActiveMinutes;   // actual time taken
        final double goal   = allocatedTime;        // allocated time



        Log.d(TAG, "calculatePerformance (aligned) active=" + active + " goal=" + goal);
        if (active > 0 && goal > 0) {
            int performance;
            if (active <= goal) {
                performance = 100; // finished within allocation

            } else {
                float ratio = (float) active / (float) goal;
                performance = Math.max(Math.round(ratio * 100f), 0);
            }
            tvPerformance.setText(performance + "%");
        } else {
            tvPerformance.setText("N/A");
        }




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up voice commands
        if (vuzixSpeechClient != null) {
            try {
                vuzixSpeechClient.deletePhrase("Next");
                vuzixSpeechClient.deletePhrase("Next Order");
                vuzixSpeechClient.deletePhrase("Go to Next");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up voice commands", e);
            }
        }

        // Your existing cleanup
        if (inFlightTimingCall != null) inFlightTimingCall.cancel();
        if (inFlightAllottedCall != null) inFlightAllottedCall.cancel();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown received in summary: keyCode=" + keyCode);

        // Handle voice commands for next button
        switch (keyCode) {
            case KEYCODE_NEXT_SUMMARY:
            case KEYCODE_VOICE_NEXT:
                if (btnNext != null && btnNext.isEnabled() && !navigating) {
                    Log.i(TAG, "Voice command triggered - navigating to next order");
                    btnNext.performClick();
                    // Optional: provide audio/visual feedback
                    // Toast.makeText(this, "Going to next order...", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(TAG, "Next button not available or navigation in progress");
                    Toast.makeText(this, "Next not available yet", Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onBackPressed() {

        // If camera/scanner is open -> just close scanner UI


        // If Short UI is open -> close it (optional, but recommended)


        // 🚫 Block navigation to previous screen (hardware back included)
        Log.d(TAG, "Back pressed – blocked (no navigation)");
        Toast.makeText(this, "Back disabled on this screen", Toast.LENGTH_SHORT).show();

        // DO NOT call super.onBackPressed();
    }

    private static String n(String s) { return s == null ? "" : s; }
}
