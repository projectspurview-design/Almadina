package com.example.supportapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PickLocationActivity extends AppCompatActivity implements LocationAdapter.OnLocationClickListener {

    private static final String TAG = "PickLocationActivity";
    private static final String PREFS_NAME = "AppPrefs";

    private static final String BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/ORDER_DETAILS";
    // REVERTED API_KEY TO USE TILDE '~' as it works in EighthActivity
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";

    public static final String EXTRA_SELECTED_LOCATION = "com.example.supportapp.SELECTED_LOCATION";
    public static final String EXTRA_SELECTED_SITE = "com.example.supportapp.SELECTED_SITE";
    public static final String EXTRA_SELECTED_CODE = "com.example.supportapp.SELECTED_CODE";
    public static final String EXTRA_SELECTED_QUANTITY = "com.example.supportapp.SELECTED_QUANTITY";

    private RecyclerView rvLocations;
    private LocationAdapter locationAdapter;
    private List<Location> locationList;
    private ImageView arrowUpLocation, arrowDownLocation;
    private AppCompatButton btnBackLocation, btnSelectLocation;
    private LinearLayoutManager layoutManager;
    private TextView tvTitlePickLocation;
    private ProgressBar progressBar;

    private OkHttpClient httpClient;
    private String currentUserId;
    private String currentUserName;
    private String orderNumber;
    private String jobNumber;
    private String referenceCode; // This variable exists
    private String prinCode;

    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private boolean isScrollingContinuously = false;
    private static final int SCROLL_DELAY_MS = 100;
    private static final int SCROLL_AMOUNT_SINGLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        getUserDataAndReferences();
        initializeViews();

        locationList = new ArrayList<>();
        setupRecyclerView();
        setupArrowButtons();
        setupNavigationButtons();

        updateArrowVisibility();
        rvLocations.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateArrowVisibility();
            }
        });

        fetchLocationsFromServer();
    }

    private void getUserDataAndReferences() {
        currentUserId = getIntent().getStringExtra("USER_ID");
        currentUserName = getIntent().getStringExtra("USER_NAME");
        orderNumber = getIntent().getStringExtra("ORDER_NUMBER");
        jobNumber = getIntent().getStringExtra("JOB_NUMBER");
        referenceCode = getIntent().getStringExtra("REFERENCE_CODE");
        prinCode = getIntent().getStringExtra("PRIN_CODE");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        currentUserId = currentUserId != null ? currentUserId : prefs.getString("CURRENT_USER_ID", prefs.getString("LOGGED_IN_USER_ID", ""));
        currentUserName = currentUserName != null ? currentUserName : prefs.getString("CURRENT_USER_NAME", prefs.getString("LOGGED_IN_USER_NAME", "User"));
        orderNumber = orderNumber != null ? orderNumber : prefs.getString("ORDER_NUMBER", "");
        jobNumber = jobNumber != null ? jobNumber : prefs.getString("JOB_NUMBER", "");
        referenceCode = referenceCode != null ? referenceCode : prefs.getString("REFERENCE_CODE", prefs.getString("CURRENT_REFERENCE", ""));
        prinCode = prinCode != null ? prinCode : prefs.getString("PRIN_CODE", "029");

        Log.d(TAG, "User Data - ID: " + currentUserId + ", Name: " + currentUserName);
        Log.d(TAG, "Order: " + orderNumber + ", Job: " + jobNumber);
        Log.d(TAG, "Reference Code: " + referenceCode + ", Prin Code: " + prinCode);
    }

    private void initializeViews() {
        tvTitlePickLocation = findViewById(R.id.tvTitlePickLocation);
        rvLocations = findViewById(R.id.rvLocations);
        arrowUpLocation = findViewById(R.id.arrowUpLocation);
        arrowDownLocation = findViewById(R.id.arrowDownLocation);
        btnBackLocation = findViewById(R.id.btnBackLocation);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        rvLocations.setLayoutManager(layoutManager);
        locationAdapter = new LocationAdapter(locationList, this);
        rvLocations.setAdapter(locationAdapter);
    }

    private void fetchLocationsFromServer() {
        showLoading(true);

        try {
            StringBuilder urlBuilder = new StringBuilder(BASE_URL);
            urlBuilder.append("?as_prin_code=").append(URLEncoder.encode(prinCode != null ? prinCode : "029", StandardCharsets.UTF_8.toString()));

            if (jobNumber != null && !jobNumber.trim().isEmpty()) {
                urlBuilder.append("&as_job_no=").append(URLEncoder.encode(jobNumber.trim(), StandardCharsets.UTF_8.toString()));
            }

            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                urlBuilder.append("&as_order_no=").append(URLEncoder.encode(orderNumber.trim(), StandardCharsets.UTF_8.toString()));
            }

            if (currentUserId != null && !currentUserId.trim().isEmpty()) {
                urlBuilder.append("&as_login_id=").append(URLEncoder.encode(currentUserId.trim(), StandardCharsets.UTF_8.toString()));
            }

            // ADDING REFERENCE CODE as requested, assuming 'as_reference_code' is the parameter name.
            // If the API expects a different name, please update 'as_reference_code'.
            if (referenceCode != null && !referenceCode.trim().isEmpty()) {
                urlBuilder.append("&as_reference_code=").append(URLEncoder.encode(referenceCode.trim(), StandardCharsets.UTF_8.toString()));
                Log.d(TAG, "Sending reference code: " + referenceCode);
            }


            String finalUrl = urlBuilder.toString();
            Log.d(TAG, "Making request to: " + finalUrl);

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .get()
                    .addHeader("accept", "application/json")
                    // CHANGED: Header name from "X-API-Key" to "XApiKey" to match working EighthActivity
                    .addHeader("XApiKey", API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Network request failed", e);
                    runOnUiThread(() -> {
                        showLoading(false);
                        handleApiError("Network connection failed. Please check your internet connection and try again.\n\nDetails: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseString = "";
                    try {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            responseString = responseBody.string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading response body", e);
                        responseString = "Error reading response";
                    }

                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Response Headers: " + response.headers().toString());
                    Log.d(TAG, "Response Body: " + responseString);

                    final String finalResponseString = responseString;

                    if (response.isSuccessful()) {
                        parseLocationData(finalResponseString);
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            handleApiError(getErrorMessage(response.code(), finalResponseString));
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating request", e);
            showLoading(false);
            handleApiError("Error creating request: " + e.getMessage());
        }
    }

    private String getErrorMessage(int responseCode, String responseBody) {
        String baseMessage;
        switch (responseCode) {
            case 401:
                baseMessage = "Authentication failed (401).\n\nPossible causes:\n" +
                        "• Invalid API key\n" +
                        "• Expired authentication\n" +
                        "• Missing login credentials\n" +
                        "• Server authentication issue";
                break;
            case 403:
                baseMessage = "Access forbidden (403).\n\nPossible causes:\n" +
                        "• User doesn't have permission\n" +
                        "• API key lacks required permissions\n" +
                        "• Account restrictions";
                break;
            case 404:
                baseMessage = "Service not found (404).\n\nPossible causes:\n" +
                        "• API endpoint changed\n" +
                        "• Server configuration issue\n" +
                        "• Network routing problem";
                break;
            case 422:
                baseMessage = "Invalid request data (422).\n\nPossible causes:\n" +
                        "• Missing required parameters\n" +
                        "• Invalid parameter values\n" +
                        "• Data format issue";
                break;
            case 500:
                baseMessage = "Server error (500).\n\nThis is a server-side issue. Please try again later or contact support.";
                break;
            case 502:
                baseMessage = "Bad gateway (502).\n\nServer is temporarily unavailable. Please try again in a few minutes.";
                break;
            case 503:
                baseMessage = "Service unavailable (503).\n\nServer is temporarily down for maintenance. Please try again later.";
                break;
            case 504:
                baseMessage = "Gateway timeout (504).\n\nServer took too long to respond. Please try again.";
                break;
            default:
                baseMessage = "Server error (" + responseCode + ").\n\nUnexpected server response.";
        }

        // Add response body if it contains useful information
        if (responseBody != null && !responseBody.trim().isEmpty() && responseBody.length() < 200) {
            try {
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("message")) {
                    baseMessage += "\n\nServer message: " + errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    baseMessage += "\n\nServer error: " + errorJson.getString("error");
                }
            } catch (JSONException e) {
                // If not JSON, include raw response if it's short and readable
                if (responseBody.length() < 100 && responseBody.matches(".*[a-zA-Z].*")) {
                    baseMessage += "\n\nServer response: " + responseBody;
                }
            }
        }

        return baseMessage;
    }

    private void parseLocationData(String jsonResponse) {
        try {
            Log.d(TAG, "Parsing JSON response: " + jsonResponse);

            // Handle empty response
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                runOnUiThread(() -> {
                    showLoading(false);
                    handleApiError("Empty response from server. Please try again.");
                });
                return;
            }

            JSONArray locationsArray = null;

            // Try to parse as different possible formats
            try {
                if (jsonResponse.trim().startsWith("[")) {
                    locationsArray = new JSONArray(jsonResponse);
                } else {
                    JSONObject responseObject = new JSONObject(jsonResponse);

                    // Check common response formats
                    if (responseObject.has("data")) {
                        Object dataObject = responseObject.get("data");
                        if (dataObject instanceof JSONArray) {
                            locationsArray = (JSONArray) dataObject;
                        } else if (dataObject instanceof JSONObject) {
                            // Single object, wrap in array
                            locationsArray = new JSONArray();
                            locationsArray.put(dataObject);
                        }
                    } else if (responseObject.has("locations")) {
                        locationsArray = responseObject.getJSONArray("locations");
                    } else if (responseObject.has("items")) {
                        locationsArray = responseObject.getJSONArray("items");
                    } else if (responseObject.has("results")) {
                        locationsArray = responseObject.getJSONArray("results");
                    } else {
                        // If response is a single object with location data, wrap it
                        if (responseObject.has("location_code") || responseObject.has("code") ||
                                responseObject.has("item_code") || responseObject.has("prod_code")) {
                            locationsArray = new JSONArray();
                            locationsArray.put(responseObject);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error for response format", e);
            }

            if (locationsArray == null || locationsArray.length() == 0) {
                runOnUiThread(() -> {
                    showLoading(false);
                    handleApiError("No location data found in response.\n\nThis could mean:\n" +
                            "• No locations available for the given parameters\n" +
                            "• Server returned unexpected data format\n" +
                            "• Data filtering issue\n\n" +
                            "Raw response: " + (jsonResponse.length() > 200 ?
                            jsonResponse.substring(0, 200) + "..." : jsonResponse));
                });
                return;
            }

            List<Location> fetchedLocations = new ArrayList<>();

            for (int i = 0; i < locationsArray.length(); i++) {
                try {
                    JSONObject locationObj = locationsArray.getJSONObject(i);

                    String locationCode = getJsonString(locationObj, "location_code", "code", "item_code", "loc_code");
                    String prodCode = getJsonString(locationObj, "prod_code", "product_code", "sku", "item_id");
                    String prodDesc = getJsonString(locationObj, "prod_desc", "description", "product_description", "name", "item_name");
                    String quantity = getJsonString(locationObj, "quantity", "qty", "stock_qty", "available_qty");

                    // Skip if essential data is missing
                    if (locationCode.isEmpty() && prodCode.isEmpty()) {
                        Log.w(TAG, "Skipping location " + i + " - missing essential identifiers");
                        continue;
                    }

                    String locationName = prodDesc.isEmpty() ? prodCode : prodDesc;
                    if (locationName.isEmpty()) {
                        locationName = locationCode.isEmpty() ? "Unknown Location" : locationCode;
                    }

                    int quantityInt = 0;
                    try {
                        quantityInt = Integer.parseInt(quantity.isEmpty() ? "0" : quantity);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid quantity format: " + quantity + ", defaulting to 0");
                    }

                    Location location = new Location(locationCode, locationName, prodCode, locationCode, quantityInt);
                    fetchedLocations.add(location);
                    Log.d(TAG, "Parsed location: " + location.toString());

                } catch (JSONException e) {
                    Log.w(TAG, "Error parsing location " + i + ", skipping", e);
                }
            }

            runOnUiThread(() -> {
                showLoading(false);
                if (fetchedLocations.isEmpty()) {
                    handleApiError("No valid location data could be parsed from the response.\n\n" +
                            "This might be due to unexpected data format or missing required fields.");
                } else {
                    updateLocationList(fetchedLocations);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during parsing", e);
            runOnUiThread(() -> {
                showLoading(false);
                handleApiError("Unexpected error while processing server response:\n" + e.getMessage());
            });
        }
    }

    private String getJsonString(JSONObject jsonObject, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (jsonObject.has(fieldName) && !jsonObject.isNull(fieldName)) {
                try {
                    return jsonObject.getString(fieldName).trim();
                } catch (JSONException e) {
                    // Continue to next field name
                }
            }
        }
        return "";
    }

    private void updateLocationList(List<Location> newLocations) {
        locationList.clear();
        locationList.addAll(newLocations);
        locationAdapter.notifyDataSetChanged();

        if (!locationList.isEmpty()) {
            locationAdapter.setSelectedPosition(0);
        }

        updateArrowVisibility();

        String message = "Successfully loaded " + newLocations.size() + " location" +
                (newLocations.size() == 1 ? "" : "s");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSelectLocation.setEnabled(!show);
        arrowUpLocation.setEnabled(!show);
        arrowDownLocation.setEnabled(!show);

        if (show) {
            btnSelectLocation.setText("LOADING...");
        } else {
            btnSelectLocation.setText("SELECT");
        }
    }

    private void handleApiError(String errorMessage) {
        Log.e(TAG, "API Error: " + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        locationList.clear();
        locationAdapter.notifyDataSetChanged();
        updateArrowVisibility();

        btnSelectLocation.setText("RETRY");
        btnSelectLocation.setOnClickListener(v -> {
            btnSelectLocation.setText("SELECT");
            setupNavigationButtons();
            fetchLocationsFromServer();
        });
    }

    private void setupArrowButtons() {
        arrowUpLocation.setOnClickListener(v -> navigateListByOneStep(-1));
        arrowDownLocation.setOnClickListener(v -> navigateListByOneStep(1));

        View.OnTouchListener continuousScrollListener = (v, event) -> {
            int direction = (v.getId() == R.id.arrowUpLocation) ? -1 : 1;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isScrollingContinuously = true;
                    scrollHandler.post(new ContinuousScroller(direction));
                    v.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isScrollingContinuously = false;
                    scrollHandler.removeCallbacksAndMessages(null);
                    v.setPressed(false);
                    return true;
            }
            return false;
        };

        arrowUpLocation.setOnTouchListener(continuousScrollListener);
        arrowDownLocation.setOnTouchListener(continuousScrollListener);
    }

    private class ContinuousScroller implements Runnable {
        private final int direction;
        ContinuousScroller(int direction) { this.direction = direction; }
        @Override
        public void run() {
            if (isScrollingContinuously) {
                navigateListByOneStep(direction);
                scrollHandler.postDelayed(this, SCROLL_DELAY_MS);
            }
        }
    }

    private void navigateListByOneStep(int direction) {
        if (locationList.isEmpty() || locationAdapter == null) return;

        int currentSelectedPos = locationAdapter.getSelectedPosition();
        if (currentSelectedPos == RecyclerView.NO_POSITION && !locationList.isEmpty()) {
            currentSelectedPos = (direction > 0) ? -1 : 0;
        }

        int newPosition = currentSelectedPos + direction;

        if (newPosition >= locationList.size()) {
            newPosition = locationList.size() - 1;
        } else if (newPosition < 0) {
            newPosition = 0;
        }

        if (newPosition != currentSelectedPos || locationAdapter.getSelectedPosition() == RecyclerView.NO_POSITION) {
            locationAdapter.setSelectedPosition(newPosition);
            rvLocations.smoothScrollToPosition(newPosition);
        }
        updateArrowVisibility();
    }

    private void setupNavigationButtons() {
        btnBackLocation.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        btnSelectLocation.setOnClickListener(v -> {
            Location selectedLocation = locationAdapter.getSelectedItem();
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SELECTED_LOCATION, selectedLocation.getName());
                resultIntent.putExtra(EXTRA_SELECTED_SITE, selectedLocation.getSite());
                resultIntent.putExtra(EXTRA_SELECTED_CODE, selectedLocation.getLocationCode());
                resultIntent.putExtra(EXTRA_SELECTED_QUANTITY, selectedLocation.getQuantity());

                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "No location selected.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocationClick(int position, Location location) {
        rvLocations.smoothScrollToPosition(position);
        updateArrowVisibility();
    }

    private void updateArrowVisibility() {
        if (layoutManager == null || locationAdapter == null || locationList.isEmpty()) {
            arrowUpLocation.setVisibility(View.INVISIBLE);
            arrowDownLocation.setVisibility(View.INVISIBLE);
            return;
        }

        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        int lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
        int itemCount = locationAdapter.getItemCount();

        arrowUpLocation.setVisibility(firstCompletelyVisibleItemPosition > 0 ? View.VISIBLE : View.INVISIBLE);
        arrowDownLocation.setVisibility(lastCompletelyVisibleItemPosition < itemCount - 1 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isScrollingContinuously = false;
        scrollHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}