package com.example.Pickbyvision.Induvidual_Pick;

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

import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;
import com.example.Pickbyvision.Induvidual_Pick.network.UnsafeOkHttpClient;
import com.example.Pickbyvision.Induvidual_Pick.session.SecureTokenStore;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CompleteordersummaryActivity extends AppCompatActivity {

    private static final String TAG = "CompleteOrderSummary";

    private static final long CHECK_NEW_JOB_INTERVAL = 10_000L;

    private VuzixSpeechClient speechClient;


    private OkHttpClient httpClient;


    private final Handler checkNewJobHandler = new Handler(Looper.getMainLooper());
    private Runnable checkNewJobRunnable;
    private boolean isCheckingNewJob = false;


    private Call inFlightJobTimingCall = null;
    private Call inFlightAllottedCall = null;
    private Call inFlightNewJobCheckCall = null;


    private volatile boolean isAlive = false;


    private String orderNumber, jobNumber, userId;
    private String referenceCode, prinCode;
    private int initialApiTotalPicks = 0, initialApiTotalQuantity = 0;


    private String currentJobNumber = null;
    private String currentOrderNumber = null;


    private int apiAllocatedMinutes = 0;
    private double actualMinutesTaken = 0;

    private TextView tvPicks, tvQuantity, tvTime, tvGoal, tvPerformanceValue, tvLastUpdate;
    private Button nextButton, logoutButton;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());


    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override
        public void onNext() {
            Log.d(TAG, "Voice command: NEXT triggered");
            runOnUiThread(CompleteordersummaryActivity.this::handleNextAction);
        }

        @Override
        public void onLogout() {
            Log.d(TAG, "Voice command: LOGOUT triggered");
            runOnUiThread(() -> LogoutManager.performLogout(CompleteordersummaryActivity.this));
        }

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

        isAlive = true;


        VoiceCommandCenter.init(this);

        initializeUI();

        httpClient = UnsafeOkHttpClient.get();

        setupButtonListeners();
        handleIncomingIntent();

        setupVuzixSpeechClient();

        if (orderNumber != null && jobNumber != null && userId != null) {
            startCheckingForNewJob();
        }
    }

    private void setupVuzixSpeechClient() {
        try {
            speechClient = new VuzixSpeechClient(this);


            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");

            Log.d(TAG, "VuzixSpeechClient initialized");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed: " + e.getMessage());
        }
    }

    private void initializeUI() {
        tvPicks = findViewById(R.id.tvPicks);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvTime = findViewById(R.id.tvTime);
        tvGoal = findViewById(R.id.tvGoal);
        tvPerformanceValue = findViewById(R.id.tvPerformanceValue);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        nextButton = findViewById(R.id.btnNext);
        logoutButton = findViewById(R.id.logoutButton);

        if (nextButton != null) {
            nextButton.setEnabled(true);
            nextButton.setClickable(true);
            nextButton.setFocusable(true);
        } else {
            Log.e(TAG, "ERROR: btnNext not found in layout!");
        }


        tvGoal.setText("Goal: Loading...");
        tvPerformanceValue.setText("Calculating...");
        tvTime.setText("Time: Loading...");
        tvLastUpdate.setText("Last Update: Never");
    }

    private void setupButtonListeners() {
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v ->
                    LogoutManager.performLogout(CompleteordersummaryActivity.this)
            );
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Log.d(TAG, "NEXT button clicked");
                handleNextAction();
            });
        }
    }

    private void handleNextAction() {
        stopCheckingForNewJob();
        cancelInFlightCalls();

        Toast.makeText(this, "Going to next order", Toast.LENGTH_SHORT).show();

        try {
            startActivity(new Intent(this, NextOrderReadyActivity.class));
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAlive = true;

        VoiceCommandCenter.init(this);

        if (orderNumber != null && jobNumber != null && userId != null) {
            startCheckingForNewJob();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCheckingForNewJob();
        cancelInFlightCalls();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isAlive = false;

        stopCheckingForNewJob();
        cancelInFlightCalls();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            setMissingInfoUI("No Intent");
            return;
        }

        orderNumber = intent.getStringExtra("ORDER_NUMBER");
        jobNumber = intent.getStringExtra("JOB_NUMBER");
        userId = intent.getStringExtra("USER_ID");
        referenceCode = intent.getStringExtra("REFERENCE_CODE");
        prinCode = intent.getStringExtra("PRIN_CODE");
        initialApiTotalPicks = intent.getIntExtra("INITIAL_API_TOTAL_PICKS", 0);
        initialApiTotalQuantity = intent.getIntExtra("INITIAL_API_TOTAL_QUANTITY", 0);

        currentJobNumber = jobNumber;
        currentOrderNumber = orderNumber;

        if (tvPicks != null) tvPicks.setText("Picks: " + initialApiTotalPicks);
        if (tvQuantity != null) tvQuantity.setText("Quantity: " + initialApiTotalQuantity);

        if (orderNumber == null || jobNumber == null || userId == null) {
            setMissingInfoUI("Missing Info");
            return;
        }

        fetchDataForCurrentJob();
    }

    private void setMissingInfoUI(String reason) {
        if (tvTime != null) tvTime.setText("Time: N/A (" + reason + ")");
        if (tvGoal != null) tvGoal.setText("Goal: N/A (" + reason + ")");
        if (tvPerformanceValue != null) tvPerformanceValue.setText("N/A");
        if (tvLastUpdate != null) tvLastUpdate.setText("Last Update: Error - " + reason);
    }

    private void startCheckingForNewJob() {
        if (isCheckingNewJob) return;

        checkNewJobRunnable = new Runnable() {
            @Override
            public void run() {
                checkForNewJob();
                checkNewJobHandler.postDelayed(this, CHECK_NEW_JOB_INTERVAL);
            }
        };

        isCheckingNewJob = true;
        checkNewJobHandler.post(checkNewJobRunnable);
        Log.d(TAG, "Started polling for new job");
    }

    private void stopCheckingForNewJob() {
        if (!isCheckingNewJob) return;
        if (checkNewJobRunnable != null) {
            checkNewJobHandler.removeCallbacks(checkNewJobRunnable);
        }
        isCheckingNewJob = false;
        Log.d(TAG, "Stopped polling for new job");
    }

    private void checkForNewJob() {
        if (userId == null) return;

        try {
            String url = String.format(
                    "%s?as_login_id=%s&ts=%d",
                    ApiConfig.GET_JOB_TIMING,
                    URLEncoder.encode(userId, StandardCharsets.UTF_8.toString()),
                    System.currentTimeMillis()
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .addHeader(ApiConfig.HEADER_AUTH, "Bearer " + SecureTokenStore.getToken(this))
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
                    String resBody = (response.body() != null) ? response.body().string() : "";

                    if (!response.isSuccessful() || resBody == null || resBody.trim().isEmpty()) return;

                    try {
                        JSONObject json = new JSONObject(resBody.trim());
                        String newJobNumber = json.optString("job_no", "");
                        String newOrderNumber = json.optString("order_no", "");

                        if (!isAlive || isFinishing()) return;

                        runOnUiThread(() -> {
                            if (!isAlive || isFinishing()) return;

                            boolean jobChanged =
                                    !newJobNumber.isEmpty() &&
                                            !newOrderNumber.isEmpty() &&
                                            (!newJobNumber.equals(currentJobNumber) ||
                                                    !newOrderNumber.equals(currentOrderNumber));

                            if (jobChanged) {
                                Log.d(TAG, "Job changed -> job=" + newJobNumber + ", order=" + newOrderNumber);

                                currentJobNumber = newJobNumber;
                                currentOrderNumber = newOrderNumber;
                                jobNumber = newJobNumber;
                                orderNumber = newOrderNumber;

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
            });

        } catch (Exception e) {
            Log.e(TAG, "New job check request build error: " + e.getMessage());
        }
    }

    private void fetchDataForCurrentJob() {
        if (orderNumber == null || jobNumber == null || userId == null) return;


        if (tvLastUpdate != null) tvLastUpdate.setText("Last Update: " + timeFormat.format(new Date()));


        apiAllocatedMinutes = 0;
        actualMinutesTaken = 0;

        String codeToUse = (prinCode != null && !prinCode.trim().isEmpty())
                ? prinCode
                : ((referenceCode != null && !referenceCode.trim().isEmpty())
                ? referenceCode
                : orderNumber);

        fetchOutboundAllottedTimeFresh(codeToUse);
        fetchJobTimingFresh(userId, orderNumber, jobNumber);
    }

    private void cancelInFlightCalls() {
        try {
            if (inFlightJobTimingCall != null) { inFlightJobTimingCall.cancel(); inFlightJobTimingCall = null; }
            if (inFlightAllottedCall != null) { inFlightAllottedCall.cancel(); inFlightAllottedCall = null; }
            if (inFlightNewJobCheckCall != null) { inFlightNewJobCheckCall.cancel(); inFlightNewJobCheckCall = null; }
        } catch (Exception ignore) { }
    }

    private void fetchOutboundAllottedTimeFresh(String prin) {
        try {
            String url = String.format(
                    "%s?as_prin_code=%s&ts=%d",
                    ApiConfig.GET_OUTBOUND_ALLOTED_TIME,
                    URLEncoder.encode(prin, StandardCharsets.UTF_8.toString()),
                    System.currentTimeMillis()
            );

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_JSON)
                    .build();

            if (inFlightAllottedCall != null) inFlightAllottedCall.cancel();
            inFlightAllottedCall = httpClient.newCall(request);

            inFlightAllottedCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "ALLOCATED TIME failed: " + e.getMessage());
                    if (!isAlive || isFinishing()) return;
                    runOnUiThread(() -> tvGoal.setText("Goal: Network Error"));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = (response.body() != null) ? response.body().string() : "";

                    if (!isAlive || isFinishing()) return;

                    runOnUiThread(() -> {
                        if (!isAlive || isFinishing()) return;

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

                            if (minStr != null
                                    && !minStr.trim().isEmpty()
                                    && !"0".equals(minStr.trim())
                                    && !"null".equalsIgnoreCase(minStr.trim())) {

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
            if (!isAlive || isFinishing()) return;
            runOnUiThread(() -> tvGoal.setText("Goal: Request Error"));
        }
    }

    private void fetchJobTimingFresh(String loginId, String orderNo, String jobNo) {
        try {
            String url = String.format(
                    "%s?as_login_id=%s&order_no=%s&job_no=%s&ts=%d",
                    ApiConfig.GET_JOB_TIMING,
                    URLEncoder.encode(loginId, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(orderNo, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(jobNo, StandardCharsets.UTF_8.toString()),
                    System.currentTimeMillis()
            );

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_JSON)
                    .build();

            if (inFlightJobTimingCall != null) inFlightJobTimingCall.cancel();
            inFlightJobTimingCall = httpClient.newCall(request);

            inFlightJobTimingCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "JOB TIMING failed: " + e.getMessage());
                    if (!isAlive || isFinishing()) return;
                    runOnUiThread(() -> {
                        tvTime.setText("Time: Network Error");
                        tvPerformanceValue.setText("N/A");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resBody = (response.body() != null) ? response.body().string() : "";

                    if (!isAlive || isFinishing()) return;

                    runOnUiThread(() -> {
                        if (!isAlive || isFinishing()) return;

                        if (!response.isSuccessful()) {
                            tvTime.setText("Time: Error (" + response.code() + ")");
                            tvPerformanceValue.setText("N/A");
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(resBody);

                            String status = json.optString("status", "UNKNOWN");
                            String totalActiveMinutesStr = json.optString("total_active_minutes", "0");

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
            if (!isAlive || isFinishing()) return;
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
                performance = 100;
            } else {
                float ratio = (float) apiAllocatedMinutes / (float) actualMinutesTaken;
                performance = Math.max(Math.round(ratio * 100f), 0);
            }
            tvPerformanceValue.setText(performance + "%");
        } else {
            tvPerformanceValue.setText("N/A");
        }
    }
}
