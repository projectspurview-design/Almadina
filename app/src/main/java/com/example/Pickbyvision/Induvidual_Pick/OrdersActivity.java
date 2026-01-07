package com.example.Pickbyvision.Induvidual_Pick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pickbyvision.data.UserSessionManager;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;
import com.example.Pickbyvision.Induvidual_Pick.network.UnsafeOkHttpClient;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.example.Pickbyvision.voice.VoiceCommandCenter.Actions;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "OrdersActivity";
    private static final String PREFS_NAME = "AppPrefs";

    private static final String EXTRA_USER_ID = "USER_ID";
    private static final String EXTRA_USER_NAME = "USER_NAME";
    private static final String EXTRA_JOB_NUMBER = "JOB_NUMBER";
    private static final String EXTRA_ORDER_NUMBER = "ORDER_NUMBER";
    private static final String EXTRA_PRIN_CODE = "PRIN_CODE";

    private ExecutorService executorService;
    private Handler mainHandler;

    private String currentUserId;
    private String currentUserName;

    private JSONArray jobList = new JSONArray();
    private int currentJobIndex = 0;

    private VuzixSpeechClient speechClient;

    private TextView progressText;
    private TextView customerNameText;
    private TextView mohebiText;
    private TextView orderNumberText;
    private TextView jobNumberText;
    private TextView jobNumberText1;
    private TextView pausedStatusText;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ordersactivity);

        VoiceCommandCenter.init(this);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        currentUserId = resolveUserId();
        currentUserName = resolveUserName();

        Log.d(TAG, "=====================================");
        Log.d(TAG, "OrdersActivity Started");
        Log.d(TAG, "UserId: " + currentUserId);
        Log.d(TAG, "UserName: " + currentUserName);
        Log.d(TAG, "=====================================");

        initializeViews();

        if (isBlank(currentUserId)) {
            Log.e(TAG, "Session invalid - redirecting to login");
            showUserDataError();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(EXTRA_USER_ID, currentUserId)
                .putString(EXTRA_USER_NAME, currentUserName)
                .apply();

        prefs.edit().remove("EighthActivityPaused").apply();

        setupInitialUI();
        loadPicklistJobData(currentUserId);
        setupButtonListeners();
        setupVoiceCommands();
    }

    private void initializeViews() {
        mohebiText = findViewById(R.id.mohebiText);
        orderNumberText = findViewById(R.id.orderNumberText);
        jobNumberText = findViewById(R.id.jobNumberText);
        jobNumberText1 = findViewById(R.id.jobNumberText1);
        pausedStatusText = findViewById(R.id.pausedStatusText);
        nextButton = findViewById(R.id.btnBottomRight);
        progressText = findViewById(R.id.progressText);
        customerNameText = findViewById(R.id.customerNameText);
    }

    private void setupInitialUI() {
        mohebiText.setText("Logged in: " + safe(currentUserName, "User"));
        orderNumberText.setText("Order#: Loading...");
        jobNumberText.setText("Job#: Loading...");
        jobNumberText1.setText("Loading...");
        pausedStatusText.setVisibility(TextView.GONE);
        progressText.setText("0/0");
        customerNameText.setText("Customer/store: Loading...");
        nextButton.setText("Next");
    }

    private void setupButtonListeners() {
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button backButton = findViewById(R.id.backButton);

        arrowUp.setOnClickListener(v -> showPreviousJob());
        arrowDown.setOnClickListener(v -> showNextJob());

        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(OrdersActivity.this);
        });

        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            if (executorService != null) executorService.shutdownNow();
            startActivity(new Intent(this, OutBoundActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        nextButton.setOnClickListener(v -> handleNext());
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");
            Log.d(TAG, "Voice commands setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Voice init failed", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceCommandCenter.init(this);
        Log.d(TAG, "onResume - Voice commands re-initialized");
    }

    private final Actions voiceActions = new Actions() {
        @Override public void onNext() { handleNext(); }
        @Override public void onBack() { finish(); }
        @Override public void onScrollUp() { showPreviousJob(); }
        @Override public void onScrollDown() { showNextJob(); }
        @Override public void onSelect() {}
        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}
        @Override public void onLogout() {
            Log.d(TAG, "Voice logout command triggered");
            LogoutManager.performLogout(OrdersActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)
                || super.onKeyDown(keyCode, event);
    }

    private void loadPicklistJobData(String loginId) {
        Log.d(TAG, "Loading picklist data for loginId: " + loginId);

        executorService.execute(() -> {
            try {
                String urlStr = ApiConfig.PICKLIST_JOB
                        + "?as_login_id=" + URLEncoder.encode(loginId, StandardCharsets.UTF_8.name());

                Log.d(TAG, "API URL: " + urlStr);

                OkHttpClient client = UnsafeOkHttpClient.get();

                Request request = new Request.Builder()
                        .url(urlStr)
                        .get()
                        .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                        .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                        .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    int responseCode = response.code();
                    Log.d(TAG, " Response Code: " + responseCode);

                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        Log.d(TAG, " API Response: " + body);
                        parsePicklistResponse(body);
                    } else {
                        Log.e(TAG, "API Error - Response Code: " + responseCode);
                        showFallback();
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, " API Exception: " + e.getMessage(), e);
                showFallback();
            }
        });
    }

    private void parsePicklistResponse(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            jobList = arr;
            currentJobIndex = 0;

            Log.d(TAG, " Parsed " + arr.length() + " jobs from API");

            mainHandler.post(() -> {
                try {
                    if (arr.length() > 0) {
                        JSONObject firstJob = arr.getJSONObject(0);
                        updateUI(firstJob, arr.length());
                    } else {
                        Toast.makeText(this, "No jobs available", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, " Failed to parse first job", e);
                    showFallback();
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, " Invalid JSON response", e);
            Log.e(TAG, "Response was: " + json);
            showFallback();
        }
    }

    private void updateUI(JSONObject job, int total) {
        try {
            String progress = (currentJobIndex + 1) + "/" + total;
            progressText.setText(progress);

            String prinName = job.optString("prin_name", safe(currentUserName, "User"));
            mohebiText.setText("Principal: " + prinName);

            String custName = job.optString("cust_name", "-");
            customerNameText.setText("Customer/store: " + custName);

            String orderNo = job.optString("order_no", "-");
            orderNumberText.setText("Order#: " + orderNo);

            String jobNo = job.optString("job_no", "-");
            jobNumberText.setText("Job#: " + jobNo);

            String prinCode = job.optString("prin_code", "-");
            jobNumberText1.setText(prinCode);

        } catch (Exception e) {
            Log.e(TAG, " Error updating UI", e);
        }
    }

    private void showNextJob() {
        if (currentJobIndex < jobList.length() - 1) {
            currentJobIndex++;
            try {
                JSONObject job = jobList.getJSONObject(currentJobIndex);
                updateUI(job, jobList.length());
            } catch (JSONException e) {
                Log.e(TAG, " Error showing next job", e);
            }
        } else {
            Toast.makeText(this, "Already at last job", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPreviousJob() {
        if (currentJobIndex > 0) {
            currentJobIndex--;
            try {
                JSONObject job = jobList.getJSONObject(currentJobIndex);
                updateUI(job, jobList.length());
            } catch (JSONException e) {
                Log.e(TAG, " Error showing previous job", e);
            }
        } else {
            Toast.makeText(this, "Already at first job", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleNext() {
        if (jobList.length() == 0) {
            Toast.makeText(this, "No job selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject currentJob = jobList.getJSONObject(currentJobIndex);

            String jobNo = currentJob.optString("job_no", "");
            String orderNo = currentJob.optString("order_no", "");
            String prinCode = currentJob.optString("prin_code", "");

            if (isBlank(jobNo) || isBlank(orderNo) || isBlank(prinCode)) {
                Toast.makeText(this, "Invalid job details", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit()
                    .putString(EXTRA_JOB_NUMBER, jobNo)
                    .putString(EXTRA_ORDER_NUMBER, orderNo)
                    .putString(EXTRA_PRIN_CODE, prinCode)
                    .apply();

            Intent intent = new Intent(this, OrderSummaryActivity.class);

            intent.putExtra(EXTRA_USER_ID, currentUserId);
            intent.putExtra(EXTRA_USER_NAME, currentUserName);

            intent.putExtra(EXTRA_JOB_NUMBER, jobNo);
            intent.putExtra(EXTRA_ORDER_NUMBER, orderNo);
            intent.putExtra(EXTRA_PRIN_CODE, prinCode);

            startActivity(intent);

        } catch (JSONException e) {
            Log.e(TAG, " Error getting current job", e);
            Toast.makeText(this, "Error loading job details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFallback() {
        mainHandler.post(() -> Toast.makeText(this, "Unable to load jobs", Toast.LENGTH_LONG).show());
    }

    private void showUserDataError() {
        Toast.makeText(this, "Please login again", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, Barcodescanner.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private String resolveUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String fromIntent = getIntent().getStringExtra(EXTRA_USER_ID);
        String fromSession = UserSessionManager.getUserId(this);
        String fromPrefs = prefs.getString(EXTRA_USER_ID, null);

        String resolved = firstNonBlank(fromIntent, fromSession, fromPrefs);
        Log.d(TAG, " Resolved UserId: " + resolved);
        return resolved;
    }

    private String resolveUserName() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String fromIntent = getIntent().getStringExtra(EXTRA_USER_NAME);
        String fromSession = UserSessionManager.getUserName(this);
        String fromPrefs = prefs.getString(EXTRA_USER_NAME, "User");

        String resolved = firstNonBlank(fromIntent, fromSession, fromPrefs, "User");
        Log.d(TAG, " Resolved UserName: " + resolved);
        return resolved;
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

    private static String safe(String val, String fallback) {
        return isBlank(val) ? fallback : val;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) executorService.shutdown();
        Log.d(TAG, " OrdersActivity destroyed");
    }
}
