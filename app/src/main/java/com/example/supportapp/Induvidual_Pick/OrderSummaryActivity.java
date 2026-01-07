package com.example.supportapp.Induvidual_Pick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.supportapp.Induvidual_Pick.adapter.OptionAdapter;
import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.R;
import com.example.supportapp.voice.VoiceCommandCenter;
import com.example.supportapp.voice.VoiceCommandCenter.Actions;

public class OrderSummaryActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String API_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/ORDER_SUMMARY";
    // NEW API Endpoints
    private static final String START_JOB_PICKING_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/START_JOB_PICKING";
    private static final String PAUSE_JOB_PICKING_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/PAUSE_JOB_PICKING";
    private static final String RESUME_JOB_PICKING_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/RESUME_JOB_PICKING";

    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";
    private static final String TAG = "NinthActivity";

    // SharedPreferences keys
    private static final String JOB_STARTED_PREFIX = "job_started_"; // e.g., "job_started_1025072980"
    private static final String JOB_INITIAL_TOTAL_PICKS_PREFIX = "job_initial_total_picks_";
    private static final String JOB_INITIAL_TOTAL_QUANTITY_PREFIX = "job_initial_total_quantity_";

    private OkHttpClient client;
    private TextView orderNumberText, skuText, quantityText;
    private AppCompatButton btnBack, btnNext;

    // (Declared in your original file; left intact)
    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

    // Parameters for API calls
    private String currentPrinCode;
    private String currentJobNo;
    private String currentOrderNo;
    private String currentLoginId;

    // Request Code for TenthActivity
    private static final int TENTH_ACTIVITY_REQUEST_CODE = 1001;

    /** Unsafe OkHttp client (kept exactly as you had it) */
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override public boolean verify(String hostname, SSLSession session) { return true; }
            });

            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(30, TimeUnit.SECONDS);
            builder.writeTimeout(30, TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating unsafe OkHttpClient: " + e.getMessage());
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ordersummaryactivity);

        // 🔊 Centralized voice registration
        VoiceCommandCenter.init(this);

        // Initialize UI
        orderNumberText = findViewById(R.id.orderNumberText);
        skuText = findViewById(R.id.skuText);
        quantityText = findViewById(R.id.quantityText);
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        Button logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(OrderSummaryActivity.this);
        });

        // Initialize OkHttpClient
        client = getUnsafeOkHttpClient();

        btnBack.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            // Only proceed if 'Pick - Individual' is selected
            if (selectedPosition == 0) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + currentJobNo, false);

                if (!hasJobBeenStarted) {
                    sendStartJobPickingApiCall();
                } else {
                    Log.d(TAG, "Job " + currentJobNo + " already started. Proceeding to TenthActivity without re-sending START.");
                    goToNextActivity();
                }
            } else {
                Log.d(TAG, "Cannot proceed - Pick Individual not selected");
                Toast.makeText(this, "Please select 'Pick - Individual' to proceed.", Toast.LENGTH_SHORT).show();
            }
        });

        // Intent + fallback params
        Intent intent = getIntent();
        String as_prin_code = intent.getStringExtra("PRIN_CODE");
        String as_job_no = intent.getStringExtra("JOB_NUMBER");
        String as_order_no = intent.getStringExtra("ORDER_NUMBER");
        String as_login_id = intent.getStringExtra("USER_ID");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (as_prin_code == null || as_prin_code.isEmpty()) {
            as_prin_code = prefs.getString("PRIN_CODE", "");
            Log.w(TAG, "PRIN_CODE missing from Intent, using SharedPreferences: " + as_prin_code);
        }
        if (as_job_no == null || as_job_no.isEmpty()) {
            as_job_no = prefs.getString("JOB_NUMBER", "");
            Log.w(TAG, "JOB_NUMBER missing from Intent, using SharedPreferences: " + as_job_no);
        }
        if (as_order_no == null || as_order_no.isEmpty()) {
            as_order_no = prefs.getString("ORDER_NUMBER", "");
            Log.w(TAG, "ORDER_NUMBER missing from Intent, using SharedPreferences: " + as_order_no);
        }
        if (as_login_id == null || as_login_id.isEmpty()) {
            as_login_id = prefs.getString("CURRENT_USER_ID", "");
            Log.w(TAG, "USER_ID missing from Intent, using SharedPreferences: " + as_login_id);
        }

        currentPrinCode = as_prin_code;
        currentJobNo = as_job_no;
        currentOrderNo = as_order_no;
        currentLoginId = as_login_id;

        if (currentPrinCode.isEmpty() || currentJobNo.isEmpty() || currentOrderNo.isEmpty() || currentLoginId.isEmpty()) {
            Toast.makeText(this, "Missing required parameters. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fetchOrderSummary(currentPrinCode, currentJobNo, currentOrderNo, currentLoginId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Ensure phrases active whenever this screen returns to foreground
        VoiceCommandCenter.init(this);
    }

    private void goBack() {
        Log.d(TAG, "Going back to previous activity");
        finish();
    }

    private void goToNextActivity() {
        Intent intent = new Intent(OrderSummaryActivity.this, InduvidualJobActivity.class);
        intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
        intent.putExtra("PRIN_CODE", currentPrinCode);
        intent.putExtra("JOB_NUMBER", currentJobNo);
        intent.putExtra("ORDER_NUMBER", currentOrderNo);
        intent.putExtra("USER_ID", currentLoginId);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int trueInitialTotalPicks = prefs.getInt(JOB_INITIAL_TOTAL_PICKS_PREFIX + currentJobNo, 0);
        int trueInitialTotalQuantity = prefs.getInt(JOB_INITIAL_TOTAL_QUANTITY_PREFIX + currentJobNo, 0);

        intent.putExtra("INITIAL_API_TOTAL_PICKS", trueInitialTotalPicks);
        intent.putExtra("INITIAL_API_TOTAL_QUANTITY", trueInitialTotalQuantity);
        Log.d(TAG, "Passing to TenthActivity - True Initial Total Picks: " + trueInitialTotalPicks + ", True Initial Total Quantity: " + trueInitialTotalQuantity);

        startActivityForResult(intent, TENTH_ACTIVITY_REQUEST_CODE);
        Log.d(TAG, "Navigating to TenthActivity");
    }

    // 🔊 Centralized voice → actions mapping (keeps your behavior)
    private final Actions voiceActions = new Actions() {
        @Override public void onNext()       { if (btnNext != null) btnNext.performClick(); }
        @Override public void onBack()       { goBack(); }
        @Override public void onScrollUp()   { /* not used on this screen */ }
        @Override public void onScrollDown() { /* not used on this screen */ }
        @Override public void onSelect()     { /* not used on this screen */ }

        @Override public void onInbound()    { /* not used here */ }
        @Override public void onOutbound()   { /* not used here */ }
        @Override public void onInventory()  { /* not used here */ }
        @Override public void onIndividual() { /* not used here */ }
        @Override public void onConsolidated(){/* not used here */ }

        @Override public void onLogout() {
            Log.d(TAG, "Voice logout command triggered");
            LogoutManager.performLogout(OrderSummaryActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void fetchOrderSummary(String as_prin_code, String as_job_no, String as_order_no, String as_login_id) {
        try {
            String url = String.format("%s?as_prin_code=%s&as_job_no=%s&as_order_no=%s&as_login_id=%s",
                    API_URL,
                    URLEncoder.encode(as_prin_code, "UTF-8"),
                    URLEncoder.encode(as_job_no, "UTF-8"),
                    URLEncoder.encode(as_order_no, "UTF-8"),
                    URLEncoder.encode(as_login_id, "UTF-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("User-Agent", "SupportApp/1.0")
                    .build();

            Log.d(TAG, "Fetching order summary from: " + url);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "API call failed: " + e.getMessage());
                        String errorMessage = "Failed to fetch data: " + e.getMessage();
                        if (e.getMessage().contains("Trust anchor")) {
                            errorMessage = "SSL Certificate error. Please check server configuration.";
                        } else if (e.getMessage().contains("timeout")) {
                            errorMessage = "Connection timeout. Please check your internet connection.";
                        } else if (e.getMessage().contains("Unable to resolve host")) {
                            errorMessage = "Cannot reach server. Please check your internet connection.";
                        }
                        Toast.makeText(OrderSummaryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body() != null ? response.body().string() : "";
                            Log.d(TAG, "API Response: " + responseBody);

                            if (responseBody.isEmpty()) {
                                runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Empty response from API", Toast.LENGTH_SHORT).show());
                                return;
                            }

                            JSONArray orderSummary;
                            if (responseBody.trim().startsWith("[")) {
                                orderSummary = new JSONArray(responseBody);
                            } else {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                orderSummary = jsonResponse.getJSONArray("order_summary");
                            }

                            if (orderSummary.length() > 0) {
                                JSONObject orderData = orderSummary.getJSONObject(0);
                                String orderNo = orderData.optString("order_no", "N/A");
                                int totalSku = orderData.optInt("total_sku", 0);
                                int totalQty = orderData.optInt("total_qty", 0);

                                runOnUiThread(() -> {
                                    orderNumberText.setText("Order#: " + orderNo);
                                    skuText.setText("SKU: " + totalSku);
                                    quantityText.setText("Quantity: " + totalQty);
                                });

                                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + currentJobNo, false);

                                if (!hasJobBeenStarted) {
                                    prefs.edit()
                                            .putInt(JOB_INITIAL_TOTAL_PICKS_PREFIX + currentJobNo, totalSku)
                                            .putInt(JOB_INITIAL_TOTAL_QUANTITY_PREFIX + currentJobNo, totalQty)
                                            .apply();
                                    Log.d(TAG, "Captured TRUE Initial Order Summary for " + currentJobNo + " - Total SKU: " + totalSku + ", Total Quantity: " + totalQty);
                                } else {
                                    Log.d(TAG, "Job " + currentJobNo + " already started. Displaying current summary.");
                                }
                            } else {
                                runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "No data available", Toast.LENGTH_SHORT).show());
                            }
                        } catch (JSONException e) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
                                Toast.makeText(OrderSummaryActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Unexpected error: " + e.getMessage());
                                Toast.makeText(OrderSummaryActivity.this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                            Toast.makeText(OrderSummaryActivity.this, "Error fetching data: " + response.message(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating API request: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void sendStartJobPickingApiCall() {
        if (currentLoginId == null || currentLoginId.isEmpty() ||
                currentOrderNo == null || currentOrderNo.isEmpty() ||
                currentJobNo == null || currentJobNo.isEmpty()) {
            Toast.makeText(this, "Missing user/order/job details to start picking.", Toast.LENGTH_LONG).show();
            goToNextActivity();
            return;
        }

        try {
            String encodedLoginId = URLEncoder.encode(currentLoginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(currentOrderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(currentJobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    START_JOB_PICKING_URL, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                    .addHeader("accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            Log.d(TAG, "Sending START_JOB_PICKING request to: " + url);

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "START_JOB_PICKING API call failed: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Failed to start picking: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    goToNextActivity();
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "START_JOB_PICKING API Response: " + response.code() + ", Body: " + responseBody);
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Job picking started.", Toast.LENGTH_SHORT).show());
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        prefs.edit().putBoolean(JOB_STARTED_PREFIX + currentJobNo, true).apply();
                        Log.d(TAG, "Job " + currentJobNo + " marked as 'started' in SharedPreferences.");
                    } else {
                        runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Failed to start picking: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                    goToNextActivity();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing START_JOB_PICKING request: " + e.getMessage());
            Toast.makeText(this, "Error preparing start picking request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            goToNextActivity();
        }
    }

    private void sendPauseJobPickingApiCall() {
        if (currentLoginId == null || currentLoginId.isEmpty() ||
                currentOrderNo == null || currentOrderNo.isEmpty() ||
                currentJobNo == null || currentJobNo.isEmpty()) {
            Log.w(TAG, "Missing user/order/job details to pause picking. Skipping pause API call.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + currentJobNo, false);
        if (!hasJobBeenStarted) {
            Log.d(TAG, "Job " + currentJobNo + " was not marked as started. Skipping PAUSE API call.");
            return;
        }

        try {
            String encodedLoginId = URLEncoder.encode(currentLoginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(currentOrderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(currentJobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    PAUSE_JOB_PICKING_URL, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                    .addHeader("accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            Log.d(TAG, "Sending PAUSE_JOB_PICKING request to: " + url);

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "PAUSE_JOB_PICKING API call failed: " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "PAUSE_JOB_PICKING API Response: " + response.code() + ", Body: " + responseBody);
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Job picking paused successfully.");
                    } else {
                        Log.e(TAG, "Failed to pause picking: " + response.message());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing PAUSE_JOB_PICKING request: " + e.getMessage());
        }
    }

    private void sendResumeJobPickingApiCall() {
        Log.w(TAG, "sendResumeJobPickingApiCall called in NinthActivity, but this should be handled by TenthActivity's onResume.");
        if (currentLoginId == null || currentLoginId.isEmpty() ||
                currentOrderNo == null || currentOrderNo.isEmpty() ||
                currentJobNo == null || currentJobNo.isEmpty()) {
            Log.w(TAG, "Missing user/order/job details to resume picking. Skipping resume API call.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + currentJobNo, false);
        if (!hasJobBeenStarted) {
            Log.d(TAG, "Job " + currentJobNo + " was not marked as started. Skipping RESUME API call.");
            return;
        }

        try {
            String encodedLoginId = URLEncoder.encode(currentLoginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(currentOrderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(currentJobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    RESUME_JOB_PICKING_URL, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                    .addHeader("accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            Log.d(TAG, "Sending RESUME_JOB_PICKING request to: " + url);

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "RESUME_JOB_PICKING API call failed: " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "RESUME_JOB_PICKING API Response: " + response.code() + ", Body: " + responseBody);
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Job picking resumed.", Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Failed to resume picking: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing RESUME_JOB_PICKING request: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(OrderSummaryActivity.this, "Error preparing resume picking request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TENTH_ACTIVITY_REQUEST_CODE) {
            Log.d(TAG, "TenthActivity returned. Sending PAUSE API call.");
            sendPauseJobPickingApiCall();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) {
            client.dispatcher().cancelAll();
        }
    }
}
