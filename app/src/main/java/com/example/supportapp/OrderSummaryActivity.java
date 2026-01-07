package com.example.supportapp;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.voice.VoiceCommandCenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
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

public class OrderSummaryActivity extends AppCompatActivity {

    // API endpoint URLs
    private static final String GET_OUTBOUND_ALLOTED_TIME_BASE_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/GET_OUTBOUND_ALLOTED_TIME";
    private static final String GET_JOB_TIMING_BASE_URL =
            "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/GET_JOB_TIMING";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";

    // Check for new job every 10 seconds (adjust if needed)
    private static final long CHECK_NEW_JOB_INTERVAL = 10_000L;

    private OkHttpClient httpClient;

    private Handler checkNewJobHandler;
    private Runnable checkNewJobRunnable;
    private boolean isCheckingNewJob = false;

    // Keep references so we can cancel before starting the next cycle
    private Call inFlightJobTimingCall = null;
    private Call inFlightAllottedCall = null;
    private Call inFlightNewJobCheckCall = null;

    // Data passed in
    private String orderNumber, jobNumber, userId;
    private String referenceCode, prinCode;
    private int initialApiTotalPicks = 0, initialApiTotalQuantity = 0;

    // Track current job to detect changes
    private String currentJobNumber = null;
    private String currentOrderNumber = null;

    // Derived values
    private int apiAllocatedMinutes = 0;
    private double actualMinutesTaken = 0;

    // UI Components
    private TextView tvPicks, tvQuantity, tvTime, tvGoal, tvPerformanceValue, tvLastUpdate;
    private Button nextButton, logoutButton;

    // Voice actions for this screen: handle "Next" and "Logout"
    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onNext()         { handleNextAction(); }
        @Override public void onLogout()       { LogoutManager.performLogout(OrderSummaryActivity.this); }

        // Unused on this screen (kept for interface completeness)
        @Override public void onBack()         {}
        @Override public void onScrollUp()     {}
        @Override public void onScrollDown()   {}
        @Override public void onSelect()       {}
        @Override public void onInbound()      {}
        @Override public void onOutbound()     {}
        @Override public void onInventory()    {}
        @Override public void onIndividual()   {}
        @Override public void onConsolidated() {}
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        // Centralized voice registration (idempotent)
        VoiceCommandCenter.init(this);

        // Initialize UI components
        initializeUI();

        // Initialize HTTP client
        initializeHttpClient();

        // Setup button listeners
        setupButtonListeners();

        // Handle incoming intent data
        handleIncomingIntent();

        // Start checking for new jobs
        startCheckingForNewJob();
    }

    private void initializeUI() {
        tvPicks = findViewById(R.id.tvPicks);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvTime = findViewById(R.id.tvTime);
        tvGoal = findViewById(R.id.tvGoal);
        tvPerformanceValue = findViewById(R.id.tvPerformanceValue);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        nextButton = findViewById(R.id.nextButton);
        logoutButton = findViewById(R.id.logoutButton);

        if (nextButton == null)  Log.e(TAG, "Next button not found in layout!");
        if (logoutButton == null) Log.e(TAG, "Logout button not found in layout!");

        tvGoal.setText("Goal: Loading...");
        tvPerformanceValue.setText("Calculating...");
        tvTime.setText("Time: Loading...");
        tvLastUpdate.setText("Last Update: Never");
    }

    private void setupButtonListeners() {
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                LogoutManager.performLogout(OrderSummaryActivity.this);
            });
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Log.d(TAG, "Next button clicked");
                handleNextAction();
            });
        }
    }

    private void handleNextAction() {
        Log.d(TAG, "Executing Next action - going to NextOrderReadyActivity");

        // Stop checking for new jobs
        stopCheckingForNewJob();

        Toast.makeText(this, "Going to next order", Toast.LENGTH_SHORT).show();

        try {
            Intent intent = new Intent(this, NextOrderReadyActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to NextOrderReadyActivity: " + e.getMessage());
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orderNumber != null && jobNumber != null && userId != null) {
            startCheckingForNewJob();
        }
        // Re-register phrases after lifecycle changes
        VoiceCommandCenter.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCheckingForNewJob();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCheckingForNewJob();
        cancelInFlightCalls();
    }

    // Route all key events through the centralized voice router
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            orderNumber = intent.getStringExtra("ORDER_NUMBER");
            jobNumber = intent.getStringExtra("JOB_NUMBER");
            userId = intent.getStringExtra("USER_ID");
            referenceCode = intent.getStringExtra("REFERENCE_CODE");
            prinCode = intent.getStringExtra("PRIN_CODE");
            initialApiTotalPicks = intent.getIntExtra("INITIAL_API_TOTAL_PICKS", 0);
            initialApiTotalQuantity = intent.getIntExtra("INITIAL_API_TOTAL_QUANTITY", 0);

            currentJobNumber = jobNumber;
            currentOrderNumber = orderNumber;

            tvPicks.setText("Picks: " + initialApiTotalPicks);
            tvQuantity.setText("Quantity: " + initialApiTotalQuantity);

            Log.d(TAG, "Initial job - order=" + orderNumber + ", job=" + jobNumber + ", user=" + userId
                    + ", ref=" + referenceCode + ", prin=" + prinCode);

            if (orderNumber == null || jobNumber == null || userId == null) {
                tvTime.setText("Time: N/A (Missing Info)");
                tvGoal.setText("Goal: N/A (Missing Info)");
                tvPerformanceValue.setText("N/A");
                tvLastUpdate.setText("Last Update: Error - Missing Info");
            } else {
                fetchDataForCurrentJob();
            }
        } else {
            tvTime.setText("Time: N/A (No Intent)");
            tvGoal.setText("Goal: N/A (No Intent)");
            tvPerformanceValue.setText("N/A");
            tvLastUpdate.setText("Last Update: Error - No Intent");
        }
    }

    private void startCheckingForNewJob() {
        if (isCheckingNewJob) return;

        if (checkNewJobHandler == null) checkNewJobHandler = new Handler(Looper.getMainLooper());
        checkNewJobRunnable = new Runnable() {
            @Override
            public void run() {
                checkForNewJob();
                checkNewJobHandler.postDelayed(this, CHECK_NEW_JOB_INTERVAL);
            }
        };
        isCheckingNewJob = true;
        checkNewJobHandler.post(checkNewJobRunnable);
        Log.d(TAG, "Started checking for new jobs every " + (CHECK_NEW_JOB_INTERVAL / 1000) + " seconds");
    }

    private void stopCheckingForNewJob() {
        if (!isCheckingNewJob) return;
        if (checkNewJobHandler != null && checkNewJobRunnable != null) {
            checkNewJobHandler.removeCallbacks(checkNewJobRunnable);
        }
        isCheckingNewJob = false;
        Log.d(TAG, "Stopped checking for new jobs");
    }

    private void checkForNewJob() {
        if (userId == null) return;

        try {
            String url = String.format(
                    "%s?as_login_id=%s&ts=%d",
                    GET_JOB_TIMING_BASE_URL,
                    URLEncoder.encode(userId, StandardCharsets.UTF_8.toString()),
                    System.currentTimeMillis());

            Log.d(TAG, "Checking for new job → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightNewJobCheckCall != null) inFlightNewJobCheckCall.cancel();
            inFlightNewJobCheckCall = httpClient.newCall(request);

            inFlightNewJobCheckCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "New job check failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "New job check code=" + response.code() + " body=" + resBody);

                    if (response.isSuccessful() && resBody != null && !resBody.trim().isEmpty()) {
                        try {
                            JSONObject json = new JSONObject(resBody.trim());
                            String newJobNumber = json.optString("job_no", "");
                            String newOrderNumber = json.optString("order_no", "");

                            runOnUiThread(() -> {
                                boolean jobChanged = false;

                                if (!newJobNumber.isEmpty() && !newOrderNumber.isEmpty()) {
                                    if (!newJobNumber.equals(currentJobNumber) || !newOrderNumber.equals(currentOrderNumber)) {
                                        Log.d(TAG, "NEW JOB DETECTED! Old: order=" + currentOrderNumber +
                                                ", job=" + currentJobNumber + " | New: order=" + newOrderNumber +
                                                ", job=" + newJobNumber);

                                        currentJobNumber = newJobNumber;
                                        currentOrderNumber = newOrderNumber;
                                        orderNumber = newOrderNumber;
                                        jobNumber = newJobNumber;
                                        jobChanged = true;
                                    }
                                }

                                if (jobChanged) {
                                    tvTime.setText("Time: Loading new job...");
                                    tvGoal.setText("Goal: Loading new job...");
                                    tvPerformanceValue.setText("Calculating...");
                                    fetchDataForCurrentJob();
                                }
                            });

                        } catch (JSONException je) {
                            Log.e(TAG, "New job check parse error: " + je.getMessage());
                        }
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "New job check request build error: " + e.getMessage());
        }
    }

    private void fetchDataForCurrentJob() {
        if (orderNumber == null || jobNumber == null || userId == null) return;

        Log.d(TAG, "Fetching fresh data for current job: order=" + orderNumber + ", job=" + jobNumber);

        String now = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new java.util.Date());
        tvLastUpdate.setText("Last Update: " + now);

        apiAllocatedMinutes = 0;
        actualMinutesTaken = 0;

        String codeToUse = prinCode != null ? prinCode :
                (referenceCode != null ? referenceCode : orderNumber);

        fetchOutboundAllottedTimeFresh(codeToUse);
        fetchJobTimingFresh(userId, orderNumber, jobNumber);
    }

    private void initializeHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override public boolean verify(String hostname, SSLSession session) { return true; }
            };

            httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "SSL setup failed, falling back to default client: " + e.getMessage());
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }

    private void cancelInFlightCalls() {
        try {
            if (inFlightJobTimingCall != null) {
                inFlightJobTimingCall.cancel();
                inFlightJobTimingCall = null;
            }
            if (inFlightAllottedCall != null) {
                inFlightAllottedCall.cancel();
                inFlightAllottedCall = null;
            }
            if (inFlightNewJobCheckCall != null) {
                inFlightNewJobCheckCall.cancel();
                inFlightNewJobCheckCall = null;
            }
        } catch (Exception ignore) {}
    }

    /** FETCH ON NEW JOB ONLY: GET_OUTBOUND_ALLOTED_TIME */
    private void fetchOutboundAllottedTimeFresh(String prin) {
        try {
            String url = String.format(
                    "%s?as_prin_code=%s&ts=%d",
                    GET_OUTBOUND_ALLOTED_TIME_BASE_URL,
                    URLEncoder.encode(prin, StandardCharsets.UTF_8.toString()),
                    System.currentTimeMillis());

            Log.d(TAG, "ALLOCATED TIME (NEW JOB) → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightAllottedCall != null) inFlightAllottedCall.cancel();
            inFlightAllottedCall = httpClient.newCall(request);

            inFlightAllottedCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "ALLOCATED TIME failed: " + e.getMessage());
                    runOnUiThread(() -> tvGoal.setText("Goal: Network Error"));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "ALLOCATED TIME code=" + response.code() + " body=" + resBody);

                    runOnUiThread(() -> {
                        if (!response.isSuccessful()) {
                            tvGoal.setText("Goal: Error (" + response.code() + ")");
                            return;
                        }
                        try {
                            if (resBody == null || resBody.trim().isEmpty()) {
                                tvGoal.setText("Goal: Empty Response");
                                return;
                            }
                            JSONObject json = new JSONObject(resBody.trim());
                            String minStr = json.optString("outbounD_ALLOCATED_TIME", "0");
                            if (minStr != null && !minStr.equals("0") && !"null".equalsIgnoreCase(minStr.trim())) {
                                apiAllocatedMinutes = Integer.parseInt(minStr.trim());
                                tvGoal.setText("Goal: " + apiAllocatedMinutes + " min");
                                calculatePerformanceFromApis();
                            } else {
                                tvGoal.setText("Goal: No Data");
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "ALLOCATED TIME parse error: " + ex.getMessage());
                            tvGoal.setText("Goal: Parse Error");
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "ALLOCATED TIME request build error: " + e.getMessage());
            runOnUiThread(() -> tvGoal.setText("Goal: Request Error"));
        }
    }

    /** FETCH ON NEW JOB ONLY: GET_JOB_TIMING */
    private void fetchJobTimingFresh(String loginId, String orderNo, String jobNo) {
        try {
            String url = String.format(
                    "%s?as_login_id=%s&order_no=%s&job_no=%s&ts=%d",
                    GET_JOB_TIMING_BASE_URL,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(orderNo, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(jobNo, StandardCharsets.UTF_8.toString()),
                    System.currentTimeMillis());

            Log.d(TAG, "JOB TIMING (NEW JOB) → " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Accept", "application/json")
                    .build();

            if (inFlightJobTimingCall != null) inFlightJobTimingCall.cancel();
            inFlightJobTimingCall = httpClient.newCall(request);

            inFlightJobTimingCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "JOB TIMING failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        tvTime.setText("Time: Network Error");
                        tvPerformanceValue.setText("N/A");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "JOB TIMING code=" + response.code() + " body=" + resBody);

                    runOnUiThread(() -> {
                        if (!response.isSuccessful()) {
                            tvTime.setText("Time: Error (" + response.code() + ")");
                            tvPerformanceValue.setText("N/A");
                            return;
                        }
                        try {
                            JSONObject json = new JSONObject(resBody);

                            String status = json.optString("status", "UNKNOWN");
                            String totalActiveMinutesStr = json.optString("total_active_minutes", "0");

                            Log.d(TAG, "=== JOB TIMING DETAILS ===");
                            Log.d(TAG, "Job Status: " + status);
                            Log.d(TAG, "Total Active Minutes: " + totalActiveMinutesStr);
                            Log.d(TAG, "API Call Time: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));

                            tvTime.setText("Time: " + totalActiveMinutesStr + " min (" + status + ")");

                            try {
                                actualMinutesTaken = Double.parseDouble(totalActiveMinutesStr);
                            } catch (NumberFormatException nfe) {
                                actualMinutesTaken = 0;
                            }

                            calculatePerformanceFromApis();

                        } catch (JSONException je) {
                            Log.e(TAG, "JOB TIMING parse error: " + je.getMessage());
                            tvTime.setText("Time: Parse Error");
                            tvPerformanceValue.setText("N/A");
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "JOB TIMING request build error: " + e.getMessage());
            runOnUiThread(() -> {
                tvTime.setText("Time: Request Error");
                tvPerformanceValue.setText("N/A");
            });
        }
    }

    private void calculatePerformanceFromApis() {
        if (apiAllocatedMinutes > 0 && actualMinutesTaken > 0) {
            int performance;
            if (actualMinutesTaken <= apiAllocatedMinutes) {
                performance = 100; // finished within allocation
            } else {
                float ratio = (float) apiAllocatedMinutes / (float) actualMinutesTaken;
                performance = Math.max(Math.round(ratio * 100f), 0);
            }
            tvPerformanceValue.setText(performance + "%");
            Log.d(TAG, "Performance calculated: " + performance + "% (allocated=" + apiAllocatedMinutes + ", actual=" + actualMinutesTaken + ")");
        } else {
            tvPerformanceValue.setText("N/A");
        }
    }
}
