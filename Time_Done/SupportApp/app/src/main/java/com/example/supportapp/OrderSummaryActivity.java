package com.example.supportapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OrderSummaryActivity extends AppCompatActivity {

    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    // API endpoint URLs
    private static final String GET_OUTBOUND_ALLOTED_TIME_BASE_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/GET_OUTBOUND_ALLOTED_TIME";
    private static final String GET_JOB_TIMING_BASE_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/GET_JOB_TIMING";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";

    private VuzixSpeechClient speechClient;
    private OkHttpClient httpClient;

    private String orderNumber, jobNumber, userId;
    private int initialApiTotalPicks = 0, initialApiTotalQuantity = 0;
    private int apiAllocatedMinutes = 0; // Store ONLY API allocated time
    private int actualMinutesTaken = 0; // Store actual time from API
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;

    private TextView tvPicks, tvQuantity, tvTime, tvGoal, tvPerformanceValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(OrderSummaryActivity.this);
        });

        initializeHttpClient();
        setupVoiceCommands();

        tvPicks = findViewById(R.id.tvPicks);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvTime = findViewById(R.id.tvTime);
        tvGoal = findViewById(R.id.tvGoal);
        tvPerformanceValue = findViewById(R.id.tvPerformanceValue);

        // Initialize with loading states - NO DEFAULT VALUES
        tvGoal.setText("Goal: Loading...");
        tvPerformanceValue.setText("Calculating...");

        // Get intent values
        Intent intent = getIntent();
        if (intent != null) {
            orderNumber = intent.getStringExtra("ORDER_NUMBER");
            jobNumber = intent.getStringExtra("JOB_NUMBER");
            userId = intent.getStringExtra("USER_ID");
            initialApiTotalPicks = intent.getIntExtra("INITIAL_API_TOTAL_PICKS", 0);
            initialApiTotalQuantity = intent.getIntExtra("INITIAL_API_TOTAL_QUANTITY", 0);

            tvPicks.setText("Picks: " + initialApiTotalPicks);
            tvQuantity.setText("Quantity: " + initialApiTotalQuantity);

            if (orderNumber != null && jobNumber != null && userId != null) {
                // Fetch both APIs - performance calculated ONLY when both succeed
                fetchOutboundAllotedTime(userId, orderNumber, jobNumber);
                fetchJobTiming(userId, orderNumber, jobNumber);
            } else {
                tvTime.setText("Time: N/A (Missing Info)");
                tvGoal.setText("Goal: N/A (Missing Info)");
                tvPerformanceValue.setText("N/A");
            }
        } else {
            tvTime.setText("Time: N/A (No Intent)");
            tvGoal.setText("Goal: N/A (No Intent)");
            tvPerformanceValue.setText("N/A");
        }

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent nextIntent = new Intent(OrderSummaryActivity.this, NextOrderReadyActivity.class);
            startActivity(nextIntent);
            finish();
        });
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);
        } catch (Exception e) {
            Log.e(TAG, "Voice setup failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_NEXT_VOICE:
                Intent nextIntent = new Intent(OrderSummaryActivity.this, NextOrderReadyActivity.class);
                startActivity(nextIntent);
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(OrderSummaryActivity.this);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initializeHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            HostnameVerifier hostnameVerifier = (hostname, session) -> true;

            httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "SSL Setup failed, using default client: " + e.getMessage());
            // Fallback with basic timeout settings
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }

// Replace your fetchOutboundAllotedTime method with this diagnostic version

    private void fetchOutboundAllotedTime(String loginId, String orderNo, String jobNo) {
        try {
            String url = String.format("%s?as_login_id=%s&as_prin_code=%s&job_no=%s",
                    GET_OUTBOUND_ALLOTED_TIME_BASE_URL,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(orderNo, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(jobNo, StandardCharsets.UTF_8.toString()));

            Log.d(TAG, "=== ALLOCATED TIME API ===");
            Log.d(TAG, "URL: " + url);
            Log.d(TAG, "LoginId: " + loginId + ", OrderNo: " + orderNo + ", JobNo: " + jobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "❌ ALLOCATED TIME API FAILED: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvGoal.setText("Goal: Network Error");
                        tvPerformanceValue.setText("N/A");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Raw Response Body: [" + resBody + "]");

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            try {
                                if (resBody == null || resBody.trim().isEmpty()) {
                                    Log.e(TAG, "❌ Empty response from allocated time API");
                                    tvGoal.setText("Goal: Empty Response");
                                    tvPerformanceValue.setText("N/A");
                                    return;
                                }

                                // Trim and clean the response
                                String cleanedBody = resBody.trim();
                                Log.d(TAG, "Cleaned Response: [" + cleanedBody + "]");

                                JSONObject jsonObject = new JSONObject(cleanedBody);

                                // Log ALL keys in the JSON response
                                Log.d(TAG, "=== ALL JSON KEYS ===");
                                JSONArray keys = jsonObject.names();
                                if (keys != null) {
                                    for (int i = 0; i < keys.length(); i++) {
                                        String key = keys.getString(i);
                                        String value = jsonObject.optString(key, "null");
                                        Log.d(TAG, "Key[" + i + "]: '" + key + "' = '" + value + "'");
                                    }
                                }

                                // Try different possible field names for allocated time
                                String allotedTime = null;
                                String foundField = null;
                                String[] possibleFields = {
                                        "outbounD_ALLOCATED_TIME",  // Exact match from your curl
                                        "outbound_allocated_time",
                                        "OUTBOUND_ALLOCATED_TIME",
                                        "allocated_time",
                                        "alloted_time",
                                        "ALLOCATED_TIME"
                                };

                                for (String field : possibleFields) {
                                    if (jsonObject.has(field)) {
                                        allotedTime = jsonObject.optString(field, "0");
                                        foundField = field;
                                        Log.d(TAG, "✅ FOUND allocated time in field '" + field + "': '" + allotedTime + "'");

                                        // Update the UI with the allocated time
                                        tvGoal.setText("Goal: " + allotedTime + " min");
                                        break;
                                    } else {
                                        Log.d(TAG, "❌ Field '" + field + "' not found");
                                    }
                                }

                                // Handle case if no valid allocated time was found
                                if (allotedTime == null || allotedTime.equals("0") || allotedTime.equals("null")) {
                                    Log.e(TAG, "❌ No valid allocated time found.");
                                    tvGoal.setText("Goal: No Valid Data");
                                    tvPerformanceValue.setText("N/A");
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "❌ JSON parsing error: " + e.getMessage());
                                tvGoal.setText("Goal: JSON Parse Error");
                                tvPerformanceValue.setText("N/A");
                            }
                        } else {
                            Log.e(TAG, "❌ API ERROR: " + response.code() + " - " + resBody);
                            tvGoal.setText("Goal: Error (" + response.code() + ")");
                            tvPerformanceValue.setText("N/A");
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Request creation error: " + e.getMessage());
            tvGoal.setText("Goal: Request Error");
            tvPerformanceValue.setText("N/A");
        }
    }


    // Fetch job timing from API
    private void fetchJobTiming(String loginId, String orderNo, String jobNo) {
        try {
            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    GET_JOB_TIMING_BASE_URL,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(orderNo, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(jobNo, StandardCharsets.UTF_8.toString()));

            Log.d(TAG, "=== JOB TIMING API ===");
            Log.d(TAG, "URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "❌ JOB TIMING API FAILED: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvTime.setText("Time: Network Error");
                        tvPerformanceValue.setText("N/A");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Job Timing Response: " + response.code() + " - " + resBody);

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonObject = new JSONObject(resBody);
                                String totalActiveTime = jsonObject.optString("total_active_time", "0:00");
                                tvTime.setText("Time: " + totalActiveTime);

                                // Store actual time taken from API
                                actualMinutesTaken = parseTimeToMinutes(totalActiveTime);
                                Log.d(TAG, "✅ ACTUAL TIME SUCCESS: " + actualMinutesTaken + " minutes");

                                // Try to calculate performance if we have both values
                                calculatePerformanceFromApis();

                            } catch (JSONException e) {
                                Log.e(TAG, "❌ Job timing JSON error: " + e.getMessage());
                                tvTime.setText("Time: Parse Error");
                                tvPerformanceValue.setText("N/A");
                            }
                        } else {
                            Log.e(TAG, "❌ Job timing API error: " + response.code());
                            tvTime.setText("Time: Error (" + response.code() + ")");
                            tvPerformanceValue.setText("N/A");
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Job timing request error: " + e.getMessage());
            runOnUiThread(() -> {
                tvTime.setText("Time: Request Error");
                tvPerformanceValue.setText("N/A");
            });
        }
    }

    // Calculate performance ONLY when BOTH APIs provide valid data
    private void calculatePerformanceFromApis() {
        Log.d(TAG, "=== PERFORMANCE CALCULATION ===");
        Log.d(TAG, "API Allocated Minutes: " + apiAllocatedMinutes);
        Log.d(TAG, "Actual Minutes Taken: " + actualMinutesTaken);

        // Only calculate if we have BOTH values from APIs
        if (actualMinutesTaken > 0 && apiAllocatedMinutes > 0) {
            int performance;

            if (actualMinutesTaken <= apiAllocatedMinutes) {
                // Completed within or equal to allocated time = 100% performance
                performance = 100;
                Log.d(TAG, "✅ EXCELLENT PERFORMANCE: Completed in " + actualMinutesTaken +
                        " min vs allocated " + apiAllocatedMinutes + " min = 100%");
            } else {
                // Took more time than allocated = calculate penalty
                // Performance = (Allocated Time / Actual Time) * 100
                float ratio = (float) apiAllocatedMinutes / actualMinutesTaken;
                performance = Math.round(ratio * 100);

                // Ensure performance doesn't go below 0%
                performance = Math.max(performance, 0);

                Log.d(TAG, "⚠️ OVERTIME: Took " + actualMinutesTaken +
                        " min vs allocated " + apiAllocatedMinutes + " min = " + performance + "%");
            }

            tvPerformanceValue.setText(performance + "%");

            Log.d(TAG, "📊 FINAL PERFORMANCE: " + performance + "%");
            Log.d(TAG, "   Formula: (" + apiAllocatedMinutes + " ÷ " + actualMinutesTaken + ") × 100 = " + performance + "%");

        } else {
            Log.d(TAG, "❌ CANNOT CALCULATE PERFORMANCE - Missing API data:");
            Log.d(TAG, "   Need both values > 0");
            Log.d(TAG, "   Allocated: " + apiAllocatedMinutes + ", Actual: " + actualMinutesTaken);
            // Keep showing "Calculating..." until we have both values
        }
    }

    private int parseTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return (hours * 60) + minutes;
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse time string: " + time);
        }
        return 0;
    }
}