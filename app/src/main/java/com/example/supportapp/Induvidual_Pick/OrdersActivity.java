package com.example.supportapp.Induvidual_Pick;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.example.supportapp.Induvidual_Pick.adapter.OptionAdapter;
import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.R;
import com.example.supportapp.voice.VoiceCommandCenter;
import com.example.supportapp.voice.VoiceCommandCenter.Actions;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

public class OrdersActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/PICKLIST_JOB";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";
    private static final String TAG = "OrdersActivity";

    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentUserId;
    private String currentUserName;
    private JSONArray jobList = new JSONArray();
    private int currentJobIndex = 0;
    private VuzixSpeechClient speechClient;

    // UI
    private TextView progressText;
    private TextView customerNameText;
    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

    // NOTE: Removed local VuzixSpeechClient & keycode constants.
    // Voice handling is centralized via VoiceCommandCenter.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ordersactivity);

        // 🔊 Register/ensure voice phrases via centralized command center
        VoiceCommandCenter.init(this);

        // Initialize executor & handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // User data
        currentUserId = getUserId();
        currentUserName = getUserName();
        Log.d(TAG, "Final User Data - ID: '" + currentUserId + "', Name: '" + currentUserName + "'");

        // UI refs
        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);
        Button nextButton = findViewById(R.id.btnBottomRight);
        progressText = findViewById(R.id.progressText);
        customerNameText = findViewById(R.id.customerNameText);
        Button logoutButton = findViewById(R.id.logoutButton);

        ImageView btnArrowUp = findViewById(R.id.arrowUp);
        ImageView btnArrowDown = findViewById(R.id.arrowDown);

        btnArrowUp.setOnClickListener(v -> showPreviousJob());
        btnArrowDown.setOnClickListener(v -> showNextJob());

        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(OrdersActivity.this);
        });

        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Log.e(TAG, "CRITICAL: No user ID available after all fallback attempts");
            showUserDataError();
            return;
        }

        // Clear pause state on fresh app start
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove("EighthActivityPaused").apply();

        // Initialize UI defaults
        mohebiText.setText("Principal: " + currentUserName + "...");
        orderNumberText.setText("Order# Loading...");
        jobNumberText.setText("Job#: Loading...");
        jobNumberText1.setText("Loading...");
        pausedStatusText.setVisibility(TextView.GONE);
        nextButton.setText("Next");
        progressText.setText("0/0");
        customerNameText.setText("Customer/store: Loading...");

        // Load data
        loadPicklistJobData(currentUserId);

        findViewById(R.id.backButton).setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked - navigating to SeventhActivity");

            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            prefs.edit().remove("EighthActivityPaused").apply();

            Intent intent = new Intent(OrdersActivity.this, OutBoundActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        setupVoiceCommands();
    }

    private void setupVoiceCommands() {
        try {

            speechClient = new VuzixSpeechClient(this);

            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");




            Log.d(TAG, "Voice commands registered");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Ensure phrases are active whenever the screen is foregrounded
        VoiceCommandCenter.init(this);
    }

    // Keep existing ScrollUp/Down (unused by voice; left intact)
    private void ScrollUp() {
        if (selectedPosition > 0) {
            selectedPosition--;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Moved up to: " + options.get(selectedPosition));
        }
    }

    private void ScrollDown() {
        if (selectedPosition < options.size() - 1) {
            selectedPosition++;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Moved down to: " + options.get(selectedPosition));
        }
    }

    private void goBack() {
        Log.d(TAG, "Going back to previous activity");
        finish();
    }

    private void goToNextIfIndividual() {
        if (selectedPosition == 0) {
            Intent intent = new Intent(OrdersActivity.this, OrderSummaryActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
            Log.d(TAG, "Navigating to EighthActivity");
        } else {
            Log.d(TAG, "Cannot proceed - Pick Individual not selected");
        }
    }

    // 🔊 Map centralized voice actions → existing methods (keeps original behavior)
    private final Actions voiceActions = new Actions() {
        @Override public void onNext()        { goToNextIfIndividual(); }
        @Override public void onBack()        { goBack(); }
        @Override public void onScrollUp()    { showPreviousJob(); }  // your original mapping
        @Override public void onScrollDown()  { showNextJob(); }      // your original mapping
        @Override public void onSelect()      { /* no-op in this screen */ }

        @Override public void onInbound()     { /* not used here */ }
        @Override public void onOutbound()    { /* not used here */ }
        @Override public void onInventory()   { /* not used here */ }
        @Override public void onIndividual()  { /* not used here */ }
        @Override public void onConsolidated(){ /* not used here */ }

        @Override public void onLogout()      {
            Log.d(TAG, "Voice logout command triggered");
            LogoutManager.performLogout(OrdersActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Route all key events through the centralized handler
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    // ======== (Everything below is your original data / UI logic, unchanged) ========

    private String getUserId() {
        String userId = getIntent().getStringExtra("USER_ID");
        if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "Got user ID from Intent: " + userId);
            return userId.trim();
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        userId = prefs.getString("CURRENT_USER_ID", null);
        if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "Got user ID from CURRENT_USER_ID: " + userId);
            return userId.trim();
        }

        userId = prefs.getString("LOGGED_IN_USER_ID", null);
        if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "Got user ID from LOGGED_IN_USER_ID: " + userId);
            return userId.trim();
        }

        String userName = prefs.getString("CURRENT_USER_NAME", null);
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d(TAG, "Using username as user ID: " + userName);
            return userName.trim();
        }

        userName = prefs.getString("LOGGED_IN_USER_NAME", null);
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d(TAG, "Using logged-in username as user ID: " + userName);
            return userName.trim();
        }

        Log.e(TAG, "No user ID found in any location");
        return null;
    }

    private String getUserName() {
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d(TAG, "Got user name from Intent: " + userName);
            return userName.trim();
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        userName = prefs.getString("CURRENT_USER_NAME", null);
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d(TAG, "Got user name from CURRENT_USER_NAME: " + userName);
            return userName.trim();
        }

        userName = prefs.getString("LOGGED_IN_USER_NAME", null);
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d(TAG, "Got user name from LOGGED_IN_USER_NAME: " + userName);
            return userName.trim();
        }

        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            Log.d(TAG, "Using user ID as name: " + currentUserId);
            return currentUserId.trim();
        }

        Log.w(TAG, "No user name found, using default");
        return "User";
    }

    private void showUserDataError() {
        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);

        mohebiText.setText("ERROR: No User Login Data");
        orderNumberText.setText("Please login again");
        jobNumberText.setText("User session expired");
        jobNumberText1.setText("Return to login screen");
        pausedStatusText.setVisibility(TextView.GONE);
        progressText.setText("0/0");
        customerNameText.setText("Customer/store: Not available");

        Button nextButton = findViewById(R.id.btnBottomRight);
        nextButton.setText("Back to Login");
        nextButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().clear().apply();

            Toast.makeText(this, "Please scan QR code to login again", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(OrdersActivity.this, Barcodescanner.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        Toast.makeText(this,
                "CRITICAL ERROR: No user information available. Please login again.",
                Toast.LENGTH_LONG).show();
    }

    private void handleNextButtonClick() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "Error: No user information. Please login again.", Toast.LENGTH_LONG).show();
            showUserDataError();
            return;
        }

        Toast.makeText(OrdersActivity.this, "Finalizing and moving to next step...", Toast.LENGTH_SHORT).show();

        SharedPreferences currentPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String orderNumber = currentPrefs.getString("ORDER_NUMBER", "");
        String jobNumber = currentPrefs.getString("JOB_NUMBER", "");
        String location = currentPrefs.getString("LOCATION", "");
        String prinCode = currentPrefs.getString("PRIN_CODE", "");
        String customerName = currentPrefs.getString("CUSTOMER_NAME", "");

        currentPrefs.edit()
                .putBoolean("finishedStep8", true)
                .remove("EighthActivityPaused")
                .apply();

        TextView pausedStatusText = findViewById(R.id.pausedStatusText);
        Button nextButton = findViewById(R.id.btnBottomRight);
        pausedStatusText.setVisibility(TextView.GONE);
        nextButton.setText("Next");

        Intent intent = new Intent(OrdersActivity.this, OrderSummaryActivity.class);
        intent.putExtra("ORDER_NUMBER", orderNumber);
        intent.putExtra("JOB_NUMBER", jobNumber);
        intent.putExtra("LOCATION", location);
        intent.putExtra("USER_ID", currentUserId);
        intent.putExtra("USER_NAME", currentUserName);
        intent.putExtra("PRIN_CODE", prinCode);
        intent.putExtra("CUSTOMER_NAME", customerName);

        startActivity(intent);
    }

    private void loadPicklistJobData(String loginId) {
        Log.d(TAG, "Loading picklist data for user: " + loginId);

        executorService.execute(() -> {
            HttpsURLConnection connection = null;
            try {
                String urlString = BASE_URL + "?as_login_id=" + loginId;
                URL url = new URL(urlString);

                Log.d(TAG, "API URL: " + urlString);

                connection = (HttpsURLConnection) url.openConnection();

                setupSSLBypass(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("XApiKey", API_KEY);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API Response Code: " + responseCode + " for user: " + loginId);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String jsonResponse = response.toString();
                    Log.d(TAG, "FULL API Response for user " + loginId + ": " + jsonResponse);

                    if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                        Log.e(TAG, "Empty response from API for user: " + loginId);
                        mainHandler.post(() -> {
                            showNoDataMessage();
                            Toast.makeText(OrdersActivity.this,
                                    "Empty response from server for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    parsePicklistResponse(jsonResponse);

                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;

                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Log.e(TAG, "API Error Response Code: " + responseCode + " for user " + loginId + ": " + errorResponse.toString());

                    mainHandler.post(() -> {
                        if (responseCode == 404) {
                            showNoDataMessage();
                            Toast.makeText(OrdersActivity.this,
                                    "No jobs found for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                        } else if (responseCode == 401 || responseCode == 403) {
                            Toast.makeText(OrdersActivity.this,
                                    "Authentication failed for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                            showUserDataError();
                        } else {
                            showFallbackData();
                            Toast.makeText(OrdersActivity.this,
                                    "Server error (" + responseCode + ") for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Network error for user " + loginId + ": " + e.getMessage(), e);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(OrdersActivity.this,
                            "Network error for user: " + currentUserName + " - " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error for user " + loginId + ": " + e.getMessage(), e);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(OrdersActivity.this,
                            "Error loading data for user: " + currentUserName + " - " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void setupSSLBypass(HttpsURLConnection connection) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) { return true; }
            };

            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier(allHostsValid);

            Log.d(TAG, "SSL bypass configured for HTTPS connection");

        } catch (Exception e) {
            Log.e(TAG, "Failed to setup SSL bypass: " + e.getMessage());
        }
    }

    private void parsePicklistResponse(String jsonResponse) {
        try {
            Log.d(TAG, "Raw JSON Response for user " + currentUserId + ": " + jsonResponse);

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                Log.e(TAG, "Empty JSON response for user: " + currentUserId);
                mainHandler.post(() -> {
                    showNoDataMessage();
                    Toast.makeText(OrdersActivity.this,
                            "Empty response from server for user: " + currentUserName,
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            String trimmedResponse = jsonResponse.trim();

            if (!trimmedResponse.startsWith("[") && !trimmedResponse.startsWith("{")) {
                Log.e(TAG, "Invalid JSON format for user " + currentUserId + ": " + trimmedResponse);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(OrdersActivity.this,
                            "Invalid response format from server for user: " + currentUserName,
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            org.json.JSONArray jsonArray = new org.json.JSONArray(trimmedResponse);
            Log.d(TAG, "Response is a JSON Array with " + jsonArray.length() + " items for user: " + currentUserId);

            if (jsonArray.length() == 0) {
                Log.w(TAG, "Empty JSON Array for user: " + currentUserId);
                mainHandler.post(() -> {
                    showNoDataMessage();
                    Toast.makeText(OrdersActivity.this,
                            "No picklist jobs found for user: " + currentUserName,
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            this.jobList = jsonArray;
            this.currentJobIndex = 0;

            JSONObject currentJob = jsonArray.getJSONObject(currentJobIndex);
            Log.d(TAG, "Using job at index " + currentJobIndex + ": " + currentJob.toString());

            if (!currentJob.has("prin_code") && !currentJob.has("order_no") && !currentJob.has("job_no")) {
                Log.e(TAG, "Job object missing required fields for user " + currentUserId);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(OrdersActivity.this,
                            "Invalid job data format for user: " + currentUserName,
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            mainHandler.post(() -> {
                updateUIWithBackendData(currentJob, jsonArray.length());
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON Array parsing error for user " + currentUserId + ": " + e.getMessage(), e);

            try {
                JSONObject singleJob = new JSONObject(jsonResponse);
                Log.d(TAG, "Parsed as single JSON object for user " + currentUserId + ": " + singleJob.toString());

                this.jobList = new JSONArray();
                this.jobList.put(singleJob);
                this.currentJobIndex = 0;

                mainHandler.post(() -> updateUIWithBackendData(singleJob, 1));

            } catch (JSONException e2) {
                Log.e(TAG, "Both JSON Array and Object parsing failed for user " + currentUserId + ": " + e2.getMessage(), e2);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(OrdersActivity.this,
                            "Invalid data format from server for user: " + currentUserName,
                            Toast.LENGTH_LONG).show();
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "General parsing error for user " + currentUserId + ": " + e.getMessage(), e);
            mainHandler.post(() -> {
                showFallbackData();
                Toast.makeText(OrdersActivity.this,
                        "Error processing server data for user: " + currentUserName,
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void updateUIWithBackendData(JSONObject jobData, int totalJobs) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            TextView mohebiText = findViewById(R.id.mohebiText);
            TextView orderNumberText = findViewById(R.id.orderNumberText);
            TextView jobNumberText = findViewById(R.id.jobNumberText);
            TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
            TextView pausedStatusText = findViewById(R.id.pausedStatusText);

            if (progressText != null) {
                progressText.setText((currentJobIndex + 1) + "/" + totalJobs);
            }

            Log.d(TAG, "Updating UI with backend data for user: " + currentUserId);

            String prinCode = jobData.optString("prin_code", "").trim();
            String prinName = jobData.optString("prin_name", "").trim();
            String orderNumber = jobData.optString("order_no", "").trim();
            String jobNumberStr = jobData.optString("job_no", "").trim();
            String custName = jobData.optString("cust_name", "").trim();
            String pdaVerified = jobData.optString("pdA_VERIFIED", "N").trim();
            String isProgress = jobData.optString("iS_PROGRESS", "N").trim();

            Log.d(TAG, "Extracted data for user " + currentUserId + ":");
            Log.d(TAG, "  prin_code: '" + prinCode + "'");
            Log.d(TAG, "  prin_name: '" + prinName + "'");
            Log.d(TAG, "  order_no: '" + orderNumber + "'");
            Log.d(TAG, "  job_no: '" + jobNumberStr + "'");
            Log.d(TAG, "  cust_name: '" + custName + "'");
            Log.d(TAG, "  pdA_VERIFIED: '" + pdaVerified + "'");
            Log.d(TAG, "  iS_PROGRESS: '" + isProgress + "'");

            if (!prinCode.isEmpty() || !prinName.isEmpty()) {
                String companyDisplay;
                if (!prinCode.isEmpty() && !prinName.isEmpty()) {
                    companyDisplay = prinCode + " " + prinName;
                } else if (!prinCode.isEmpty()) {
                    companyDisplay = prinCode;
                } else {
                    companyDisplay = prinName;
                }
                if (!prinCode.isEmpty()) {
                    editor.putString("PRIN_CODE", prinCode);
                }

                mohebiText.setText("Principal: " + companyDisplay);
            } else {
                mohebiText.setText("Principal: " + currentUserName);
            }

            if (!orderNumber.isEmpty()) {
                orderNumberText.setText("Order#: " + orderNumber);
                editor.putString("ORDER_NUMBER", orderNumber);
                editor.putString("CURRENT_ORDER_NO", orderNumber);
            } else {
                orderNumberText.setText("Order number: Not available");
            }

            if (!jobNumberStr.isEmpty()) {
                jobNumberText.setText("Job#:  " + jobNumberStr);
                editor.putString("JOB_NUMBER", jobNumberStr);
                editor.putString("CURRENT_JOB_NO", jobNumberStr);
            } else {
                jobNumberText.setText("Job number: Not available");
            }

            if (!custName.isEmpty()) {
                jobNumberText1.setText(custName);
                editor.putString("LOCATION", custName);
            } else {
                jobNumberText1.setText("Location: Not available");
            }

            if (!custName.isEmpty()) {
                customerNameText.setText("Customer/store: " + custName);
                editor.putString("CUSTOMER_NAME", custName);
            } else {
                customerNameText.setText("Customer/store: Not available");
            }

            if ("Y".equalsIgnoreCase(isProgress)) {
                pausedStatusText.setText("In Progress");
                pausedStatusText.setVisibility(TextView.VISIBLE);
                editor.putBoolean("EighthActivityPaused", true);
                Button nextButton = findViewById(R.id.btnBottomRight);
                nextButton.setText("NEXT");
            } else if ("Y".equalsIgnoreCase(pdaVerified)) {
                pausedStatusText.setText("Verified");
                pausedStatusText.setVisibility(TextView.VISIBLE);
            } else {
                pausedStatusText.setVisibility(TextView.GONE);
                editor.remove("EighthActivityPaused");
            }

            boolean saved = editor.commit();
            Log.d(TAG, "SharedPreferences save result: " + saved);

            Toast.makeText(OrdersActivity.this,
                    "Data loaded successfully for " + currentUserName + ": " + totalJobs + " job" + (totalJobs > 1 ? "s" : "") + " found",
                    Toast.LENGTH_SHORT).show();

            Button nextButton = findViewById(R.id.btnBottomRight);
            nextButton.setOnClickListener(v -> handleNextButtonClick());

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with backend data for user " + currentUserId + ": " + e.getMessage(), e);
            showFallbackData();
            Toast.makeText(OrdersActivity.this,
                    "Error displaying server data for user: " + currentUserName,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showNoDataMessage() {
        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);

        mohebiText.setText("Principal:" + currentUserName);
        orderNumberText.setText("Order# No active orders");
        jobNumberText.setText("Job#:  No active jobs");
        jobNumberText1.setText("No location assigned");
        pausedStatusText.setVisibility(TextView.GONE);
        progressText.setText("0/0");
        customerNameText.setText("Customer/store: No data available");

        Button nextButton = findViewById(R.id.btnBottomRight);
        nextButton.setText("Refresh");
        nextButton.setOnClickListener(v -> {
            if (currentUserId != null && !currentUserId.isEmpty()) {
                Toast.makeText(this, "Refreshing data for " + currentUserName + "...", Toast.LENGTH_SHORT).show();
                loadPicklistJobData(currentUserId);
                nextButton.setText("Next");
            } else {
                Toast.makeText(this, "No user information available", Toast.LENGTH_SHORT).show();
                showUserDataError();
            }
        });
    }

    private void showFallbackData() {
        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);

        mohebiText.setText("Unable to load data for " + (currentUserName != null ? currentUserName : "user"));
        orderNumberText.setText("Order number: Unable to load");
        jobNumberText.setText("Job number: Unable to load");
        jobNumberText1.setText("Unable to load location");
        pausedStatusText.setVisibility(TextView.GONE);
        progressText.setText("0/0");
        customerNameText.setText("Customer/store: Unable to load");

        Button nextButton = findViewById(R.id.btnBottomRight);
        nextButton.setText("Retry");
        nextButton.setOnClickListener(v -> {
            if (currentUserId != null && !currentUserId.isEmpty()) {
                Toast.makeText(this, "Retrying data load for " + currentUserName + "...", Toast.LENGTH_SHORT).show();
                loadPicklistJobData(currentUserId);
                nextButton.setText("Next");
            } else {
                Toast.makeText(this, "No user information available", Toast.LENGTH_SHORT).show();
                showUserDataError();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            boolean paused = data.getBooleanExtra("PAUSED", false);
            TextView pausedStatusText = findViewById(R.id.pausedStatusText);
            Button nextButton = findViewById(R.id.btnBottomRight);
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (paused) {
                prefs.edit().putBoolean("EighthActivityPaused", true).apply();
                pausedStatusText.setText("Paused");
                pausedStatusText.setVisibility(TextView.VISIBLE);
                nextButton.setText("NEXT");
            } else {
                prefs.edit().remove("EighthActivityPaused").apply();
                pausedStatusText.setVisibility(TextView.GONE);
                nextButton.setText("Next");
            }
            if (jobList != null && jobList.length() > 0 && progressText != null) {
                progressText.setText((currentJobIndex + 1) + "/" + jobList.length());
            } else {
                progressText.setText("0/0");
            }
        }
    }

    private void showNextJob() {
        if (jobList != null && currentJobIndex < jobList.length() - 1) {
            currentJobIndex++;
            try {
                updateUIWithBackendData(jobList.getJSONObject(currentJobIndex), jobList.length());
            } catch (JSONException e) {
                Log.e(TAG, "Error loading next job", e);
            }
        } else {
            Toast.makeText(this, "No more jobs", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPreviousJob() {
        if (jobList != null && currentJobIndex > 0) {
            currentJobIndex--;
            try {
                updateUIWithBackendData(jobList.getJSONObject(currentJobIndex), jobList.length());
            } catch (JSONException e) {
                Log.e(TAG, "Error loading previous job", e);
            }
        } else {
            Toast.makeText(this, "Already at first job", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
