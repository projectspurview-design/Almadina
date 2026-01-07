package com.example.Pickbyvision.Induvidual_Pick;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.Pickbyvision.Induvidual_Pick.network.UnsafeOkHttpClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.Pickbyvision.Induvidual_Pick.Location.Location;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class InduvidualJobActivity extends AppCompatActivity {

    private static final String TAG = "InduvidualJobActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int MENU_REQUEST_CODE = 101;
    private static final int PICK_LOCATION_REQUEST_CODE = 102;

    private java.util.HashMap<String, Integer> originalQuantitiesMap = new java.util.HashMap<>();


    private static final String PREF_LAST_PICKED_LOCATION_ID = "lastPickedLocationId";
    private static final String PREF_LAST_PICKED_LOCATION_CODE = "lastPickedLocationCode";
    private static final String PREF_LAST_PICKED_QUANTITY = "lastPickedQuantity";
    private static final String PREF_LAST_PICKED_PRODUCT = "lastPickedProduct";
    private static final String PREF_LAST_PICKED_DESCRIPTION = "lastPickedDescription";
    private static final String PREF_LAST_PICKED_EXPIRATION = "lastPickedExpiration";
    private static final String PREF_LAST_PICKED_MANUFACTURING = "lastPickedManufacturing";
    private static final String PREF_LAST_PICKED_PDA_VERIFIED = "lastPickedPdaVerified";


    private static final String PREF_LAST_PICKED_ORDER_NO = "lastPickedOrderNo";
    private static final String PREF_LAST_PICKED_JOB_NO = "lastPickedJobNo";


    private static final String PREF_CURRENT_ORDER_NO = "CURRENT_ORDER_NO";
    private static final String PREF_CURRENT_JOB_NO = "CURRENT_JOB_NO";


    private static final String JOB_STARTED_PREFIX = "job_started_";

    private OkHttpClient httpClient;
    private PreviewView previewView;
    private ProcessCameraProvider mCameraProvider;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private Handler uiHandler;
    private LinearLayout orderInfoContainer;
    private Button btnScanBarcode;
    private LinearLayout menuLayout;
    private TextView skuStatusText;
    private LinearLayout shortQuantityInputContainer;
    private TextView pausedStatusText;

    private TextView locationText;
    private TextView quantityText;
    private TextView productText;
    private TextView descriptionText;
    private TextView expirationText;
    private TextView manufacturingText;

    private NumberPicker npSingleDigitInput;
    private TextView tvEnteredQuantity;
    private Button btnAddDigit;
    private Button btnConfirmShortInput;
    private View mfgContainer;
    private View expContainer;
    private View dateDivider;

    private final StringBuilder enteredQuantityBuilder = new StringBuilder();

    private static final long SKU_MESSAGE_DISPLAY_DURATION_MS = 4000;

    private ArrayList<Location> orderLocations = new ArrayList<>();
    private int currentLocationIndex = -1;
    private Location currentOrderLocation;

    private String currentUserId;
    private String orderNumber;
    private String jobNumber;
    private String referenceCode;
    private String prinCode;


    private ProgressBar progressBar;
    private TextView progressText;
    private int originalTotalLocations = 0;

    private static final String JOB_INITIAL_TOTAL_PICKS_PREFIX = "job_initial_total_picks_";
    private static final String JOB_INITIAL_TOTAL_QUANTITY_PREFIX = "job_initial_total_qty_";

    private int initialApiTotalPicks = 0;
    private int initialApiTotalQuantity = 0;

    private long startTime = 0;
    private long elapsedTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private TextView timerTextView;
    private ImageView productImageView;


    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onNext() {
            if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
                View btn = findViewById(R.id.btnConfirmShortInput);
                if (btn != null) btn.performClick();
                return;
            }
            if (btnScanBarcode != null) btnScanBarcode.performClick();
        }

        @Override public void onBack() {
            if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
                View btn = findViewById(R.id.btnCancelShortInput);
                if (btn != null) btn.performClick();
            } else {
                onBackPressed();
            }
        }

        @Override public void onScrollUp() {
            if (npSingleDigitInput != null && npSingleDigitInput.getVisibility() == View.VISIBLE) {
                int v = npSingleDigitInput.getValue();
                if (v > npSingleDigitInput.getMinValue()) npSingleDigitInput.setValue(v - 1);
            }
        }

        @Override public void onScrollDown() {
            if (npSingleDigitInput != null && npSingleDigitInput.getVisibility() == View.VISIBLE) {
                int v = npSingleDigitInput.getValue();
                if (v < npSingleDigitInput.getMaxValue()) npSingleDigitInput.setValue(v + 1);
            }
        }

        @Override public void onSelect() {
            if (btnScanBarcode != null) btnScanBarcode.performClick();
        }

        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}

        @Override public void onLogout() { LogoutManager.performLogout(InduvidualJobActivity.this); }

        @Override public void onScan()        { if (btnScanBarcode != null) btnScanBarcode.performClick(); }
        @Override public void onShort()       { showShortQuantityInput(); }
        @Override public void onJumpTo()      { openPickLocationActivity(); }
        @Override public void onShortRemove() { handleBackspaceDigit(); }
        @Override public void onShortAdd()    { handleAddDigit(); }
        @Override public void onShortBack()   { handleCancelShortQuantity(); }
        @Override public void onShortNext()   { handleConfirmShortQuantity(); }
        @Override public void onDigit(int d)  {
            if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
                handleVoiceDigitInput(d);
            }
        }
    };



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.induvidualjobactivity);

        timerTextView = findViewById(R.id.timerTextView);
        startTimer();

        VoiceCommandCenter.init(this);



        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        initialApiTotalPicks = getIntent().getIntExtra("INITIAL_API_TOTAL_PICKS",
                prefs.getInt("job_initial_total_picks_" + jobNumber, 0));

        initialApiTotalQuantity = getIntent().getIntExtra("INITIAL_API_TOTAL_QUANTITY",
                prefs.getInt("job_initial_total_qty_" + jobNumber, 0));

        Log.d(TAG, "Resolved totals -> Picks=" + initialApiTotalPicks + ", Qty=" + initialApiTotalQuantity);


        httpClient = UnsafeOkHttpClient.get();

        previewView = findViewById(R.id.camera_preview);
        orderInfoContainer = findViewById(R.id.orderInfoContainer);
        btnScanBarcode = findViewById(R.id.btnScanBarcode);
        menuLayout = findViewById(R.id.menuLayout);
        skuStatusText = findViewById(R.id.skuStatusText);
        shortQuantityInputContainer = findViewById(R.id.shortQuantityInputContainer);
        pausedStatusText = findViewById(R.id.pausedStatusText);

        locationText = findViewById(R.id.locationText);
        quantityText = findViewById(R.id.quantityText);
        productText = findViewById(R.id.productText);
        descriptionText = findViewById(R.id.descriptionText);
        expirationText = findViewById(R.id.expirationText);
        manufacturingText = findViewById(R.id.manufacturingText);
        productImageView = findViewById(R.id.productImageView);

        Button logoutButton = findViewById(R.id.logoutButton);
        mfgContainer  = findViewById(R.id.mfgContainer);
        expContainer  = findViewById(R.id.expContainer);
        dateDivider   = findViewById(R.id.dateDivider);

        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(InduvidualJobActivity.this);
        });

        npSingleDigitInput = findViewById(R.id.npSingleDigitInput);
        tvEnteredQuantity = findViewById(R.id.tvEnteredQuantity);
        Button btnBackspaceDigit = findViewById(R.id.btnBackspaceDigit);
        btnAddDigit = findViewById(R.id.btnAddDigit);
        btnConfirmShortInput = findViewById(R.id.btnConfirmShortInput);
        Button btnCancelShortInput = findViewById(R.id.btnCancelShortInput);

        cameraExecutor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        findViewById(R.id.btnJumpTo).setOnClickListener(v -> openPickLocationActivity());
        findViewById(R.id.btnShort).setOnClickListener(v -> showShortQuantityInput());
        btnScanBarcode.setOnClickListener(v -> toggleScanState());

        setupNumberPickers();

        if (btnAddDigit != null) btnAddDigit.setOnClickListener(v -> handleAddDigit());
        if (btnBackspaceDigit != null) btnBackspaceDigit.setOnClickListener(v -> handleBackspaceDigit());
        btnConfirmShortInput.setOnClickListener(v -> handleConfirmShortQuantity());
        btnCancelShortInput.setOnClickListener(v -> handleCancelShortQuantity());

        getUserDataAndReferences();

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        currentOrderLocation = loadLastPickedLocation(orderNumber, jobNumber);

        if (currentOrderLocation != null) {
            Log.d(TAG, "Loaded last picked location from SharedPreferences for current order: " + currentOrderLocation.getLocationCode());
            updateUIWithLocationDetails(currentOrderLocation);
            fetchOrderLocations(false);
        } else {
            updateUIWithLocationDetails(null);
            Log.d(TAG, "No valid last picked location found for current order, fetching order details...");
            fetchOrderLocations(true);
        }

        if (currentOrderLocation != null) {
            fetchOrderLocations(false);
            startJobPicking();
        } else {
            updateUIWithLocationDetails(null);
            Log.d(TAG, "No valid last picked location found for current order, fetching order details...");
            fetchOrderLocations(true);
            startJobPicking();
        }
    }

    private static boolean isBlank(String s) {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty()
                || t.equalsIgnoreCase("null")
                || t.equalsIgnoreCase("n/a")
                || t.equalsIgnoreCase("na")
                || t.equals("--")
                || t.equals("0000-00-00");
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override public void run() {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimerUI(elapsedTime);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimerUI(long elapsed) {
        long totalSeconds = elapsed / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (timerTextView != null) {
            timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void handleVoiceDigitInput(int digit) {
        if (digit < 0 || digit > 9) return;

        if (enteredQuantityBuilder.length() == 0 && digit == 0) {
            tvEnteredQuantity.setText("0");
            return;
        }

        final int maxDigitsAllowed = 6;
        if (enteredQuantityBuilder.length() >= maxDigitsAllowed) {
            Toast.makeText(this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
            return;
        }

        enteredQuantityBuilder.append(digit);
        tvEnteredQuantity.setText(enteredQuantityBuilder.toString());
        Log.d(TAG, "Voice digit entered: " + digit + " | Current quantity: " + enteredQuantityBuilder);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed. Sending RESULT_CANCELED to OrderSummaryActivity.");
        pauseJobPicking();
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    private void getUserDataAndReferences() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        String intentOrderNumber = getIntent().getStringExtra("ORDER_NUMBER");
        String intentJobNumber = getIntent().getStringExtra("JOB_NUMBER");
        String intentUserId = getIntent().getStringExtra("USER_ID");
        String intentPrinCode = getIntent().getStringExtra("PRIN_CODE");
        String intentReferenceCode = getIntent().getStringExtra("REFERENCE_CODE");

        String prevSavedOrderNo = prefs.getString(PREF_CURRENT_ORDER_NO, "");
        String prevSavedJobNo = prefs.getString(PREF_CURRENT_JOB_NO, "");

        orderNumber = intentOrderNumber != null ? intentOrderNumber : prefs.getString(PREF_CURRENT_ORDER_NO, "");
        jobNumber = intentJobNumber != null ? intentJobNumber : prefs.getString(PREF_CURRENT_JOB_NO, "");

        currentUserId = intentUserId != null ? intentUserId : prefs.getString("CURRENT_USER_ID", prefs.getString("LOGGED_IN_USER_ID", ""));
        String currentUserName = getIntent().getStringExtra("USER_NAME");
        currentUserName = currentUserName != null ? currentUserName : prefs.getString("CURRENT_USER_NAME", prefs.getString("LOGGED_IN_USER_NAME", "User"));
        referenceCode = intentReferenceCode != null ? intentReferenceCode : prefs.getString("CURRENT_REFERENCE", "");
        prinCode = intentPrinCode != null ? intentPrinCode : prefs.getString("PRIN_CODE", "029");

        boolean isNewOrder = !orderNumber.equals(prevSavedOrderNo) || !jobNumber.equals(prevSavedJobNo);

        if (isNewOrder && (!orderNumber.isEmpty() || !jobNumber.isEmpty())) {
            Log.d(TAG, "Detected new order/job. Clearing last picked location from SharedPreferences. Old: " + prevSavedOrderNo + "/" + prevSavedJobNo + ", New: " + orderNumber + "/" + jobNumber);
            saveLastPickedLocation(null);
            if (!jobNumber.isEmpty()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(JOB_STARTED_PREFIX + jobNumber, false);
                editor.remove(JOB_INITIAL_TOTAL_PICKS_PREFIX + jobNumber);
                editor.remove(JOB_INITIAL_TOTAL_QUANTITY_PREFIX + jobNumber);
                editor.apply();
                Log.d(TAG, "Cleared 'job started' flag and initial totals for new job: " + jobNumber);
            }
        } else if (orderNumber.isEmpty() && jobNumber.isEmpty()) {
            Log.d(TAG, "No order/job detected. Clearing last picked location from SharedPreferences.");
            saveLastPickedLocation(null);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("CURRENT_USER_ID", currentUserId);
        editor.putString("CURRENT_USER_NAME", currentUserName);
        editor.putString(PREF_CURRENT_ORDER_NO, orderNumber);
        editor.putString(PREF_CURRENT_JOB_NO, jobNumber);
        editor.putString("CURRENT_REFERENCE", referenceCode);
        editor.putString("PRIN_CODE", prinCode);
        editor.apply();

        Log.d(TAG, "User Data - ID: " + currentUserId + ", Name: " + currentUserName);
        Log.d(TAG, "Order: " + orderNumber + ", Job: " + jobNumber);
        Log.d(TAG, "Reference Code: " + referenceCode + ", Prin Code: " + prinCode);
    }

    private Location loadLastPickedLocation(String currentOrderNo, String currentJobNo) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lastLocationId = prefs.getString(PREF_LAST_PICKED_LOCATION_ID, null);
        String lastLocationCode = prefs.getString(PREF_LAST_PICKED_LOCATION_CODE, null);
        String lastProduct = prefs.getString(PREF_LAST_PICKED_PRODUCT, null);
        String lastDescription = prefs.getString(PREF_LAST_PICKED_DESCRIPTION, null);
        String lastExpiration = prefs.getString(PREF_LAST_PICKED_EXPIRATION, null);
        String lastManufacturing = prefs.getString(PREF_LAST_PICKED_MANUFACTURING, null);
        String lastPdaVerified = prefs.getString(PREF_LAST_PICKED_PDA_VERIFIED, "N");
        int lastQuantity = prefs.getInt(PREF_LAST_PICKED_QUANTITY, 0);

        String savedLastPickedOrderNo = prefs.getString(PREF_LAST_PICKED_ORDER_NO, "");
        String savedLastPickedJobNo = prefs.getString(PREF_LAST_PICKED_JOB_NO, "");

        if (lastLocationId != null && !lastLocationId.isEmpty() &&
                lastLocationCode != null && !lastLocationCode.isEmpty() &&
                currentOrderNo.equals(savedLastPickedOrderNo) && currentJobNo.equals(savedLastPickedJobNo) &&
                lastPdaVerified.equalsIgnoreCase("N")) {
            Log.d(TAG, "Attempting to load last picked location: ID=" + lastLocationId + ", Code=" + lastLocationCode + ", Verified=" + lastPdaVerified + " for current order: " + currentOrderNo + "/" + currentJobNo);
            return new Location(lastLocationId, lastLocationCode, lastProduct, lastDescription,
                    lastExpiration, lastManufacturing, lastQuantity, lastPdaVerified);
        }
        Log.d(TAG, "No valid (unverified and matching current order/job) last picked location found in SharedPreferences.");
        return null;
    }

    private void saveLastPickedLocation(Location location) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (location != null) {
            editor.putString(PREF_LAST_PICKED_LOCATION_ID, location.getId());
            editor.putString(PREF_LAST_PICKED_LOCATION_CODE, location.getLocationCode());
            editor.putInt(PREF_LAST_PICKED_QUANTITY, location.getQuantity());
            editor.putString(PREF_LAST_PICKED_PRODUCT, location.getProduct());
            editor.putString(PREF_LAST_PICKED_DESCRIPTION, location.getDescription());
            editor.putString(PREF_LAST_PICKED_EXPIRATION, location.getExpiration());
            editor.putString(PREF_LAST_PICKED_MANUFACTURING, location.getManufacturing());
            editor.putString(PREF_LAST_PICKED_PDA_VERIFIED, location.getPdAVerified());
            editor.putString(PREF_LAST_PICKED_ORDER_NO, orderNumber);
            editor.putString(PREF_LAST_PICKED_JOB_NO, jobNumber);
            Log.d(TAG, "Saved last picked location to SharedPreferences: " + location.getLocationCode() + " (Verified: " + location.getPdAVerified() + ") for order/job: " + orderNumber + "/" + jobNumber);
        } else {
            editor.remove(PREF_LAST_PICKED_LOCATION_ID);
            editor.remove(PREF_LAST_PICKED_LOCATION_CODE);
            editor.remove(PREF_LAST_PICKED_QUANTITY);
            editor.remove(PREF_LAST_PICKED_PRODUCT);
            editor.remove(PREF_LAST_PICKED_DESCRIPTION);
            editor.remove(PREF_LAST_PICKED_EXPIRATION);
            editor.remove(PREF_LAST_PICKED_MANUFACTURING);
            editor.remove(PREF_LAST_PICKED_PDA_VERIFIED);
            editor.remove(PREF_LAST_PICKED_ORDER_NO);
            editor.remove(PREF_LAST_PICKED_JOB_NO);
            Log.d(TAG, "Cleared last picked location from SharedPreferences.");
        }
        editor.apply();
    }


    private void fetchOrderLocations(boolean forceSelectionOfFirstUnverified) {
        if (orderNumber == null || orderNumber.isEmpty() || jobNumber == null || jobNumber.isEmpty()) {
            Log.d(TAG, "Order or Job number is missing. Cannot fetch locations.");
            Toast.makeText(this, "Order or Job number is missing. Please select an order.", Toast.LENGTH_LONG).show();
            updateUIWithLocationDetails(null);
            updateProgressDisplay();
            showInitialScreenState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        progressText.setText("Loading order details...");
        orderInfoContainer.setVisibility(View.GONE);

        try {
            StringBuilder urlBuilder = new StringBuilder(ApiConfig.ORDER_DETAILS);
            urlBuilder.append("?as_prin_code=").append(URLEncoder.encode(prinCode != null ? prinCode : "029", StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&as_job_no=").append(URLEncoder.encode(jobNumber.trim(), StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&as_order_no=").append(URLEncoder.encode(orderNumber.trim(), StandardCharsets.UTF_8.toString()));
            if (currentUserId != null && !currentUserId.trim().isEmpty()) {
                urlBuilder.append("&as_login_id=").append(URLEncoder.encode(currentUserId.trim(), StandardCharsets.UTF_8.toString()));
            }
            if (referenceCode != null && !referenceCode.trim().isEmpty()) {
                urlBuilder.append("&as_reference_code=").append(URLEncoder.encode(referenceCode.trim(), StandardCharsets.UTF_8.toString()));
                Log.d(TAG, "Sending reference code: " + referenceCode);
            }

            String finalUrl = urlBuilder.toString();
            Log.d(TAG, "Making request to fetch order details: " + finalUrl);

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
                    Log.e(TAG, "Network request failed for order details", e);
                    final String errorMessage = e.getMessage();
                    runOnUiThread(() -> {
                        orderInfoContainer.setVisibility(View.VISIBLE);
                        Toast.makeText(InduvidualJobActivity.this, "Network error fetching order details: " + errorMessage, Toast.LENGTH_LONG).show();
                        updateUIWithLocationDetails(null);
                        orderLocations.clear();
                        originalTotalLocations = 0;
                        updateProgressDisplay();
                        showInitialScreenState();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseString = "";
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) responseString = responseBody.string();
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body or closing response for order details", e);
                        final String errorMessage = e.getMessage();
                        runOnUiThread(() -> {
                            orderInfoContainer.setVisibility(View.VISIBLE);
                            Toast.makeText(InduvidualJobActivity.this, "Failed to read server response for order details: " + errorMessage, Toast.LENGTH_LONG).show();
                            updateUIWithLocationDetails(null);
                            updateProgressDisplay();
                            showInitialScreenState();
                        });
                        return;
                    }

                    final String finalResponseString = responseString;
                    final int finalResponseCode = response.code();
                    final boolean finalForceSelection = forceSelectionOfFirstUnverified;

                    Log.d(TAG, "Order Details API Response Code: " + finalResponseCode);
                    Log.d(TAG, "Order Details API Response Body: " + finalResponseString);

                    runOnUiThread(() -> {
                        orderInfoContainer.setVisibility(View.VISIBLE);
                        if (response.isSuccessful()) {
                            try {
                                JSONArray locationsArray = null;

                                if (finalResponseString.trim().startsWith("[")) {
                                    locationsArray = new JSONArray(finalResponseString);
                                } else {
                                    JSONObject responseObject = new JSONObject(finalResponseString);
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

                                if (locationsArray == null || locationsArray.length() == 0) {
                                    Toast.makeText(InduvidualJobActivity.this, "No location data found for this order.", Toast.LENGTH_LONG).show();
                                    orderLocations.clear();
                                    originalTotalLocations = 0;
                                    currentOrderLocation = null;
                                    currentLocationIndex = -1;
                                    updateUIWithLocationDetails(null);
                                    saveLastPickedLocation(null);
                                    updateProgressDisplay();
                                    showInitialScreenState();
                                    return;
                                }

                                ArrayList<Location> newFetchedLocations = new ArrayList<>();
                                for (int i = 0; i < locationsArray.length(); i++) {
                                    try {
                                        JSONObject locationObj = locationsArray.getJSONObject(i);
                                        newFetchedLocations.add(Location.fromJson(locationObj));
                                    } catch (JSONException e) {
                                        Log.w(TAG, "Error parsing location object at index " + i + ", skipping.", e);
                                    }
                                }

                                orderLocations.clear();
                                orderLocations.addAll(newFetchedLocations);

                                originalQuantitiesMap.clear();
                                for (Location loc : orderLocations) {
                                    originalQuantitiesMap.put(loc.getId(), loc.getQuantity());
                                }

                                Collections.sort(orderLocations, new Comparator<Location>() {
                                    @Override
                                    public int compare(Location l1, Location l2) {
                                        int locCompare = l1.getLocationCode().compareTo(l2.getLocationCode());
                                        if (locCompare == 0) return l1.getProduct().compareTo(l2.getProduct());
                                        return locCompare;
                                    }
                                });

                                originalTotalLocations = orderLocations.size();
                                Log.d(TAG, "Fetched " + originalTotalLocations + " locations. Original total set.");

                                Location targetLocation = null;
                                int targetIndex = -1;

                                if (finalForceSelection) {
                                    for (int i = 0; i < orderLocations.size(); i++) {
                                        if (orderLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) {
                                            targetLocation = orderLocations.get(i);
                                            targetIndex = i;
                                            Log.d(TAG, "Set current location to first unverified (forced): " + targetLocation.getLocationCode());
                                            break;
                                        }
                                    }
                                } else {
                                    Location loadedLastPicked = loadLastPickedLocation(orderNumber, jobNumber);
                                    if (loadedLastPicked != null) {
                                        for (int i = 0; i < orderLocations.size(); i++) {
                                            if (orderLocations.get(i).getId().equals(loadedLastPicked.getId()) &&
                                                    orderLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) {
                                                targetLocation = orderLocations.get(i);
                                                targetLocation.setQuantity(loadedLastPicked.getQuantity());
                                                targetIndex = i;
                                                Log.d(TAG, "Found and set last picked location: " + targetLocation.getLocationCode() + " qty " + targetLocation.getQuantity());
                                                break;
                                            }
                                        }
                                    }

                                    if (targetLocation == null) {
                                        for (int i = 0; i < orderLocations.size(); i++) {
                                            if (orderLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) {
                                                targetLocation = orderLocations.get(i);
                                                targetIndex = i;
                                                Log.d(TAG, "Fallback to first unverified location: " + targetLocation.getLocationCode());
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (targetLocation != null) {
                                    currentLocationIndex = targetIndex;
                                    currentOrderLocation = targetLocation;
                                    updateUIWithLocationDetails(currentOrderLocation);
                                    saveLastPickedLocation(currentOrderLocation);
                                    Toast.makeText(InduvidualJobActivity.this, "Order loaded. Showing location: " + currentOrderLocation.getLocationCode(), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(InduvidualJobActivity.this, "All locations for this order are already verified.", Toast.LENGTH_LONG).show();
                                    currentOrderLocation = null;
                                    currentLocationIndex = -1;
                                    updateUIWithLocationDetails(null);
                                    saveLastPickedLocation(null);
                                }

                                updateProgressDisplay();
                                showInitialScreenState();

                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error for order details response: " + e.getMessage(), e);
                                final String jsonErrorMessage = e.getMessage();
                                Toast.makeText(InduvidualJobActivity.this, "Error parsing order details: " + jsonErrorMessage, Toast.LENGTH_LONG).show();
                                updateUIWithLocationDetails(null);
                                orderLocations.clear();
                                originalTotalLocations = 0;
                                updateProgressDisplay();
                                showInitialScreenState();
                            }

                        } else {
                            Toast.makeText(InduvidualJobActivity.this, "Server error fetching order details: " + finalResponseCode + " " + finalResponseString, Toast.LENGTH_SHORT).show();
                            updateUIWithLocationDetails(null);
                            orderLocations.clear();
                            originalTotalLocations = 0;
                            updateProgressDisplay();
                            showInitialScreenState();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating request to fetch order details", e);
            final String requestErrorMessage = e.getMessage();
            Toast.makeText(this, "Error preparing order details request: " + requestErrorMessage, Toast.LENGTH_LONG).show();
            updateUIWithLocationDetails(null);
            orderLocations.clear();
            originalTotalLocations = 0;
            updateProgressDisplay();
            showInitialScreenState();
        }
    }

    private void openPickLocationActivity() {
        Log.d(TAG, "Attempting to open PickLocationActivity");
        Intent intent = new Intent(this, PickLocationActivity.class);
        intent.putExtra("ORDER_NUMBER", orderNumber);
        intent.putExtra("JOB_NUMBER", jobNumber);
        intent.putExtra("USER_ID", currentUserId);
        intent.putExtra("REFERENCE_CODE", referenceCode);
        if (orderLocations != null && !orderLocations.isEmpty()) {
            intent.putParcelableArrayListExtra(PickLocationActivity.ORDER_LOCATIONS_LIST_KEY, orderLocations);
        }
        intent.putExtra(PickLocationActivity.CURRENT_LOCATION_OBJECT_KEY, currentOrderLocation);
        startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
    }

    private void toggleScanState() {
        if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please confirm or cancel short quantity first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrderLocation == null) {
            Toast.makeText(this, "Please select a location first by loading an order or using 'Jump To'.", Toast.LENGTH_LONG).show();
            return;
        }

        if (btnScanBarcode.getText().toString().equalsIgnoreCase(getString(R.string.resume_text))) {
            updateUIForPauseState(false);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCameraAndScanner();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void startCameraAndScanner() {
        isScanning.set(true);
        showScanningScreenState();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(mCameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error starting camera: " + errorMessage, Toast.LENGTH_LONG).show();
                    showInitialScreenState();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCameraAndScanner(boolean isError) {
        if (mCameraProvider != null) {
            mCameraProvider.unbindAll();
            Log.d(TAG, "Camera unbound.");
        }
        isScanning.set(false);
        if (!isError) showInitialScreenState();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        if (previewView == null) {
            Log.e(TAG, "PreviewView is null in bindPreviewAndAnalysis. Cannot proceed.");
            stopCameraAndScanner(true);
            return;
        }

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (!isScanning.get()) {
                image.close();
                return;
            }

            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                barcodeScanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            if (!isScanning.get() || barcodes.isEmpty()) {
                                return;
                            }

                            if (isScanning.compareAndSet(true, false)) {
                                if (mCameraProvider != null) mCameraProvider.unbindAll();
                                String scannedBarcodeValue = barcodes.get(0).getRawValue();
                                Log.d(TAG, "Barcode detected: " + scannedBarcodeValue);
                                verifyBarcodeWithServer(scannedBarcodeValue, currentOrderLocation.getQuantity());
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isScanning.get()) Log.e(TAG, "Barcode scanning failed", e);
                        })
                        .addOnCompleteListener(task -> image.close());
            } else {
                image.close();
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "Camera use cases bound.");
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            stopCameraAndScanner(true);
        }
    }

    private void verifyBarcodeWithServer(String scannedBarcodeValue, int quantityPicked) {
        if (currentOrderLocation == null) {
            Toast.makeText(this, "Please select a location first.", Toast.LENGTH_LONG).show();
            showInitialScreenState();
            isScanning.set(false);
            return;
        }

        String expectedLocationCode = currentOrderLocation.getLocationCode();

        if (!scannedBarcodeValue.trim().equalsIgnoreCase(expectedLocationCode != null ? expectedLocationCode.trim() : "")) {
            Log.d(TAG, "Scanned barcode does not match expected location code.");
            runOnUiThread(() -> {
                Toast.makeText(this, "WRONG SKU Scanned! (Expected Location Barcode)", Toast.LENGTH_SHORT).show();
                showScanResultScreenState(false);
            });
            return;
        }

        sendPickingDetailsToApi(currentOrderLocation.getId(), expectedLocationCode, quantityPicked, true);
    }

    private void updateUIWithLocationDetails(@Nullable Location location) {
        if (location == null) {
            locationText.setText("");
            quantityText.setText("");
            productText.setText("");
            descriptionText.setText("");
            expirationText.setText("");
            manufacturingText.setText("");

            if (productImageView != null) productImageView.setImageResource(R.drawable.ic_placeholder_image);

            if (mfgContainer != null) mfgContainer.setVisibility(View.GONE);
            if (expContainer != null) expContainer.setVisibility(View.GONE);
            if (dateDivider  != null) dateDivider.setVisibility(View.GONE);

            Log.d(TAG, "UI cleared: No current location set.");
            return;
        }

        locationText.setText(location.getLocationCode() != null ? location.getLocationCode() : "");
        quantityText.setText(String.valueOf(location.getQuantity()));
        productText.setText(location.getProduct() != null ? location.getProduct() : "");
        descriptionText.setText(location.getDescription() != null ? location.getDescription() : "");

        String mfg = formatDateOnly(location.getManufacturing());
        String exp = formatDateOnly(location.getExpiration());

        boolean hasMfg = !isBlank(mfg);
        boolean hasExp = !isBlank(exp);

        if (hasMfg) manufacturingText.setText(mfg);
        if (hasExp)  expirationText.setText(exp);

        if (mfgContainer != null) mfgContainer.setVisibility(hasMfg ? View.VISIBLE : View.GONE);
        if (expContainer != null) expContainer.setVisibility(hasExp ? View.VISIBLE : View.GONE);
        if (dateDivider != null) dateDivider.setVisibility((hasMfg || hasExp) ? View.VISIBLE : View.GONE);

        String imageUrl = null;
        try {
            imageUrl = (String) location.getClass().getMethod("getAwsPath").invoke(location);
        } catch (Exception ignore) {
            try { imageUrl = (String) location.getClass().getMethod("getImageUrl").invoke(location); } catch (Exception ignored) {}
            try { if (imageUrl == null) imageUrl = (String) location.getClass().getMethod("getImagePath").invoke(location); } catch (Exception ignored2) {}
        }
        loadProductImage(imageUrl);
    }

    private String formatDateOnly(String dateString) {
        if (isBlank(dateString)) return "";

        try {
            String[] fmts = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "MM/dd/yyyy HH:mm:ss",
                    "dd/MM/yyyy HH:mm:ss",
                    "yyyy-MM-dd",
                    "MM/dd/yyyy",
                    "dd/MM/yyyy"
            };
            SimpleDateFormat out = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            for (String f : fmts) {
                try {
                    Date d = new SimpleDateFormat(f, Locale.getDefault()).parse(dateString);
                    if (d != null) return out.format(d);
                } catch (ParseException ignored) {}
            }
            if (dateString.matches("\\d{2}-\\d{2}-\\d{4}")) return dateString;
            return "";
        } catch (Exception e) {
            Log.w(TAG, "formatDateOnly failed for: " + dateString, e);
            return "";
        }
    }

    private void loadProductImage(@Nullable String url) {
        if (productImageView == null) return;

        if (url == null || url.trim().isEmpty()) {
            runOnUiThread(() -> productImageView.setImageResource(R.drawable.ic_placeholder_image));
            return;
        }

        Object currentTag = productImageView.getTag();
        if (currentTag != null && url.equals(currentTag.toString())) return;
        productImageView.setTag(url);

        Request req = new Request.Builder().url(url).get().build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Product image load failed: " + e.getMessage());
                runOnUiThread(() -> productImageView.setImageResource(R.drawable.ic_placeholder_image));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                if (!resp.isSuccessful() || resp.body() == null) {
                    runOnUiThread(() -> productImageView.setImageResource(R.drawable.ic_placeholder_image));
                    return;
                }
                byte[] bytes = resp.body().bytes();
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                runOnUiThread(() -> {
                    if (bmp != null) {
                        productImageView.setImageBitmap(bmp);
                        productImageView.setVisibility(View.VISIBLE);
                    } else {
                        productImageView.setImageResource(R.drawable.ic_placeholder_image);
                    }
                });
            }
        });
    }

    private String getJobNoFromSession() {
        String jobNo = this.jobNumber;
        if (jobNo == null || jobNo.trim().isEmpty()) Log.e(TAG, "Job number missing in TenthActivity instance.");
        return jobNo;
    }

    private String getOrderNoFromSession() {
        String orderNo = this.orderNumber;
        if (orderNo == null || orderNo.trim().isEmpty()) Log.e(TAG, "Order number missing in TenthActivity instance.");
        return orderNo;
    }

    private String getSiteCodeFromSession() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getString("CURRENT_SITE_CODE", "A1");
    }

    private String getLoginIdFromSession() {
        String loginId = this.currentUserId;
        if (loginId == null || loginId.trim().isEmpty()) Log.e(TAG, "Login ID missing in TenthActivity instance.");
        return loginId;
    }

    private void showInitialScreenState() {
        if (previewView != null) previewView.setVisibility(View.GONE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.VISIBLE);
        if (btnScanBarcode != null) btnScanBarcode.setText(getString(R.string.scan_barcode_text));
        if (menuLayout != null) menuLayout.setVisibility(View.VISIBLE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);
        if (btnScanBarcode != null && btnScanBarcode.getVisibility() == View.VISIBLE) btnScanBarcode.requestFocus();
        updateProgressDisplay();
    }

    private void showScanningScreenState() {
        if (previewView != null) previewView.setVisibility(View.VISIBLE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void showScanResultScreenState(boolean isRightSku) {
        if (previewView != null) previewView.setVisibility(View.GONE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);

        if (skuStatusText != null) {
            skuStatusText.setVisibility(View.VISIBLE);
            if (isRightSku) {
                skuStatusText.setText("RIGHT SKU");
                skuStatusText.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                skuStatusText.setText("WRONG SKU");
                skuStatusText.setTextColor(Color.parseColor("#FF6B6B"));
            }
        }

        uiHandler.postDelayed(() -> {
            if (isRightSku) {
                if (currentOrderLocation != null) {
                    currentOrderLocation.setPdAVerified("Y");
                    saveLastPickedLocation(currentOrderLocation);
                }
                updateProgressDisplay();
                findNextUnverifiedLocationAndProceed(true);
            } else {
                showInitialScreenState();
            }
        }, SKU_MESSAGE_DISPLAY_DURATION_MS);
    }

    private void showShortQuantityInput() {
        if (isScanning.compareAndSet(true, false)) {
            if (mCameraProvider != null) mCameraProvider.unbindAll();
        }

        if (previewView != null) previewView.setVisibility(View.GONE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);

        if (shortQuantityInputContainer != null) {
            shortQuantityInputContainer.setVisibility(View.VISIBLE);
            enteredQuantityBuilder.setLength(0);
            tvEnteredQuantity.setText("0");
            npSingleDigitInput.setValue(0);
            if (npSingleDigitInput != null) npSingleDigitInput.requestFocus();
        }
    }

    private void hideShortQuantityInputAndShowMenu() {
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        showInitialScreenState();
    }

    private void setupNumberPickers() {
        if (npSingleDigitInput != null) {
            npSingleDigitInput.setMinValue(0);
            npSingleDigitInput.setMaxValue(9);
            npSingleDigitInput.setWrapSelectorWheel(false);
        }
    }

    private void handleAddDigit() {
        int currentDigit = npSingleDigitInput.getValue();
        if (enteredQuantityBuilder.length() == 0 && currentDigit == 0) {
            tvEnteredQuantity.setText("0");
            return;
        }

        final int maxDigitsAllowed = 6;
        if (enteredQuantityBuilder.length() >= maxDigitsAllowed) {
            Toast.makeText(this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
            return;
        }

        enteredQuantityBuilder.append(currentDigit);
        tvEnteredQuantity.setText(enteredQuantityBuilder.toString());

        npSingleDigitInput.setValue(0);
        npSingleDigitInput.requestFocus();
    }

    private void handleBackspaceDigit() {
        if (enteredQuantityBuilder.length() > 0) {
            enteredQuantityBuilder.setLength(enteredQuantityBuilder.length() - 1);
            tvEnteredQuantity.setText(enteredQuantityBuilder.length() == 0 ? "0" : enteredQuantityBuilder.toString());
        } else {
            tvEnteredQuantity.setText("0");
            Toast.makeText(this, "No digits to delete.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleConfirmShortQuantity() {
        if (currentOrderLocation == null) {
            Toast.makeText(this, "No active location. Please select an order.", Toast.LENGTH_LONG).show();
            handleCancelShortQuantity();
            return;
        }

        int enteredShortQuantity;

        if (enteredQuantityBuilder.length() == 0) {
            enteredShortQuantity = 0;
        } else {
            try {
                enteredShortQuantity = Integer.parseInt(enteredQuantityBuilder.toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int originalMaxQuantity = currentOrderLocation.getQuantity();
        if (originalQuantitiesMap.containsKey(currentOrderLocation.getId())) {
            originalMaxQuantity = originalQuantitiesMap.get(currentOrderLocation.getId());
        }

        if (enteredShortQuantity < 0) {
            Toast.makeText(this, "Quantity cannot be negative.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredShortQuantity > originalMaxQuantity) {
            Toast.makeText(this, "Short quantity (" + enteredShortQuantity + ") cannot be greater than original required (" + originalMaxQuantity + ").", Toast.LENGTH_LONG).show();
            enteredQuantityBuilder.setLength(0);
            tvEnteredQuantity.setText("0");
            return;
        }

        int finalPickedQuantity = originalMaxQuantity - enteredShortQuantity;

        currentOrderLocation.setQuantity(finalPickedQuantity);
        updateUIWithLocationDetails(currentOrderLocation);
        saveLastPickedLocation(currentOrderLocation);

        enteredQuantityBuilder.setLength(0);
        tvEnteredQuantity.setText("0");

        Toast.makeText(this, String.format(Locale.getDefault(), "Short: %d. New Pick Quantity: %d. Scan to confirm.", enteredShortQuantity, finalPickedQuantity), Toast.LENGTH_LONG).show();

        hideShortQuantityInputAndShowMenu();
    }

    private void handleCancelShortQuantity() {
        Toast.makeText(this, "Short quantity input cancelled.", Toast.LENGTH_SHORT).show();
        enteredQuantityBuilder.setLength(0);
        tvEnteredQuantity.setText("0");
        hideShortQuantityInputAndShowMenu();
    }


    private void sendPickingDetailsToApi(String keyNumber, String locationCode, int quantityPicked, boolean isBarcodeScan) {
        String loginId = getLoginIdFromSession();
        String jobNo = getJobNoFromSession();
        String orderNo = getOrderNoFromSession();
        String siteCode = getSiteCodeFromSession();

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(ApiConfig.CONFIRM_ORDER))
                .newBuilder()
                .addQueryParameter("as_prin_code", prinCode)
                .addQueryParameter("as_job_no", jobNo)
                .addQueryParameter("as_order_no", orderNo)
                .addQueryParameter("as_login_id", loginId)
                .addQueryParameter("as_key_number", keyNumber)
                .addQueryParameter("as_qty1", String.valueOf(quantityPicked))
                .addQueryParameter("as_qty2", String.valueOf(0))
                .addQueryParameter("as_quantity", String.valueOf(quantityPicked))
                .addQueryParameter("as_site_code", siteCode)
                .addQueryParameter("as_location_code", locationCode)
                .build();

        Log.d(TAG, "API URL for Picking Details: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API Request failed for picking details", e);
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(InduvidualJobActivity.this, "Network error during picking confirmation: " + errorMessage, Toast.LENGTH_LONG).show();
                    showInitialScreenState();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                final String finalResponseBody = responseBody;
                final int finalResponseCode = response.code();
                final boolean finalIsSuccessful = response.isSuccessful();

                runOnUiThread(() -> {
                    if (finalIsSuccessful) {
                        boolean isSuccess = finalResponseBody.toUpperCase().contains("UPDATE SUCCESS") ||
                                finalResponseBody.toUpperCase().contains("SUCCESS");
                        if (isSuccess) {
                            if (currentOrderLocation != null) {
                                currentOrderLocation.setPdAVerified("Y");
                                saveLastPickedLocation(currentOrderLocation);
                            }
                            updateProgressDisplay();

                            if (isBarcodeScan) {
                                showScanResultScreenState(true);
                            } else {
                                Toast.makeText(InduvidualJobActivity.this, "Pick confirmed. Proceeding to next location.", Toast.LENGTH_LONG).show();
                                findNextUnverifiedLocationAndProceed(true);
                            }
                        } else {
                            Toast.makeText(InduvidualJobActivity.this, "Picking confirmation failed: " + finalResponseBody, Toast.LENGTH_LONG).show();
                            showInitialScreenState();
                        }
                    } else {
                        Toast.makeText(InduvidualJobActivity.this, "Server error during picking confirmation: " + finalResponseCode + " " + finalResponseBody, Toast.LENGTH_SHORT).show();
                        showInitialScreenState();
                    }
                });
            }
        });
    }

    private void findNextUnverifiedLocationAndProceed(boolean startFromNextIndex) {
        if (orderLocations == null || orderLocations.isEmpty()) {
            Toast.makeText(this, "No order locations available. Please use 'Jump To' to load an order.", Toast.LENGTH_LONG).show();
            updateUIWithLocationDetails(null);
            showInitialScreenState();
            saveLastPickedLocation(null);
            return;
        }

        if (currentOrderLocation != null && currentLocationIndex != -1 && currentLocationIndex < orderLocations.size()) {
            orderLocations.get(currentLocationIndex).setPdAVerified("Y");
        }

        int startIndex = startFromNextIndex ? currentLocationIndex + 1 : 0;
        if (startIndex < 0) startIndex = 0;

        Location nextLocation = null;
        int nextLocationIdx = -1;

        for (int i = startIndex; i < orderLocations.size(); i++) {
            Location location = orderLocations.get(i);
            if (location.getPdAVerified().equalsIgnoreCase("N")) {
                nextLocation = location;
                nextLocationIdx = i;
                break;
            }
        }

        if (nextLocation != null) {
            currentLocationIndex = nextLocationIdx;
            currentOrderLocation = nextLocation;
            updateUIWithLocationDetails(currentOrderLocation);
            saveLastPickedLocation(currentOrderLocation);
            showInitialScreenState();
            Toast.makeText(this, "Proceeding to next unverified location: " + currentOrderLocation.getLocationCode(), Toast.LENGTH_LONG).show();
        } else {
            boolean allLocationsVerified = true;
            for (Location loc : orderLocations) {
                if ("N".equalsIgnoreCase(loc.getPdAVerified())) {
                    allLocationsVerified = false;
                    break;
                }
            }

            if (allLocationsVerified) {
                Toast.makeText(this, "All locations picked. Finishing order.", Toast.LENGTH_LONG).show();

                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (jobNumber != null && !jobNumber.isEmpty()) {
                    editor.remove(JOB_INITIAL_TOTAL_PICKS_PREFIX + jobNumber);
                    editor.remove(JOB_INITIAL_TOTAL_QUANTITY_PREFIX + jobNumber);
                    editor.apply();
                    prefs.edit().remove(JOB_STARTED_PREFIX + jobNumber).apply();
                }

                Intent intent = new Intent(InduvidualJobActivity.this, ConfirmationPageActivity.class);
                intent.putExtra("ORDER_NUMBER", orderNumber);
                intent.putExtra("JOB_NUMBER", jobNumber);
                intent.putExtra("USER_ID", currentUserId);
                intent.putExtra("REFERENCE_CODE", referenceCode);
                intent.putExtra("PRIN_CODE", prinCode);
                intent.putExtra("INITIAL_API_TOTAL_PICKS", initialApiTotalPicks);
                intent.putExtra("INITIAL_API_TOTAL_QUANTITY", initialApiTotalQuantity);
                startActivity(intent);
                finish();
                saveLastPickedLocation(null);
            } else {
                Toast.makeText(this, "No more unverified locations found from current point. Use 'Jump To' to select another.", Toast.LENGTH_LONG).show();
                showInitialScreenState();
            }
        }
        updateProgressDisplay();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentOrderLocation != null) startCameraAndScanner();
                else fetchOrderLocations(true);
            } else {
                Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_LONG).show();
                showInitialScreenState();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MENU_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                boolean paused = data.getBooleanExtra("PAUSED", false);
                updateUIForPauseState(paused);
            }
        } else if (requestCode == PICK_LOCATION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<Parcelable> parcelableList = data.getParcelableArrayListExtra(PickLocationActivity.ORDER_LOCATIONS_LIST_KEY);
                int selectedIndex = data.getIntExtra(PickLocationActivity.SELECTED_LOCATION_INDEX_KEY, -1);

                ArrayList<Location> returnedLocations = new ArrayList<>();
                if (parcelableList != null) {
                    for (Parcelable p : parcelableList) {
                        if (p instanceof Location) returnedLocations.add((Location) p);
                    }
                }

                if (!returnedLocations.isEmpty()) {
                    this.orderLocations = returnedLocations;
                    this.originalTotalLocations = returnedLocations.size();

                    Location selectedLocationFromPicker = null;
                    if (selectedIndex != -1 && selectedIndex < returnedLocations.size()) {
                        selectedLocationFromPicker = returnedLocations.get(selectedIndex);
                    }

                    Location targetLocation = null;
                    int targetIndex = -1;

                    if (selectedLocationFromPicker != null && selectedLocationFromPicker.getPdAVerified().equalsIgnoreCase("N")) {
                        targetLocation = selectedLocationFromPicker;
                        targetIndex = selectedIndex;
                    } else {
                        for (int i = 0; i < returnedLocations.size(); i++) {
                            if (returnedLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) {
                                targetLocation = returnedLocations.get(i);
                                targetIndex = i;
                                break;
                            }
                        }
                    }

                    if (targetLocation != null) {
                        this.currentLocationIndex = targetIndex;
                        this.currentOrderLocation = targetLocation;
                        updateUIWithLocationDetails(currentOrderLocation);
                        saveLastPickedLocation(currentOrderLocation);
                        showInitialScreenState();
                        Toast.makeText(this, "Order location loaded: " + currentOrderLocation.getLocationCode(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No unverified locations found in the selected order.", Toast.LENGTH_LONG).show();
                        currentOrderLocation = null;
                        currentLocationIndex = -1;
                        updateUIWithLocationDetails(null);
                        saveLastPickedLocation(null);
                        showInitialScreenState();
                    }
                    updateProgressDisplay();
                } else {
                    Toast.makeText(this, "No unverified locations found for the order.", Toast.LENGTH_LONG).show();
                    currentOrderLocation = null;
                    orderLocations.clear();
                    currentLocationIndex = -1;
                    this.originalTotalLocations = 0;
                    updateUIWithLocationDetails(null);
                    saveLastPickedLocation(null);
                    showInitialScreenState();
                    updateProgressDisplay();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Location selection cancelled.", Toast.LENGTH_SHORT).show();
                if (currentOrderLocation == null) updateUIWithLocationDetails(null);
                showInitialScreenState();
                updateProgressDisplay();
            }
        }
    }

    private void updateUIForPauseState(boolean isPaused) {
        if (isPaused) {
            if (isScanning.compareAndSet(true, false)) {
                if (mCameraProvider != null) mCameraProvider.unbindAll();
            }
            if (previewView != null) previewView.setVisibility(View.GONE);
            if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
            if (btnScanBarcode != null) btnScanBarcode.setText(getString(R.string.resume_text));
            if (menuLayout != null) menuLayout.setVisibility(View.GONE);
            if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
            if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
            if (pausedStatusText != null) pausedStatusText.setVisibility(View.VISIBLE);
            if (btnScanBarcode != null && btnScanBarcode.getVisibility() == View.VISIBLE) btnScanBarcode.requestFocus();
        } else {
            showInitialScreenState();
        }
    }

    private void updateProgressDisplay() {
        if (progressBar == null || progressText == null) return;

        int totalLocations = this.originalTotalLocations;
        int completedLocations = 0;
        for (Location loc : orderLocations) {
            if (loc.getPdAVerified() != null && loc.getPdAVerified().equalsIgnoreCase("Y")) {
                completedLocations++;
            }
        }

        if (totalLocations == 0) {
            progressText.setText("0/0");
            progressBar.setProgress(0);
            return;
        }

        progressText.setText(String.format(Locale.getDefault(), "%d/%d", completedLocations, totalLocations));
        int progressPercentage = (completedLocations * 100) / totalLocations;
        progressBar.setProgress(progressPercentage);
    }


    private void startJobPicking() {
        if (currentUserId == null || orderNumber == null || jobNumber == null) {
            Toast.makeText(this, "Missing user or order information", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean jobAlreadyStarted = prefs.getBoolean(JOB_STARTED_PREFIX + jobNumber, false);

        if (jobAlreadyStarted) return;

        try {
            StringBuilder urlBuilder = new StringBuilder(ApiConfig.START_JOB_PICKING);
            urlBuilder.append("?as_login_id=").append(URLEncoder.encode(currentUserId, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&order_no=").append(URLEncoder.encode(orderNumber, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&job_no=").append(URLEncoder.encode(jobNumber, StandardCharsets.UTF_8.toString()));

            String finalUrl = urlBuilder.toString();

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(InduvidualJobActivity.this, "Failed to start job: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = "";
                    try (ResponseBody body = response.body()) {
                        if (body != null) responseBody = body.string();
                    }

                    final String finalResponseBody = responseBody;

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            markJobAsStarted(jobNumber);
                            Toast.makeText(InduvidualJobActivity.this, "Job picking started", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(InduvidualJobActivity.this, "Server error starting job: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error preparing start job request: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void pauseJobPicking() {
        if (currentUserId == null || orderNumber == null || jobNumber == null) return;

        try {
            StringBuilder urlBuilder = new StringBuilder(ApiConfig.PAUSE_JOB_PICKING);
            urlBuilder.append("?as_login_id=").append(URLEncoder.encode(currentUserId, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&order_no=").append(URLEncoder.encode(orderNumber, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&job_no=").append(URLEncoder.encode(jobNumber, StandardCharsets.UTF_8.toString()));

            String finalUrl = urlBuilder.toString();

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { }
            });

        } catch (Exception ignored) {}
    }

    private void resumeJobPicking() {
        if (currentUserId == null || orderNumber == null || jobNumber == null) {
            Toast.makeText(this, "Missing user or order information", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(ApiConfig.RESUME_JOB_PICKING);
            urlBuilder.append("?as_login_id=").append(URLEncoder.encode(currentUserId, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&order_no=").append(URLEncoder.encode(orderNumber, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&job_no=").append(URLEncoder.encode(jobNumber, StandardCharsets.UTF_8.toString()));

            String finalUrl = urlBuilder.toString();

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(InduvidualJobActivity.this, "Failed to resume job: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = "";
                    try (ResponseBody body = response.body()) {
                        if (body != null) responseBody = body.string();
                    }
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(InduvidualJobActivity.this, "Job picking resumed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(InduvidualJobActivity.this, "Failed to resume job: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error preparing resume job request: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void markJobAsStarted(String jobNo) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean(JOB_STARTED_PREFIX + jobNo, true).apply();
    }

    private void sendPauseJobPickingApiCall() {
        String loginId = getLoginIdFromSession();
        String orderNo = getOrderNoFromSession();
        String jobNo = getJobNoFromSession();

        if (loginId == null || loginId.isEmpty() || orderNo == null || orderNo.isEmpty() || jobNo == null || jobNo.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + jobNo, false);
        if (!hasJobBeenStarted) return;

        try {
            String encodedLoginId = URLEncoder.encode(loginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(orderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(jobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    ApiConfig.PAUSE_JOB_PICKING, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { }
            });
        } catch (Exception ignored) {}
    }

    private void sendResumeJobPickingApiCall() {
        String loginId = getLoginIdFromSession();
        String orderNo = getOrderNoFromSession();
        String jobNo = getJobNoFromSession();

        if (loginId == null || loginId.isEmpty() || orderNo == null || orderNo.isEmpty() || jobNo == null || jobNo.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + jobNo, false);
        if (!hasJobBeenStarted) return;

        try {
            String encodedLoginId = URLEncoder.encode(loginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(orderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(jobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    ApiConfig.RESUME_JOB_PICKING, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { }
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);
        if (isScanning.get() && !isChangingConfigurations() && !isShortQuantityInputVisible) {
            stopCameraAndScanner(false);
        }
        if (timerHandler != null && timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
        if (uiHandler != null) uiHandler.removeCallbacksAndMessages(null);

        if (!isFinishing()) {
            sendPauseJobPickingApiCall();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean jobStarted = prefs.getBoolean(JOB_STARTED_PREFIX + jobNumber, false);

        if (jobStarted && jobNumber != null && !jobNumber.isEmpty()) {
            resumeJobPicking();
        }

        boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);
        boolean isShowingSkuStatus = (skuStatusText != null && skuStatusText.getVisibility() == View.VISIBLE);
        boolean isInPausedState = (pausedStatusText != null && pausedStatusText.getVisibility() == View.VISIBLE);

        if (currentOrderLocation != null && !isShortQuantityInputVisible && !isShowingSkuStatus) {
            sendResumeJobPickingApiCall();
        }

        if (timerRunnable != null) timerHandler.post(timerRunnable);
        else startTimer();

        if (isShortQuantityInputVisible) {
            if (enteredQuantityBuilder.length() > 0) tvEnteredQuantity.setText(enteredQuantityBuilder.toString());
            else tvEnteredQuantity.setText("0");
        } else if (isScanning.get() && !isShowingSkuStatus && !isInPausedState &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraAndScanner();
        } else if (!isShowingSkuStatus && !isInPausedState) {
            showInitialScreenState();
        } else if (isInPausedState) {
            if (btnScanBarcode != null && btnScanBarcode.getVisibility() == View.VISIBLE &&
                    btnScanBarcode.getText().toString().equalsIgnoreCase(getString(R.string.resume_text))) {
                btnScanBarcode.requestFocus();
            }
        }

        updateProgressDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) cameraExecutor.shutdown();
        if (uiHandler != null) uiHandler.removeCallbacksAndMessages(null);
        if (barcodeScanner != null) barcodeScanner.close();
        if (timerHandler != null && timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
        Log.d(TAG, "onDestroy called.");
    }
}
