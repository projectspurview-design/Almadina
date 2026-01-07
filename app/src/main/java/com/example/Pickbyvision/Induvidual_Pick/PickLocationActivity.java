package com.example.Pickbyvision.Induvidual_Pick;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
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

import com.example.Pickbyvision.Induvidual_Pick.Location.Location;
import com.example.Pickbyvision.Induvidual_Pick.adapter.LocationAdapter;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;
import com.example.Pickbyvision.Induvidual_Pick.network.UnsafeOkHttpClient;
import com.example.Pickbyvision.R;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PickLocationActivity extends AppCompatActivity implements LocationAdapter.OnLocationClickListener {

    private static final String TAG = "PickLocationActivity";
    private static final String PREFS_NAME = "AppPrefs";

    public static final String ORDER_LOCATIONS_LIST_KEY = "ORDER_LOCATIONS";
    public static final String SELECTED_LOCATION_INDEX_KEY = "SELECTED_LOCATION_INDEX";
    public static final String CURRENT_LOCATION_INDEX_KEY = "CURRENT_LOCATION_INDEX";
    public static final String CURRENT_LOCATION_OBJECT_KEY = "CURRENT_LOCATION_OBJECT";

    private RecyclerView rvLocations;
    private LocationAdapter locationAdapter;
    private ArrayList<Location> locationList;

    private ImageView arrowUpLocation, arrowDownLocation;
    private AppCompatButton btnBackLocation, btnSelectLocation;
    private LinearLayoutManager layoutManager;
    private TextView tvTitlePickLocation;
    private ProgressBar progressBar;

    private OkHttpClient httpClient;
    private VuzixSpeechClient speechClient;

    private String currentUserId;
    private String currentUserName;
    private String orderNumber;
    private String jobNumber;
    private String referenceCode;
    private String prinCode;

    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private boolean isScrollingContinuously = false;
    private static final int SCROLL_DELAY_MS = 100;

    private int incomingCurrentLocationIndex = -1;
    private Location incomingCurrentLocationObject = null;

    private static final int KEYCODE_BACK_VOICE = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_SELECT_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_ScrollUP_VOICE = KeyEvent.KEYCODE_F3;
    private static final int KEYCODE_ScrollDOWN_VOICE = KeyEvent.KEYCODE_F4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);

        setupVoiceCommands();


        httpClient = UnsafeOkHttpClient.get();

        getUserDataAndReferences();
        initializeViews();

        locationList = new ArrayList<>();
        setupRecyclerView();
        setupArrowButtons();
        setupNavigationButtons();

        if (getIntent().hasExtra(ORDER_LOCATIONS_LIST_KEY)) {
            ArrayList<Location> existingLocations =
                    getIntent().getParcelableArrayListExtra(ORDER_LOCATIONS_LIST_KEY);

            if (existingLocations != null) {
                ArrayList<Location> unverifiedExistingLocations = new ArrayList<>();
                for (Location loc : existingLocations) {
                    if ("N".equalsIgnoreCase(loc.getPdAVerified())) {
                        unverifiedExistingLocations.add(loc);
                    }
                }

                if (!unverifiedExistingLocations.isEmpty()) {
                    Collections.sort(unverifiedExistingLocations, (l1, l2) -> {
                        int locCompare = l1.getLocationCode().compareTo(l2.getLocationCode());
                        if (locCompare == 0) return l1.getProduct().compareTo(l2.getProduct());
                        return locCompare;
                    });

                    locationList.addAll(unverifiedExistingLocations);
                    locationAdapter.notifyDataSetChanged();
                }
            }
        }

        incomingCurrentLocationIndex = getIntent().getIntExtra(CURRENT_LOCATION_INDEX_KEY, -1);
        incomingCurrentLocationObject = getIntent().getParcelableExtra(CURRENT_LOCATION_OBJECT_KEY);

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

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");

            speechClient.insertKeycodePhrase("Back", KEYCODE_BACK_VOICE);
            speechClient.insertKeycodePhrase("Select", KEYCODE_SELECT_VOICE);
            speechClient.insertKeycodePhrase("Scroll Up", KEYCODE_ScrollUP_VOICE);
            speechClient.insertKeycodePhrase("Scroll Down", KEYCODE_ScrollDOWN_VOICE);

            Log.d(TAG, "Voice commands registered successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Vuzix speech commands: " + e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_BACK_VOICE:
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;

            case KEYCODE_SELECT_VOICE:
                if (btnSelectLocation != null) btnSelectLocation.performClick();
                return true;

            case KEYCODE_ScrollUP_VOICE:
                navigateListByOneStep(-1);
                return true;

            case KEYCODE_ScrollDOWN_VOICE:
                navigateListByOneStep(1);
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void getUserDataAndReferences() {
        currentUserId = getIntent().getStringExtra("USER_ID");
        currentUserName = getIntent().getStringExtra("USER_NAME");
        orderNumber = getIntent().getStringExtra("ORDER_NUMBER");
        jobNumber = getIntent().getStringExtra("JOB_NUMBER");
        referenceCode = getIntent().getStringExtra("REFERENCE_CODE");
        prinCode = getIntent().getStringExtra("PRIN_CODE");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        currentUserId = currentUserId != null
                ? currentUserId
                : prefs.getString("CURRENT_USER_ID", prefs.getString("LOGGED_IN_USER_ID", ""));

        currentUserName = currentUserName != null
                ? currentUserName
                : prefs.getString("CURRENT_USER_NAME", prefs.getString("LOGGED_IN_USER_NAME", "User"));

        orderNumber = orderNumber != null ? orderNumber : prefs.getString("CURRENT_ORDER_NO", "");
        jobNumber = jobNumber != null ? jobNumber : prefs.getString("CURRENT_JOB_NO", "");
        referenceCode = referenceCode != null ? referenceCode : prefs.getString("CURRENT_REFERENCE", "");
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
            StringBuilder urlBuilder = new StringBuilder(ApiConfig.ORDER_DETAILS);

            urlBuilder.append("?as_prin_code=")
                    .append(URLEncoder.encode(prinCode != null ? prinCode : "029",
                            StandardCharsets.UTF_8.toString()));

            if (jobNumber != null && !jobNumber.trim().isEmpty()) {
                urlBuilder.append("&as_job_no=")
                        .append(URLEncoder.encode(jobNumber.trim(), StandardCharsets.UTF_8.toString()));
            }

            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                urlBuilder.append("&as_order_no=")
                        .append(URLEncoder.encode(orderNumber.trim(), StandardCharsets.UTF_8.toString()));
            }

            if (currentUserId != null && !currentUserId.trim().isEmpty()) {
                urlBuilder.append("&as_login_id=")
                        .append(URLEncoder.encode(currentUserId.trim(), StandardCharsets.UTF_8.toString()));
            }

            if (referenceCode != null && !referenceCode.trim().isEmpty()) {
                urlBuilder.append("&as_reference_code=")
                        .append(URLEncoder.encode(referenceCode.trim(), StandardCharsets.UTF_8.toString()));
                Log.d(TAG, "Sending reference code: " + referenceCode);
            }

            String finalUrl = urlBuilder.toString();
            Log.d(TAG, "Making request to: " + finalUrl);

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .get()
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_JSON)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Network request failed", e);
                    runOnUiThread(() -> {
                        showLoading(false);
                        handleApiError("Network connection failed. Please check internet.\n\nDetails: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseString = "";

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) responseString = responseBody.string();
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body", e);
                        String err = e.getMessage();
                        runOnUiThread(() -> {
                            showLoading(false);
                            handleApiError("Failed to read server response. Details: " + err);
                        });
                        return;
                    }

                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Response Headers: " + response.headers());
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
            case 401: baseMessage = "Authentication failed (401).\n\nCheck API key or login."; break;
            case 403: baseMessage = "Access forbidden (403).\n\nPermissions issue."; break;
            case 404: baseMessage = "Service not found (404).\n\nAPI endpoint might be wrong."; break;
            case 422: baseMessage = "Invalid request data (422).\n\nCheck parameters."; break;
            case 500: baseMessage = "Server error (500).\n\nPlease try again later."; break;
            case 502: baseMessage = "Bad gateway (502).\n\nServer temporarily unavailable."; break;
            case 503: baseMessage = "Service unavailable (503).\n\nServer maintenance?"; break;
            case 504: baseMessage = "Gateway timeout (504).\n\nServer took too long to respond."; break;
            default:  baseMessage = "Server error (" + responseCode + ").\n\nUnexpected response.";
        }

        if (responseBody != null && !responseBody.trim().isEmpty() && responseBody.length() < 200) {
            try {
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("message")) baseMessage += "\n\nServer message: " + errorJson.getString("message");
                else if (errorJson.has("error")) baseMessage += "\n\nServer error: " + errorJson.getString("error");
            } catch (JSONException ignore) {
                if (responseBody.length() < 100 && responseBody.matches(".*[a-zA-Z].*")) {
                    baseMessage += "\n\nRaw response: " + responseBody;
                }
            }
        }
        return baseMessage;
    }

    private void parseLocationData(String jsonResponse) {
        try {
            Log.d(TAG, "Parsing JSON response: " + jsonResponse);

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                runOnUiThread(() -> {
                    showLoading(false);
                    handleApiError("Empty response from server. Please try again.");
                });
                return;
            }

            JSONArray locationsArray = null;

            try {
                if (jsonResponse.trim().startsWith("[")) {
                    locationsArray = new JSONArray(jsonResponse);
                } else {
                    JSONObject responseObject = new JSONObject(jsonResponse);

                    if (responseObject.has("Details")) {
                        Object detailsObject = responseObject.get("Details");
                        if (detailsObject instanceof JSONArray) locationsArray = (JSONArray) detailsObject;
                        else if (detailsObject instanceof JSONObject) {
                            locationsArray = new JSONArray();
                            locationsArray.put(detailsObject);
                        }
                    } else if (responseObject.has("data")) {
                        Object dataObject = responseObject.get("data");
                        if (dataObject instanceof JSONArray) locationsArray = (JSONArray) dataObject;
                        else if (dataObject instanceof JSONObject) {
                            locationsArray = new JSONArray();
                            locationsArray.put(dataObject);
                        }
                    } else {
                        if (responseObject.has("location_code") || responseObject.has("prod_code") || responseObject.has("key_number")) {
                            locationsArray = new JSONArray();
                            locationsArray.put(responseObject);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error (format check): " + e.getMessage());
                try {
                    JSONObject singleObject = new JSONObject(jsonResponse);
                    locationsArray = new JSONArray();
                    locationsArray.put(singleObject);
                } catch (JSONException ex) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        handleApiError("Invalid JSON response format.\n\nRaw: " +
                                (jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse));
                    });
                    return;
                }
            }

            if (locationsArray == null || locationsArray.length() == 0) {
                runOnUiThread(() -> {
                    showLoading(false);
                    handleApiError("No location data found in response.");
                });
                return;
            }

            ArrayList<Location> fetchedLocations = new ArrayList<>();

            for (int i = 0; i < locationsArray.length(); i++) {
                try {
                    JSONObject locationObj = locationsArray.getJSONObject(i);
                    Location location = Location.fromJson(locationObj);

                    if ("N".equalsIgnoreCase(location.getPdAVerified())) {
                        fetchedLocations.add(location);
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Skipping bad location JSON at index " + i, e);
                }
            }

            runOnUiThread(() -> {
                showLoading(false);

                if (fetchedLocations.isEmpty()) {
                    handleApiError("No unverified locations found.");
                    return;
                }

                Collections.sort(fetchedLocations, (l1, l2) -> {
                    int locCompare = l1.getLocationCode().compareTo(l2.getLocationCode());
                    if (locCompare == 0) return l1.getProduct().compareTo(l2.getProduct());
                    return locCompare;
                });

                updateLocationList(fetchedLocations);

                int selectionIndex = -1;

                if (incomingCurrentLocationObject != null) {
                    for (int i = 0; i < locationList.size(); i++) {
                        if (locationList.get(i).getId().equals(incomingCurrentLocationObject.getId())
                                && "N".equalsIgnoreCase(locationList.get(i).getPdAVerified())) {
                            selectionIndex = i;
                            break;
                        }
                    }
                }

                if (selectionIndex == -1 && incomingCurrentLocationIndex != -1 && incomingCurrentLocationIndex < locationList.size()) {
                    if ("N".equalsIgnoreCase(locationList.get(incomingCurrentLocationIndex).getPdAVerified())) {
                        selectionIndex = incomingCurrentLocationIndex;
                    }
                }

                if (selectionIndex == -1 && !locationList.isEmpty()) {
                    selectionIndex = 0;
                }

                if (selectionIndex != -1) {
                    locationAdapter.setSelectedPosition(selectionIndex);
                    rvLocations.smoothScrollToPosition(selectionIndex);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Unexpected parse error", e);
            runOnUiThread(() -> {
                showLoading(false);
                handleApiError("Unexpected error while processing response:\n" + e.getMessage());
            });
        }
    }

    private void updateLocationList(ArrayList<Location> newLocations) {
        locationList.clear();
        locationList.addAll(newLocations);
        locationAdapter.notifyDataSetChanged();
        updateArrowVisibility();

        Toast.makeText(this, "Loaded " + newLocations.size() + " unverified locations", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        if (btnSelectLocation != null) {
            btnSelectLocation.setEnabled(!show);
            btnSelectLocation.setText(show ? "LOADING..." : "SELECT");
        }

        if (arrowUpLocation != null) arrowUpLocation.setEnabled(!show);
        if (arrowDownLocation != null) arrowDownLocation.setEnabled(!show);
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

        ContinuousScroller(int direction) {
            this.direction = direction;
        }

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

        if (newPosition >= locationList.size()) newPosition = locationList.size() - 1;
        if (newPosition < 0) newPosition = 0;

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
                resultIntent.putParcelableArrayListExtra(ORDER_LOCATIONS_LIST_KEY, locationList);
                resultIntent.putExtra(SELECTED_LOCATION_INDEX_KEY, locationAdapter.getSelectedPosition());
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "No location selected.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocationClick(int position, Location location) {
        locationAdapter.setSelectedPosition(position);
        rvLocations.smoothScrollToPosition(position);
        updateArrowVisibility();
    }

    private void updateArrowVisibility() {
        if (layoutManager == null || locationAdapter == null || locationList.isEmpty()) {
            arrowUpLocation.setVisibility(View.INVISIBLE);
            arrowDownLocation.setVisibility(View.INVISIBLE);
            return;
        }

        int first = layoutManager.findFirstCompletelyVisibleItemPosition();
        int last = layoutManager.findLastCompletelyVisibleItemPosition();
        int count = locationAdapter.getItemCount();

        arrowUpLocation.setVisibility(first > 0 ? View.VISIBLE : View.INVISIBLE);
        arrowDownLocation.setVisibility(last < count - 1 ? View.VISIBLE : View.INVISIBLE);
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
        if (locationAdapter != null) {
            locationAdapter.cleanup();
        }
    }
}
