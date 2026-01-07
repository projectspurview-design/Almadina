package com.example.supportapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EighthActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/PICKLIST_JOB";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";
    private static final String TAG = "EighthActivity";

    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eighth);

        // Initialize executor service for background tasks
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // ENHANCED: Multiple fallback methods to get user data
        currentUserId = getUserId();
        currentUserName = getUserName();

        Log.d(TAG, "Final User Data - ID: '" + currentUserId + "', Name: '" + currentUserName + "'");

        // Initialize UI components
        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);
        Button nextButton = findViewById(R.id.btnBottomRight);

        // ENHANCED: Check if we have valid user data before proceeding
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Log.e(TAG, "CRITICAL: No user ID available after all fallback attempts");
            showUserDataError();
            return; // Don't proceed without user data
        }

        // Clear pause state on fresh app start
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove("EighthActivityPaused").apply();

        // Initialize UI to default state
        mohebiText.setText("Loading data for " + currentUserName + "...");
        orderNumberText.setText("Order number: Loading...");
        jobNumberText.setText("Job number: Loading...");
        jobNumberText1.setText("Loading...");
        pausedStatusText.setVisibility(TextView.GONE);
        nextButton.setText("Next");

        // Load picklist job data from API
        loadPicklistJobData(currentUserId);

        // Set up button listeners
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> handleNextButtonClick());
    }

    // ENHANCED: Comprehensive method to get user ID with multiple fallbacks
    private String getUserId() {
        // Method 1: From Intent
        String userId = getIntent().getStringExtra("USER_ID");
        if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "Got user ID from Intent: " + userId);
            return userId.trim();
        }

        // Method 2: From SharedPreferences - multiple keys
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Try CURRENT_USER_ID first
        userId = prefs.getString("CURRENT_USER_ID", null);
        if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "Got user ID from CURRENT_USER_ID: " + userId);
            return userId.trim();
        }

        // Try LOGGED_IN_USER_ID
        userId = prefs.getString("LOGGED_IN_USER_ID", null);
        if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "Got user ID from LOGGED_IN_USER_ID: " + userId);
            return userId.trim();
        }

        // Method 3: Try to use username as ID
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

    // ENHANCED: Comprehensive method to get user name
    private String getUserName() {
        // Method 1: From Intent
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d(TAG, "Got user name from Intent: " + userName);
            return userName.trim();
        }

        // Method 2: From SharedPreferences
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

        // Method 3: Use user ID as name if available
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            Log.d(TAG, "Using user ID as name: " + currentUserId);
            return currentUserId.trim();
        }

        Log.w(TAG, "No user name found, using default");
        return "User";
    }

    // NEW: Show specific error when no user data is available
    private void showUserDataError() {
        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);
        Button nextButton = findViewById(R.id.btnBottomRight);

        mohebiText.setText("ERROR: No User Login Data");
        orderNumberText.setText("Please login again");
        jobNumberText.setText("User session expired");
        jobNumberText1.setText("Return to login screen");
        pausedStatusText.setVisibility(TextView.GONE);

        nextButton.setText("Back to Login");
        nextButton.setOnClickListener(v -> {
            // Clear all user data and go back to login
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().clear().apply();

            Toast.makeText(this, "Please scan QR code to login again", Toast.LENGTH_LONG).show();

            // Go back to FourthActivity (QR scan login)
            Intent intent = new Intent(EighthActivity.this, FourthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        Toast.makeText(this,
                "CRITICAL ERROR: No user information available. Please login again.",
                Toast.LENGTH_LONG).show();
    }

    // ENHANCED: Better next button handling
    // ENHANCED: Better next button handling
    private void handleNextButtonClick() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "Error: No user information. Please login again.", Toast.LENGTH_LONG).show();
            showUserDataError();
            return;
        }

        Toast.makeText(EighthActivity.this, "Finalizing and moving to next step...", Toast.LENGTH_SHORT).show();

        SharedPreferences currentPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String orderNumber = currentPrefs.getString("ORDER_NUMBER", "");
        String jobNumber = currentPrefs.getString("JOB_NUMBER", "");
        String location = currentPrefs.getString("LOCATION", "");
        String prinCode = currentPrefs.getString("PRIN_CODE", ""); // ✅ NEW: Get PRIN_CODE

        currentPrefs.edit()
                .putBoolean("finishedStep8", true)
                .remove("EighthActivityPaused")
                .apply();

        // Reset UI state
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);
        Button nextButton = findViewById(R.id.btnBottomRight);
        pausedStatusText.setVisibility(TextView.GONE);
        nextButton.setText("Next");

        // ✅ Pass all required extras, including PRIN_CODE
        Intent intent = new Intent(EighthActivity.this, NinthActivity.class);
        intent.putExtra("ORDER_NUMBER", orderNumber);
        intent.putExtra("JOB_NUMBER", jobNumber);
        intent.putExtra("LOCATION", location);
        intent.putExtra("USER_ID", currentUserId);
        intent.putExtra("USER_NAME", currentUserName);
        intent.putExtra("PRIN_CODE", prinCode); // ✅ NEW: Include this

        startActivity(intent);
    }


    private void loadPicklistJobData(String loginId) {
        Log.d(TAG, "Loading picklist data for user: " + loginId);

        executorService.execute(() -> {
            try {
                // FIXED: Construct URL with query parameter for the actual logged-in user
                // Make sure the parameter name matches what the API expects
                String urlString = BASE_URL + "?as_login_id=" + loginId;
                URL url = new URL(urlString);

                Log.d(TAG, "API URL: " + urlString);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("XApiKey", API_KEY);
                connection.setConnectTimeout(15000); // Increased timeout
                connection.setReadTimeout(20000); // Increased timeout

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

                    // FIXED: Better response validation
                    if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                        Log.e(TAG, "Empty response from API for user: " + loginId);
                        mainHandler.post(() -> {
                            showNoDataMessage();
                            Toast.makeText(EighthActivity.this,
                                    "Empty response from server for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    // Parse JSON response and update UI
                    parsePicklistResponse(jsonResponse);

                } else {
                    // Handle error response
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;

                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Log.e(TAG, "API Error Response Code: " + responseCode + " for user " + loginId + ": " + errorResponse.toString());

                    mainHandler.post(() -> {
                        // FIXED: Show more specific error based on response code
                        if (responseCode == 404) {
                            showNoDataMessage();
                            Toast.makeText(EighthActivity.this,
                                    "No jobs found for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                        } else if (responseCode == 401 || responseCode == 403) {
                            Toast.makeText(EighthActivity.this,
                                    "Authentication failed for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                            showUserDataError();
                        } else {
                            showFallbackData();
                            Toast.makeText(EighthActivity.this,
                                    "Server error (" + responseCode + ") for user: " + currentUserName,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                connection.disconnect();

            } catch (IOException e) {
                Log.e(TAG, "Network error for user " + loginId + ": " + e.getMessage(), e);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(EighthActivity.this,
                            "Network error for user: " + currentUserName + " - " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error for user " + loginId + ": " + e.getMessage(), e);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(EighthActivity.this,
                            "Error loading data for user: " + currentUserName + " - " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void parsePicklistResponse(String jsonResponse) {
        try {
            Log.d(TAG, "Raw JSON Response for user " + currentUserId + ": " + jsonResponse);

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                Log.e(TAG, "Empty JSON response for user: " + currentUserId);
                mainHandler.post(() -> {
                    showNoDataMessage();
                    Toast.makeText(EighthActivity.this,
                            "Empty response from server for user: " + currentUserName,
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // FIXED: Better JSON validation and parsing
            String trimmedResponse = jsonResponse.trim();

            // Check if response starts with [ (array) or { (object)
            if (!trimmedResponse.startsWith("[") && !trimmedResponse.startsWith("{")) {
                Log.e(TAG, "Invalid JSON format for user " + currentUserId + ": " + trimmedResponse);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(EighthActivity.this,
                            "Invalid response format from server for user: " + currentUserName,
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Parse as JSONArray since your API returns an array
            org.json.JSONArray jsonArray = new org.json.JSONArray(trimmedResponse);
            Log.d(TAG, "Response is a JSON Array with " + jsonArray.length() + " items for user: " + currentUserId);

            if (jsonArray.length() == 0) {
                Log.w(TAG, "Empty JSON Array for user: " + currentUserId);
                mainHandler.post(() -> {
                    showNoDataMessage();
                    Toast.makeText(EighthActivity.this,
                            "No picklist jobs found for user: " + currentUserName,
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Get the first object from the array
            JSONObject firstJob = jsonArray.getJSONObject(0);
            Log.d(TAG, "Using first job from array for user " + currentUserId + ": " + firstJob.toString());

            // FIXED: Validate that the job object has the required fields
            if (!firstJob.has("prin_code") && !firstJob.has("order_no") && !firstJob.has("job_no")) {
                Log.e(TAG, "Job object missing required fields for user " + currentUserId);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(EighthActivity.this,
                            "Invalid job data format for user: " + currentUserName,
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Update UI on main thread
            mainHandler.post(() -> {
                updateUIWithBackendData(firstJob, jsonArray.length());
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON Array parsing error for user " + currentUserId + ": " + e.getMessage(), e);

            // FIXED: Try parsing as single object if array parsing fails
            try {
                JSONObject singleJob = new JSONObject(jsonResponse);
                Log.d(TAG, "Parsed as single JSON object for user " + currentUserId + ": " + singleJob.toString());

                mainHandler.post(() -> {
                    updateUIWithBackendData(singleJob, 1);
                });

            } catch (JSONException e2) {
                Log.e(TAG, "Both JSON Array and Object parsing failed for user " + currentUserId + ": " + e2.getMessage(), e2);
                mainHandler.post(() -> {
                    showFallbackData();
                    Toast.makeText(EighthActivity.this,
                            "Invalid data format from server for user: " + currentUserName,
                            Toast.LENGTH_LONG).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "General parsing error for user " + currentUserId + ": " + e.getMessage(), e);
            mainHandler.post(() -> {
                showFallbackData();
                Toast.makeText(EighthActivity.this,
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

            Log.d(TAG, "Updating UI with backend data for user: " + currentUserId);

            // FIXED: Better data extraction with more detailed logging
            String prinCode = jobData.optString("prin_code", "").trim();
            String prinName = jobData.optString("prin_name", "").trim();
            String orderNumber = jobData.optString("order_no", "").trim();
            String jobNumber = jobData.optString("job_no", "").trim();
            String custName = jobData.optString("cust_name", "").trim();
            String pdaVerified = jobData.optString("pdA_VERIFIED", "N").trim();
            String isProgress = jobData.optString("iS_PROGRESS", "N").trim();

            Log.d(TAG, "Extracted data for user " + currentUserId + ":");
            Log.d(TAG, "  prin_code: '" + prinCode + "'");
            Log.d(TAG, "  prin_name: '" + prinName + "'");
            Log.d(TAG, "  order_no: '" + orderNumber + "'");
            Log.d(TAG, "  job_no: '" + jobNumber + "'");
            Log.d(TAG, "  cust_name: '" + custName + "'");
            Log.d(TAG, "  pdA_VERIFIED: '" + pdaVerified + "'");
            Log.d(TAG, "  iS_PROGRESS: '" + isProgress + "'");

            // Update company name - from server data
            if (!prinCode.isEmpty() || !prinName.isEmpty()) {
                String companyDisplay = "";
                if (!prinCode.isEmpty() && !prinName.isEmpty()) {
                    companyDisplay = prinCode + " " + prinName;
                } else if (!prinCode.isEmpty()) {
                    companyDisplay = prinCode;
                } else {
                    companyDisplay = prinName;
                }
                if (!prinCode.isEmpty()) {
                    editor.putString("PRIN_CODE", prinCode);  // ✅ Store PRIN_CODE for next screen
                }

                mohebiText.setText(companyDisplay);
                Log.d(TAG, "Company for user " + currentUserId + ": " + companyDisplay);
            } else {
                mohebiText.setText("Company: " + currentUserName);
                Log.w(TAG, "No company data for user " + currentUserId);
            }

            // Update order number - from server data
            if (!orderNumber.isEmpty()) {
                orderNumberText.setText("Order number: " + orderNumber);
                editor.putString("ORDER_NUMBER", orderNumber);
                Log.d(TAG, "Order Number for user " + currentUserId + ": " + orderNumber);
            } else {
                orderNumberText.setText("Order number: Not available");
                Log.w(TAG, "No order number for user " + currentUserId);
            }

            // Update job number - from server data
            if (!jobNumber.isEmpty()) {
                jobNumberText.setText("Job number: " + jobNumber);
                editor.putString("JOB_NUMBER", jobNumber);
                Log.d(TAG, "Job Number for user " + currentUserId + ": " + jobNumber);
            } else {
                jobNumberText.setText("Job number: Not available");
                Log.w(TAG, "No job number for user " + currentUserId);
            }

            // Update location/customer info - from server data
            if (!custName.isEmpty()) {
                jobNumberText1.setText(custName);
                editor.putString("LOCATION", custName);
                Log.d(TAG, "Customer/Location for user " + currentUserId + ": " + custName);
            } else {
                jobNumberText1.setText("Location: Not available");
                Log.w(TAG, "No customer name for user " + currentUserId);
            }

            // Handle verification and progress status from server
            Log.d(TAG, "For user " + currentUserId + " - PDA Verified: " + pdaVerified + ", Is Progress: " + isProgress);

            // Update status based on server data
            if ("Y".equalsIgnoreCase(isProgress)) {
                pausedStatusText.setText("In Progress");
                pausedStatusText.setVisibility(TextView.VISIBLE);
                editor.putBoolean("EighthActivityPaused", true);
                Button nextButton = findViewById(R.id.btnBottomRight);
                nextButton.setText("Resume");
            } else if ("Y".equalsIgnoreCase(pdaVerified)) {
                pausedStatusText.setText("Verified");
                pausedStatusText.setVisibility(TextView.VISIBLE);
            } else {
                pausedStatusText.setVisibility(TextView.GONE);
                editor.remove("EighthActivityPaused");
            }

            // Save all changes
            boolean saved = editor.commit();
            Log.d(TAG, "SharedPreferences save result: " + saved);

            Toast.makeText(EighthActivity.this,
                    "Data loaded successfully for " + currentUserName + ": " + totalJobs + " job" + (totalJobs > 1 ? "s" : "") + " found",
                    Toast.LENGTH_SHORT).show();

            // Reset next button click listener to normal behavior
            Button nextButton = findViewById(R.id.btnBottomRight);
            nextButton.setOnClickListener(v -> handleNextButtonClick());

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with backend data for user " + currentUserId + ": " + e.getMessage(), e);
            showFallbackData();
            Toast.makeText(EighthActivity.this,
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

        mohebiText.setText("No jobs assigned to " + currentUserName);
        orderNumberText.setText("Order number: No active orders");
        jobNumberText.setText("Job number: No active jobs");
        jobNumberText1.setText("No location assigned");
        pausedStatusText.setVisibility(TextView.GONE);

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
        // Show error message when API fails - no hardcoded data
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

        Button nextButton = findViewById(R.id.btnBottomRight);
        nextButton.setText("Retry");
        nextButton.setOnClickListener(v -> {
            // Retry loading data
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
                nextButton.setText("Resume");
            } else {
                prefs.edit().remove("EighthActivityPaused").apply();
                pausedStatusText.setVisibility(TextView.GONE);
                nextButton.setText("Next");
            }
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