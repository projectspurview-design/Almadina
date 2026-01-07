package com.example.supportapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.URLEncoder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class NinthActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String API_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/ORDER_SUMMARY";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";
    private static final String TAG = "NinthActivity";
    private static final String ORDER_DATA_KEY = "ORDER_SUMMARY_DATA_";
    private static final String LAST_USER_ID_KEY = "LAST_USER_ID";

    private OkHttpClient client;
    private TextView orderNumberText, skuText, quantityText;
    private Button Back, Next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ninth);

        // Initialize TextViews and Buttons
        orderNumberText = findViewById(R.id.orderNumberText);
        skuText = findViewById(R.id.skuText);
        quantityText = findViewById(R.id.quantityText);
        Back = findViewById(R.id.btnBack);
        Next = findViewById(R.id.btnNext);

        // Set button listeners
        Back.setOnClickListener(v -> {
            Intent intent = new Intent(NinthActivity.this, EighthActivity.class);
            startActivity(intent);
        });
        Next.setOnClickListener(v -> {
            Intent intent = new Intent(NinthActivity.this, TenthActivity.class);
            startActivity(intent);
        });

        // Get parameters from Intent
        Intent intent = getIntent();
        String as_prin_code = intent.getStringExtra("PRIN_CODE");
        String as_job_no = intent.getStringExtra("JOB_NUMBER");
        String as_order_no = intent.getStringExtra("ORDER_NUMBER");
        String as_login_id = intent.getStringExtra("USER_ID");

        // Fallback to SharedPreferences
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

        // Validate parameters
        if (as_prin_code.isEmpty() || as_job_no.isEmpty() || as_order_no.isEmpty() || as_login_id.isEmpty()) {
            Toast.makeText(this, "Missing required parameters. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Check if data exists for the same user
        String lastUserId = prefs.getString(LAST_USER_ID_KEY, "");
        String cachedData = prefs.getString(ORDER_DATA_KEY + as_login_id, "");
        if (as_login_id.equals(lastUserId) && !cachedData.isEmpty()) {
            Log.d(TAG, "Using cached data for user: " + as_login_id);
            displayCachedData(cachedData);
        } else {
            // Fetch new data if user changed or no data exists
            fetchOrderSummary(as_prin_code, as_job_no, as_order_no, as_login_id);
        }
    }

    private void fetchOrderSummary(String as_prin_code, String as_job_no, String as_order_no, String as_login_id) {
        try {
            // Build URL with encoded query parameters
            String url = String.format("%s?as_prin_code=%s&as_job_no=%s&as_order_no=%s&as_login_id=%s",
                    API_URL,
                    URLEncoder.encode(as_prin_code, "UTF-8"),
                    URLEncoder.encode(as_job_no, "UTF-8"),
                    URLEncoder.encode(as_order_no, "UTF-8"),
                    URLEncoder.encode(as_login_id, "UTF-8"));

            // Build request with headers
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("User-Agent", "SupportApp/1.0")
                    .build();

            // Log.d(TAG, "Fetching order summary from: " + url);

            // Make API call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "API call failed: " + e.getMessage());
                        /*   Toast.makeText(NinthActivity.this, "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show();*/
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body() != null ? response.body().string() : "";
                            Log.d(TAG, "API Response: " + responseBody);

                            if (responseBody.isEmpty()) {
                                runOnUiThread(() -> {
                                    /*  Toast.makeText(NinthActivity.this, "Empty response from API", Toast.LENGTH_SHORT).show();*/
                                });
                                return;
                            }

                            // Determine if response is a JSONArray or JSONObject
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
                                String totalSku = orderData.optString("total_sku", "N/A");
                                String totalQty = orderData.optString("total_qty", "N/A");

                                // Prepare data to cache
                                JSONObject cacheObject = new JSONObject();
                                cacheObject.put("order_no", orderNo);
                                cacheObject.put("total_sku", totalSku);
                                cacheObject.put("total_qty", totalQty);
                                String cachedData = cacheObject.toString();

                                // Save to SharedPreferences
                                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(ORDER_DATA_KEY + as_login_id, cachedData);
                                editor.putString(LAST_USER_ID_KEY, as_login_id);
                                editor.apply();

                                // Update UI
                                runOnUiThread(() -> {
                                    orderNumberText.setText("Order Number: " + orderNo);
                                    skuText.setText("SKU: " + totalSku);
                                    quantityText.setText("Quantity: " + totalQty);
                                });
                            } else {
                                runOnUiThread(() -> {
                                    /* Toast.makeText(NinthActivity.this, "No data available", Toast.LENGTH_SHORT).show();*/
                                });
                            }
                        } catch (JSONException e) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
                                /*Toast.makeText(NinthActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();*/
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Unexpected error: " + e.getMessage());
                                /* Toast.makeText(NinthActivity.this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();*/
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                            /*  Toast.makeText(NinthActivity.this, "Error fetching data: " + response.message(), Toast.LENGTH_SHORT).show();*/
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating API request: " + e.getMessage());
            runOnUiThread(() -> {
                /* Toast.makeText(NinthActivity.this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();*/
            });
        }
    }

    private void displayCachedData(String cachedData) {
        try {
            JSONObject cachedObject = new JSONObject(cachedData);
            String orderNo = cachedObject.optString("order_no", "N/A");
            String totalSku = cachedObject.optString("total_sku", "N/A");
            String totalQty = cachedObject.optString("total_qty", "N/A");

            runOnUiThread(() -> {
                orderNumberText.setText("Order Number: " + orderNo);
                skuText.setText("SKU: " + totalSku);
                quantityText.setText("Quantity: " + totalQty);
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing cached data: " + e.getMessage());
            runOnUiThread(() -> {
                /* Toast.makeText(NinthActivity.this, "Error loading cached data", Toast.LENGTH_SHORT).show();*/
                fetchOrderSummaryFromPrefs(); // Fallback to fetch if cache is corrupted
            });
        }
    }

    private void fetchOrderSummaryFromPrefs() {
        // Fallback to fetch data if cached data is invalid
        Intent intent = getIntent();
        String as_prin_code = intent.getStringExtra("PRIN_CODE");
        String as_job_no = intent.getStringExtra("JOB_NUMBER");
        String as_order_no = intent.getStringExtra("ORDER_NUMBER");
        String as_login_id = intent.getStringExtra("USER_ID");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (as_prin_code == null || as_prin_code.isEmpty()) as_prin_code = prefs.getString("PRIN_CODE", "");
        if (as_job_no == null || as_job_no.isEmpty()) as_job_no = prefs.getString("JOB_NUMBER", "");
        if (as_order_no == null || as_order_no.isEmpty()) as_order_no = prefs.getString("ORDER_NUMBER", "");
        if (as_login_id == null || as_login_id.isEmpty()) as_login_id = prefs.getString("CURRENT_USER_ID", "");

        if (!as_prin_code.isEmpty() && !as_job_no.isEmpty() && !as_order_no.isEmpty() && !as_login_id.isEmpty()) {
            fetchOrderSummary(as_prin_code, as_job_no, as_order_no, as_login_id);
        } else {
            runOnUiThread(() -> {
                /* Toast.makeText(this, "No valid data available", Toast.LENGTH_SHORT).show();*/
            });
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