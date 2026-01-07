package com.example.Pickbyvision.Consolidated_Pick;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.Pickbyvision.Induvidual_Pick.network.ApiConfig;

import com.example.Pickbyvision.Induvidual_Pick.Barcodescanner;
import com.example.Pickbyvision.Consolidated_Pick.location.Location;

import okhttp3.Call;
import okhttp3.Callback;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;

import java.util.Collections;
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

import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Consolidated_Pick.model.ApiMessage;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedPickDetail;
import com.example.Pickbyvision.Consolidated_Pick.model.ScanBarcodeRequest;
import com.example.Pickbyvision.Consolidated_Pick.repo.ConsolidatedRepository;
import com.example.Pickbyvision.R;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ConsolidatedJobActivity extends AppCompatActivity {

    private static final String TAG = "ConsolidatedJob";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 4441;

    private static final int KEYCODE_NEXT_SUMMARY = KeyEvent.KEYCODE_FORWARD;


    private static final int REQ_PICK_LOCATION = 3021;

    private static final String PREF_CONS_BATCH_TOTAL = "cons_batch_total_";






    private final ConsolidatedRepository repo = new ConsolidatedRepository();


    private TextView tvLocation, tvQuantity, tvProduct, tvDesc;
    private MaterialButton btnJumpTo;

    private boolean isScrolling = false;

    private static final int SCROLL_DELAY = 300;


    private TextView manufacturingText, expirationText;
    private TextView manufacturingLabelText, expirationLabelText;

    private static final java.text.SimpleDateFormat OUT_FMT =
            new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());


    private Handler scrollingHandler = new Handler(Looper.getMainLooper());

    private PreviewView cameraPreview;
    private TextView skuStatusText;
    private View orderInfoContainer;
    private View menuLayout;
    private Button btnScanBarcode;



    private OkHttpClient httpClient;


    private boolean startApiHitInThisSession = false;



    private final Set<String> completedLocationCodes = new HashSet<>();
    private static final String PREF_CONS_COMPLETED_LOCS = "cons_completed_locs_";

    private View shortQuantityInputContainer;
    private NumberPicker npSingleDigitInput;
    private TextView tvEnteredQuantity;
    private Button btnAddDigit, btnBackspaceDigit, btnCancelShortInput, btnConfirmShortInput;

    private ProgressBar progressBar;
    private TextView progressText;
    private int totalItems = 0;

    private int originalTotalLocations = 0;
    private int completedItems = 0;

    private int batchTotalItems = 0;
    private int currentLocationIndex = 0;
    private static final String PREF_CONS_TOTAL = "cons_total_";
    private static final String PREF_CONS_DONE  = "cons_done_";
    private static final String PREF_CONS_CURRENT_INDEX = "cons_current_index_";

    private ExecutorService cameraExecutor;

    private ProcessCameraProvider cameraProvider;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    private String companyCode, prinCode, transBatchId, jobNo, siteCode, locationCode, prodCode, pickUser, orderNo;


    private String loginId;

    private ConsolidatedPickDetail detail;

    private int pickedQty = 0;
    private String lastScanned = "";
    private final StringBuilder enteredQuantityBuilder = new StringBuilder();

    private long startTime = 0L;
    private long elapsedTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private TextView timerTextView;
    private int successfulPicksCount = 0;

    private List<Location> allLocations = Collections.emptyList();
    
    


    private volatile boolean navigatedAway = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consolidated_job);

        tvLocation = findViewById(R.id.locationText);
        tvQuantity = findViewById(R.id.quantityText);
        tvProduct  = findViewById(R.id.productText);
        tvDesc     = findViewById(R.id.descriptionText);
        btnJumpTo  = findViewById(R.id.btnJumpTo);

        manufacturingText      = findViewById(R.id.manufacturingText);
        expirationText         = findViewById(R.id.expirationText);
        manufacturingLabelText = findViewById(R.id.manufacturingLabelText);
        expirationLabelText    = findViewById(R.id.expirationLabelText);

        setVisible(manufacturingText, false);
        setVisible(expirationText, false);
        setVisible(manufacturingLabelText, false);
        setVisible(expirationLabelText, false);

        cameraPreview      = findViewById(R.id.camera_preview);
        skuStatusText      = findViewById(R.id.skuStatusText);
        orderInfoContainer = findViewById(R.id.orderInfoContainer);
        menuLayout         = findViewById(R.id.menuLayout);
        btnScanBarcode     = findViewById(R.id.btnScanBarcode);

        shortQuantityInputContainer = findViewById(R.id.shortQuantityInputContainer);
        npSingleDigitInput          = findViewById(R.id.npSingleDigitInput);
        tvEnteredQuantity           = findViewById(R.id.tvEnteredQuantity);
        btnAddDigit                 = findViewById(R.id.btnAddDigit);
        btnBackspaceDigit           = findViewById(R.id.btnBackspaceDigit);
        btnCancelShortInput         = findViewById(R.id.btnCancelShortInput);
        btnConfirmShortInput        = findViewById(R.id.btnConfirmShortInput);

        timerTextView = findViewById(R.id.timerTextView);
        startTimer();

        progressBar  = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        setupNumberPicker();
        setupShortButtonHandlers();

        orderNo      = getIntent().getStringExtra("ORDER_NO");
        companyCode  = getIntent().getStringExtra("COMPANY_CODE");
        prinCode     = getIntent().getStringExtra("PRIN_CODE");
        transBatchId = getIntent().getStringExtra("TRANS_BATCH_ID");
        jobNo        = getIntent().getStringExtra("JOB_NO");
        siteCode     = getIntent().getStringExtra("SITE_CODE");
        locationCode = getIntent().getStringExtra("LOCATION_CODE");
        prodCode     = getIntent().getStringExtra("PROD_CODE");
        pickUser     = getIntent().getStringExtra("PICK_USER");
        loginId      = getIntent().getStringExtra("LOGIN_ID");

        if (loginId == null || loginId.trim().isEmpty()) {
            loginId = pickUser;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String batchKey = n(transBatchId);

        boolean isFreshStart = getIntent().getBooleanExtra("FRESH_START", false);

        if (isFreshStart) {
            Log.d(TAG, "🔄 FRESH START detected - Clearing previous locked value (was: " + originalTotalLocations + ")");

            prefs.edit()
                    .remove(PREF_CONS_BATCH_TOTAL + batchKey)
                    .remove(PREF_CONS_TOTAL + batchKey)
                    .remove(PREF_CONS_DONE + batchKey)
                    .remove(PREF_CONS_CURRENT_INDEX + batchKey)
                    .remove(PREF_CONS_COMPLETED_LOCS + batchKey)
                    .apply();

            originalTotalLocations = -1;
        }
        originalTotalLocations = prefs.getInt(PREF_CONS_BATCH_TOTAL + batchKey, -1);

        if (originalTotalLocations == -1) {
            int intentTotal = getIntent().getIntExtra("TOTAL_ITEMS", 0);

            if (intentTotal > 0) {
                originalTotalLocations = intentTotal;

                prefs.edit()
                        .putInt(PREF_CONS_BATCH_TOTAL + batchKey, originalTotalLocations)
                        .apply();

                Log.d(TAG, "LOCKED original total locations at: " + originalTotalLocations);

                prefs.edit()
                        .remove(PREF_CONS_TOTAL + batchKey)
                        .remove(PREF_CONS_DONE + batchKey)
                        .remove(PREF_CONS_CURRENT_INDEX + batchKey)
                        .remove(PREF_CONS_COMPLETED_LOCS + batchKey)
                        .apply();
            } else {
                Log.w(TAG, " No TOTAL_ITEMS in Intent and no saved total. Defaulting to 0.");
                originalTotalLocations = 0;
            }
        } else {
            Log.d(TAG, "Using LOCKED original total: " + originalTotalLocations);
        }

        batchTotalItems = originalTotalLocations;

        loadCompletedLocations();

        int t   = getIntent().getIntExtra("TOTAL_ITEMS", -1);
        int d   = getIntent().getIntExtra("COMPLETED_ITEMS", -1);
        int idx = getIntent().getIntExtra("CURRENT_LOCATION_INDEX", -1);

        if (t == -1)   t   = prefs.getInt(PREF_CONS_TOTAL + batchKey, originalTotalLocations);
        if (d == -1)   d   = prefs.getInt(PREF_CONS_DONE  + batchKey, 0);
        if (idx == -1) idx = prefs.getInt(PREF_CONS_CURRENT_INDEX + batchKey, 0);

        if (t < 0)   t   = originalTotalLocations;
        if (d < 0)   d   = 0;
        if (idx < 0) idx = 0;

        totalItems           = t;
        completedItems       = d;
        currentLocationIndex = idx;
        successfulPicksCount = completedItems;
        Log.d(TAG, "📊 Progress loaded: totalItems=" + totalItems +
                ", completedItems=" + completedItems +
                ", currentIndex=" + currentLocationIndex +
                ", originalTotal(locked)=" + originalTotalLocations);

        persistProgress();
        updateProgressUI();

        loadAllLocations();

        new Thread(() -> {
            try {
                detail = repo.getConsolidatedPickDetailBlocking(
                        n(companyCode), n(prinCode), n(transBatchId),
                        n(jobNo), n(siteCode), n(locationCode), n(prodCode)
                );
                runOnUiThread(this::bindUi);
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Detail load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();

        Button logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> LogoutManager.performLogout(this));
        }

        btnJumpTo.setOnClickListener(v -> {
            stopCameraIfAny();
            showInitialState();
            Intent i = new Intent(this, LocationDetailsActivity.class);
            i.putExtra("TRANS_BATCH_ID", n(transBatchId));
            i.putExtra("COMPANY_CODE",   n(companyCode));
            i.putExtra("PRIN_CODE",      n(prinCode));
            i.putExtra("PICK_USER",      n(pickUser));
            i.putExtra("SELECT_ONLY", true);
            i.putExtra("CURRENT_LOCATION_CODE", n(locationCode));
            i.putExtra("SELECTED_INDEX", currentLocationIndex);
            startActivityForResult(i, REQ_PICK_LOCATION);
        });

        btnScanBarcode.setOnClickListener(v -> {
            if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, "Confirm/close short quantity first.", Toast.LENGTH_SHORT).show();
                return;
            }
            startOrAskCamera();
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        View btnShort = findViewById(R.id.btnShort);
        if (btnShort != null) {
            btnShort.setOnClickListener(v -> showShortQuantityInput());
        }

        showInitialState();
        updateProgressUI();

        initializeHttpClient();

        handleStartOrResumeFlow();

    }

    private void initializeHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
                    .build();

            Log.d(TAG, "HTTP client initialized with custom SSL configuration");

        } catch (Exception e) {
            Log.e(TAG, "Failed to create HTTP client with custom SSL config, falling back to default", e);
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
    }

    private String buildConsUrl(String baseUrl) throws Exception {
        return baseUrl
                + "?as_login_id=" + URLEncoder.encode(loginId, StandardCharsets.UTF_8.name())
                + "&as_trans_batch_id=" + URLEncoder.encode(transBatchId, StandardCharsets.UTF_8.name());
    }

    private void handleStartOrResumeFlow() {

        if (loginId == null || transBatchId == null || httpClient == null) {
            Log.e(TAG, "Cannot determine start/resume - missing data");
            return;
        }

        try {
            String url = buildConsUrl(ApiConfig.GET_CONSOLIDATED_STATUS);
            Log.d(TAG, "GET_CONSOLIDATED_STATUS URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader(ApiConfig.HEADER_ACCEPT, ApiConfig.ACCEPT_ALL)
                    .addHeader(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT_VALUE)
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "STATUS check failed", e);

                    runOnUiThread(() -> startConsolidatedJobPicking());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "STATUS response: " + body);

                    String status = parseStatus(body);

                    runOnUiThread(() -> decideActionFromStatus(status));
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error during STATUS flow", e);
            startConsolidatedJobPicking();
        }
    }
    private String parseStatus(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("status", "").toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private void decideActionFromStatus(String status) {

        Log.d(TAG, "Consolidated status = " + status);

        switch (status) {

            case "":
            case "NOT_STARTED":
                Log.d(TAG, "No active session → START");
                startConsolidatedJobPicking();
                break;

            case "PAUSED":
                Log.d(TAG, "Paused session → RESUME");
                resumeConsolidatedJobPicking();
                break;

            case "STARTED":
                Log.d(TAG, "Already STARTED → no action");
                break;

            case "COMPLETED":
                Log.d(TAG, "Already COMPLETED → no action / navigate");
                break;

            default:
                Log.w(TAG, "Unknown status → START as fallback");
                startConsolidatedJobPicking();
                break;
        }
    }

    private void resumeConsolidatedJobPicking() {

        if (loginId == null || transBatchId == null || httpClient == null) {
            Log.w(TAG, "Resume skipped - missing data or httpClient");
            return;
        }

        try {
            String finalUrl = buildConsUrl(ApiConfig.RESUME_CONSOLIDATED_PICKING);
            Log.d(TAG, "RESUME_CONSOLIDATED_PICKING URL: " + finalUrl);

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader("accept", "*/*")
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "RESUME_CONSOLIDATED_PICKING failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    Log.d(TAG, "RESUME response code: " + response.code());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing RESUME request", e);
        }
    }



    private void startConsolidatedJobPicking() {

        if (startApiHitInThisSession) {
            Log.d(TAG, "START_CONSOLIDATED_PICKING already hit in this session");
            return;
        }

        if (loginId == null || loginId.trim().isEmpty()
                || transBatchId == null || transBatchId.trim().isEmpty()) {

            Log.e(TAG, "Cannot start consolidated picking - missing loginId or transBatchId");
            Toast.makeText(this, "Missing user or batch information", Toast.LENGTH_SHORT).show();
            return;
        }

        if (httpClient == null) {
            Log.e(TAG, "httpClient is NULL - initializeHttpClient() not called");
            Toast.makeText(this, "Network client not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(ApiConfig.START_CONSOLIDATED_PICKING);
            urlBuilder.append("?as_login_id=")
                    .append(URLEncoder.encode(loginId, StandardCharsets.UTF_8.name()));
            urlBuilder.append("&as_trans_batch_id=")
                    .append(URLEncoder.encode(transBatchId, StandardCharsets.UTF_8.name()));

            String finalUrl = urlBuilder.toString();
            Log.d(TAG, "START_CONSOLIDATED_PICKING URL: " + finalUrl);

            startApiHitInThisSession = true;

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader("accept", "*/*")
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "START_CONSOLIDATED_PICKING failed", e);

                    startApiHitInThisSession = false;

                    runOnUiThread(() ->
                            Toast.makeText(
                                    ConsolidatedJobActivity.this,
                                    "Failed to start consolidated picking: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    String responseBody = "";
                    try (ResponseBody body = response.body()) {
                        if (body != null) responseBody = body.string();
                    }

                    Log.d(TAG, "START_CONSOLIDATED_PICKING response code: " + response.code());
                    Log.d(TAG, "START_CONSOLIDATED_PICKING response body: " + responseBody);

                    String finalResponseBody = responseBody;

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Consolidated picking START successful");
                            Toast.makeText(
                                    ConsolidatedJobActivity.this,
                                    "Consolidated picking started",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            startApiHitInThisSession = false;

                            Log.e(TAG, "Server error starting consolidated picking: "
                                    + response.code() + " " + finalResponseBody);

                            Toast.makeText(
                                    ConsolidatedJobActivity.this,
                                    "Server error starting batch: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            startApiHitInThisSession = false;

            Log.e(TAG, "Error creating START_CONSOLIDATED_PICKING request", e);
            Toast.makeText(
                    this,
                    "Error preparing start request: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void pauseConsolidatedJobPicking() {

        if (loginId == null || transBatchId == null || httpClient == null) {
            Log.w(TAG, "Pause skipped - missing data or httpClient");
            return;
        }

        try {
            String finalUrl = buildConsUrl(ApiConfig.PAUSE_CONSOLIDATED_PICKING);
            Log.d(TAG, "PAUSE_CONSOLIDATED_PICKING URL: " + finalUrl);

            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader("accept", "*/*")
                    .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "PAUSE_CONSOLIDATED_PICKING failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    Log.d(TAG, "PAUSE response code: " + response.code());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing PAUSE request", e);
        }
    }







    private void loadAllLocations() {
        final String batchId = n(transBatchId);
        final String comp    = n(companyCode);
        final String prin    = n(prinCode);
        final String user    = n(pickUser);

        if (batchId.isEmpty() || comp.isEmpty() || prin.isEmpty() || user.isEmpty()) {
            Log.w(TAG, "Not loading allLocations – missing required params.");
            return;
        }

        new Thread(() -> {
            try {
                List<Location> list = repo.getConsolidatedPicklistBlocking(comp, prin, batchId, user);
                if (list != null) {
                    list.removeIf(loc -> loc == null || !batchId.equals(n(loc.getTransBatchId())));
                    allLocations = list;
                } else {
                    allLocations = Collections.emptyList();
                }

                if (allLocations != null) {
                    totalItems = allLocations.size();

                    Log.d(TAG, "Loaded " + totalItems + " locations. " +
                            "Original total (locked): " + originalTotalLocations);
                }

                runOnUiThread(this::updateProgressUI);

            } catch (IOException e) {
                Log.e(TAG, "Failed to load picklist in job screen", e);
            }
        }).start();
    }



    private void moveToFirstLocationAndReload() {
        if (allLocations == null || allLocations.isEmpty()) {
            Log.w(TAG, "No locations available to loop back to.");
            return;
        }

        int firstIncompleteIndex = -1;
        for (int i = 0; i < allLocations.size(); i++) {
            Location loc = allLocations.get(i);
            if (loc != null) {
                String locCode = n(loc.getLocationCode());
                if (!completedLocationCodes.contains(locCode)) {
                    firstIncompleteIndex = i;
                    break;
                }
            }
        }

        if (firstIncompleteIndex == -1) {
            Log.d(TAG, "All locations are completed. Navigating to summary.");
            goNext();
            return;
        }

        currentLocationIndex = firstIncompleteIndex;
        Location firstLoc = allLocations.get(firstIncompleteIndex);
        String batchId = n(transBatchId);

        Log.d(TAG, "Looping back to first incomplete location at index: " + currentLocationIndex);
        loadLocationDetail(firstLoc, batchId);
    }


    private void moveToNextLocationAndReload() {
        if (allLocations == null || allLocations.isEmpty()) {
            Log.w(TAG, "No locations to move to.");
            return;
        }

        int nextIncompleteIndex = -1;
        for (int i = currentLocationIndex + 1; i < allLocations.size(); i++) {
            Location loc = allLocations.get(i);
            if (loc != null) {
                String locCode = n(loc.getLocationCode());
                if (!completedLocationCodes.contains(locCode)) {
                    nextIncompleteIndex = i;
                    break;
                }
            }
        }

        if (nextIncompleteIndex == -1) {
            Log.d(TAG, "No incomplete locations ahead. Looping back to first incomplete location.");
            moveToFirstLocationAndReload();
            return;
        }

        currentLocationIndex = nextIncompleteIndex;
        Location nextLoc = allLocations.get(nextIncompleteIndex);
        String batchId = n(transBatchId);

        Log.d(TAG, "Moving to next incomplete location at index: " + currentLocationIndex);
        loadLocationDetail(nextLoc, batchId);
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

    private void bindUi() {
        if (detail == null) return;
        tvDesc.setText(n(detail.getProdName()));
        tvProduct.setText(n(detail.getProdCode()));
        String site = n(detail.getSiteCode());
        String loc  = n(detail.getLocationCode());

        tvLocation.setText(site + loc);


        int maxQty = 0;
        int pdaQty = 0;

        Integer apiMax = detail.getQuantity();
        Integer apiPda = detail.getPdaQuantity();

        if (apiMax != null) maxQty = apiMax;
        if (apiPda != null) pdaQty = apiPda;

        int remainingQty = maxQty - pdaQty;
        if (remainingQty < 0) remainingQty = 0;

        pickedQty = remainingQty;
        tvQuantity.setText(String.valueOf(remainingQty));

        bindDates(detail);
    }




    private void setupNumberPicker() {
        if (npSingleDigitInput != null) {
            npSingleDigitInput.setMinValue(0);
            npSingleDigitInput.setMaxValue(9);
            npSingleDigitInput.setWrapSelectorWheel(false);
        }
    }

    private void setupShortButtonHandlers() {
        if (btnAddDigit != null) {
            btnAddDigit.setOnClickListener(v -> handleAddDigit());
        }
        if (btnBackspaceDigit != null) {
            btnBackspaceDigit.setOnClickListener(v -> handleBackspaceDigit());
        }
        if (btnConfirmShortInput != null) {
            btnConfirmShortInput.setOnClickListener(v -> handleConfirmShortQuantity());
        }
        if (btnCancelShortInput != null) {
            btnCancelShortInput.setOnClickListener(v -> handleCancelShortQuantity());
        }
    }


    private void loadCompletedLocations() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String batchKey = n(transBatchId);
        String saved = prefs.getString(PREF_CONS_COMPLETED_LOCS + batchKey, "");

        completedLocationCodes.clear();
        if (!saved.isEmpty()) {
            String[] codes = saved.split(",");
            for (String code : codes) {
                if (!code.trim().isEmpty()) {
                    completedLocationCodes.add(code.trim());
                }
            }
        }

        Log.d(TAG, "Loaded completed locations: " + completedLocationCodes);
    }




    private void saveCompletedLocations() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String batchKey = n(transBatchId);

        String joined = TextUtils.join(",", completedLocationCodes);

        prefs.edit()
                .putString(PREF_CONS_COMPLETED_LOCS + batchKey, joined)
                .apply();

        Log.d(TAG, "Saved completed locations: " + completedLocationCodes);
    }

    private void showShortQuantityInput() {
        if (isScanning.compareAndSet(true, false)) {
            if (cameraProvider != null) cameraProvider.unbindAll();
        }

        setVisible(cameraPreview, false);
        setVisible(orderInfoContainer, false);
        setVisible(menuLayout, false);
        setVisible(skuStatusText, false);

        if (shortQuantityInputContainer != null) {
            shortQuantityInputContainer.setVisibility(View.VISIBLE);
            enteredQuantityBuilder.setLength(0);
            if (tvEnteredQuantity != null) tvEnteredQuantity.setText("0");
            if (npSingleDigitInput != null) {
                npSingleDigitInput.setValue(0);
                npSingleDigitInput.requestFocus();
            }
        }
    }

    private void hideShortQuantityInputAndShowMenu() {
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        showInitialState();
    }

    private void handleAddDigit() {
        if (npSingleDigitInput == null || tvEnteredQuantity == null) return;
        int d = npSingleDigitInput.getValue();

        if (enteredQuantityBuilder.length() == 0 && d == 0) {
            tvEnteredQuantity.setText("0");
            return;
        }

        final int maxDigits = 6;
        if (enteredQuantityBuilder.length() >= maxDigits) {
            Toast.makeText(this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
            return;
        }

        String candidateStr = enteredQuantityBuilder.toString() + d;
        int candidateValue;
        try {
            candidateValue = Integer.parseInt(candidateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (detail != null) {
            Integer apiMax = detail.getQuantity();
            Integer apiPda = detail.getPdaQuantity();

            int maxQty = (apiMax != null) ? apiMax : 0;
            int pdaQty = (apiPda != null) ? apiPda : 0;
            int remainingQty = maxQty - pdaQty;
            if (remainingQty < 0) remainingQty = 0;

            if (candidateValue > remainingQty) {
                Toast.makeText(
                        this,
                        "Requested qty (" + candidateValue + ") is more than available (" + remainingQty + ").",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
        }

        enteredQuantityBuilder.append(d);
        tvEnteredQuantity.setText(enteredQuantityBuilder.toString());

        npSingleDigitInput.setValue(0);
        npSingleDigitInput.requestFocus();
    }



    private void handleBackspaceDigit() {
        if (tvEnteredQuantity == null) return;
        if (enteredQuantityBuilder.length() > 0) {
            enteredQuantityBuilder.setLength(enteredQuantityBuilder.length() - 1);
            tvEnteredQuantity.setText(enteredQuantityBuilder.length() == 0 ? "0" : enteredQuantityBuilder.toString());
        } else {
            tvEnteredQuantity.setText("0");
            Toast.makeText(this, "No digits to delete.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleConfirmShortQuantity() {
        if (detail == null) {
            Toast.makeText(this, "Detail not loaded yet.", Toast.LENGTH_LONG).show();
            handleCancelShortQuantity();
            return;
        }

        int shortQty;
        if (enteredQuantityBuilder.length() == 0 ||
                (enteredQuantityBuilder.length() == 1 && enteredQuantityBuilder.charAt(0) == '0')) {
            shortQty = 0;
        } else {
            try {
                shortQty = Integer.parseInt(enteredQuantityBuilder.toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid short quantity entered.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (shortQty < 0) {
            Toast.makeText(this, "Short quantity cannot be negative.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer apiMax = detail.getQuantity();
        Integer apiPda = detail.getPdaQuantity();

        int maxQty = (apiMax != null) ? apiMax : 0;
        int pdaQty = (apiPda != null) ? apiPda : 0;
        int remainingBeforeShort = maxQty - pdaQty;
        if (remainingBeforeShort < 0) remainingBeforeShort = 0;

        if (shortQty > remainingBeforeShort) {
            Toast.makeText(
                    this,
                    "Short quantity (" + shortQty + ") cannot be more than remaining (" + remainingBeforeShort + ").",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        int finalPicked = remainingBeforeShort - shortQty;

        pickedQty = finalPicked;
        tvQuantity.setText(String.valueOf(finalPicked));

        if (shortQty == 0) {
            Toast.makeText(
                    this,
                    "No short. Picking " + finalPicked + " out of " + remainingBeforeShort + ". Scan barcode to confirm pick.",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Short " + shortQty + ". Picking " + finalPicked + " out of " + remainingBeforeShort + ". Scan barcode to confirm pick.",
                    Toast.LENGTH_LONG
            ).show();
        }

        hideShortQuantityInputAndShowMenu();
    }




    private void handleCancelShortQuantity() {
        enteredQuantityBuilder.setLength(0);
        if (tvEnteredQuantity != null) tvEnteredQuantity.setText("0");
        hideShortQuantityInputAndShowMenu();
    }

    private void showInitialState() {
        setVisible(cameraPreview, false);
        setVisible(orderInfoContainer, true);
        setVisible(menuLayout, true);
        setVisible(skuStatusText, false);
        if (btnScanBarcode != null) btnScanBarcode.setEnabled(true);

        updateProgressUI();
    }




    private void showScanningState() {
        setVisible(cameraPreview, true);
        setVisible(orderInfoContainer, false);
        setVisible(menuLayout, false);
        setVisible(skuStatusText, false);
        if (btnScanBarcode != null) btnScanBarcode.setEnabled(false);
    }

    private void showScanResult(boolean right) {
        setVisible(cameraPreview, false);
        setVisible(orderInfoContainer, false);
        setVisible(menuLayout, false);
        setVisible(skuStatusText, true);
        skuStatusText.setText(right ? "RIGHT SKU" : "WRONG SKU");
        skuStatusText.setTextColor(getColor(right ? android.R.color.holo_green_light
                : android.R.color.holo_red_light));
        skuStatusText.postDelayed(() -> {
            if (!navigatedAway) showInitialState();
        }, 3000);
    }

    private void setVisible(View v, boolean show) {
        if (v != null) v.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startOrAskCamera() {
        if (n(locationCode).isEmpty()) {
            Toast.makeText(this, "No active location. Select an item first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraAndScanner();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void startCameraAndScanner() {
        isScanning.set(true);
        showScanningState();

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindUseCases(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Camera provider error", e);
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showInitialState();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCameraIfAny() {
        if (cameraProvider != null) cameraProvider.unbindAll();
        isScanning.set(false);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindUseCases(@NonNull ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, image -> {
            if (!isScanning.get()) { image.close(); return; }

            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage input = InputImage.fromMediaImage(
                        mediaImage,
                        image.getImageInfo().getRotationDegrees()
                );

                barcodeScanner.process(input)
                        .addOnSuccessListener(barcodes -> {
                            if (!isScanning.get() || barcodes.isEmpty()) return;

                            if (isScanning.compareAndSet(true, false)) {
                                stopCameraIfAny();
                                String scanned = barcodes.get(0).getRawValue();
                                handleScan(scanned);
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Barcode scan failed", e))
                        .addOnCompleteListener(t -> image.close());
            } else {
                image.close();
            }
        });

        try {
            provider.unbindAll();
            provider.bindToLifecycle(this, selector, preview, analysis);
        } catch (Exception e) {
            Log.e(TAG, "bindToLifecycle failed", e);
            stopCameraIfAny();
            showInitialState();
        }
    }

    private void handleScan(String scannedValue) {
        if (scannedValue == null) {
            Toast.makeText(this, "No barcode detected.", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        String expectedRaw = n(locationCode);

        String gotRawFull = scannedValue == null ? "" : scannedValue;

        String gotRaw = extractLocationFromScan(gotRawFull);

        String expected = normalizeForCompare(expectedRaw);
        String got      = normalizeForCompare(gotRaw);

        Log.d(TAG, "handleScan() - expectedRaw: '" + expectedRaw + "'");
        Log.d(TAG, "handleScan() - scanned full: '" + gotRawFull + "', extracted: '" + gotRaw + "'");
        Log.d(TAG, "handleScan() - normalized expected: '" + expected + "', normalized got: '" + got + "'");

        if (got.isEmpty()) {
            Toast.makeText(this, "No barcode detected.", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        if (!got.equals(expected)) {
            Toast.makeText(this, "WRONG SKU (expected location barcode)", Toast.LENGTH_SHORT).show();
            showScanResult(false);
            return;
        }

        if (detail == null) {
            Toast.makeText(this, "Detail not loaded yet. Cannot send scan.", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        sendScanToServer();
    }


    private void sendScanToServer() {
        if (detail == null) {
            Toast.makeText(this, "Detail not loaded yet", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        String orderNumberToSend;
        try {
            String detailOrderNo = n(detail.getOrderNo());
            if (!detailOrderNo.isEmpty()) {
                orderNumberToSend = detailOrderNo;
            } else {
                orderNumberToSend = n(orderNo);
            }
        } catch (Exception ignored) {
            orderNumberToSend = n(orderNo);
        }

        ScanBarcodeRequest body = new ScanBarcodeRequest(
                n(companyCode),
                n(prinCode),
                n(transBatchId),
                n(jobNo),
                n(siteCode),
                n(detail.getLocationCode()),
                orderNumberToSend,
                n(detail.getProdCode()),
                "All",
                n(pickUser),
                pickedQty,
                0
        );

        Log.d(TAG, "Sending SCAN_BARCODE: " +
                "companyCode=" + companyCode +
                ", prinCode=" + prinCode +
                ", transBatchId=" + transBatchId +
                ", jobNo=" + jobNo +
                ", siteCode=" + siteCode +
                ", locationCode=" + detail.getLocationCode() +
                ", orderNo=" + orderNumberToSend +
                ", productCode=" + detail.getProdCode() +
                ", palletId=All" +
                ", userId=" + pickUser +
                ", quantity1=" + pickedQty +
                ", quantity2=0");

        new Thread(() -> {
            try {
                ApiMessage res = repo.scanBarcodeBlocking(body);
                runOnUiThread(() -> {
                    if (res != null && res.isSuccess()) {
                        successfulPicksCount++;
                        Log.d(TAG, "Successful pick count (sku_cnt): " + successfulPicksCount);

                        completedItems = successfulPicksCount;

                        checkAndMarkLocationComplete();

                        persistProgress();

                        updateProgressUI();

                        showDebugToast(true);

                        showScanResult(true);

                        boolean allSkusPicked = (successfulPicksCount >= originalTotalLocations);
                        if (allSkusPicked) {
                            Log.d(TAG, "All locations picked (" + successfulPicksCount + "/" + totalItems + "). Navigating to summary.");
                            goNext();
                            return;
                        } else {
                            Log.d(TAG, "Pick successful. Moving to next location...");

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                moveToNextLocationAndReload();
                            }, 3000);
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showDebugToast(false);

                    Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showInitialState();
                });
            }
        }).start();
    }

    private void showDebugToast(boolean scanSuccess) {
        String status = scanSuccess ? "✅ SUCCESS" : "❌ FAILED";

        int currentPos = currentLocationIndex + 1;
        int totalLocs = (totalItems > 0) ? totalItems : (allLocations != null ? allLocations.size() : 0);

        Intent currentIntent = getIntent();
        int intentTotal = currentIntent.getIntExtra("TOTAL_ITEMS", -1);
        int intentCompleted = currentIntent.getIntExtra("COMPLETED_ITEMS", -1);
        int intentIndex = currentIntent.getIntExtra("CURRENT_LOCATION_INDEX", -1);

        int progressPct = (originalTotalLocations > 0) ? (successfulPicksCount * 100 / originalTotalLocations) : 0;

        String prevLocInfo = "N/A";
        if (allLocations != null && currentLocationIndex > 0 && currentLocationIndex - 1 < allLocations.size()) {
            Location prevLoc = allLocations.get(currentLocationIndex - 1);
            if (prevLoc != null) {
                String prevCode = n(prevLoc.getLocationCode());
                int prevQty = prevLoc.getQuantity();
                boolean prevCompleted = completedLocationCodes.contains(prevCode);
                prevLocInfo = prevCode + " | Qty:" + prevQty + " | Done:" + (prevCompleted ? "YES" : "NO");
            }
        }

        String currLocInfo = "N/A";
        if (detail != null) {
            String currCode = n(detail.getLocationCode());
            Integer currQty = detail.getQuantity();
            Integer currPda = detail.getPdaQuantity();
            int maxQty = (currQty != null) ? currQty : 0;
            int pdaQty = (currPda != null) ? currPda : 0;
            int currRemaining = maxQty - pdaQty;
            boolean currCompleted = completedLocationCodes.contains(currCode);
            currLocInfo = currCode + " | Qty:" + maxQty + " | Picked:" + pdaQty +
                    " | Remaining:" + currRemaining + " | Done:" + (currCompleted ? "YES" : "NO");
        }

        String nextLocInfo = "N/A";
        if (allLocations != null && currentLocationIndex + 1 < allLocations.size()) {
            Location nextLoc = allLocations.get(currentLocationIndex + 1);
            if (nextLoc != null) {
                String nextCode = n(nextLoc.getLocationCode());
                int nextQty = nextLoc.getQuantity();
                boolean nextCompleted = completedLocationCodes.contains(nextCode);
                nextLocInfo = nextCode + " | Qty:" + nextQty + " | Done:" + (nextCompleted ? "YES" : "NO");
            }
        } else {
            nextLocInfo = "END OF LIST";
        }

        int totalMaxQty = 0;
        int completedLocsCount = completedLocationCodes.size();

        if (allLocations != null) {
            for (Location loc : allLocations) {
                if (loc != null) {
                    int qty = loc.getQuantity();
                    totalMaxQty += qty;
                }
            }
        }

        boolean isLastLocation = (currentLocationIndex >= totalItems - 1);
        boolean shouldExit = (successfulPicksCount >= originalTotalLocations);

        String debugMsg = "";



        Log.d(TAG, debugMsg.replace("\n", " | "));
    }


    private void checkAndMarkLocationComplete() {
        if (detail == null) return;

        String currentLoc = n(detail.getLocationCode());
        if (currentLoc.isEmpty()) return;

        if (completedLocationCodes.contains(currentLoc)) {
            return;
        }

        Integer apiMax = detail.getQuantity();
        Integer apiPda = detail.getPdaQuantity();

        int maxQty = (apiMax != null) ? apiMax : 0;
        int pdaQty = (apiPda != null) ? apiPda : 0;
        int remainingQty = maxQty - pdaQty;

        if (remainingQty <= 0) {
            Log.d(TAG, "Location " + currentLoc + " is now fully completed. Marking as done.");

            completedLocationCodes.add(currentLoc);
            saveCompletedLocations();

            Log.d(TAG, "✅ Denominator remains locked at: " + batchTotalItems);
        }
    }


    private void loadLocationDetail(Location loc, String toastMessage) {
        final String nextSite     = n(loc.getSiteCode());
        final String nextLocCode  = n(loc.getLocationCode());
        final String nextBatch    = n(loc.getTransBatchId());
        final String nextCompany  = n(loc.getCompanyCode());
        final String nextPrin     = n(loc.getPrinCode());
        final String nextJob      = n(loc.getJobNo());
        final String nextProd     = n(loc.getProdCode());

        new Thread(() -> {
            try {
                ConsolidatedPickDetail nextDetail = repo.getConsolidatedPickDetailBlocking(
                        nextCompany, nextPrin, nextBatch, nextJob, nextSite, nextLocCode, nextProd
                );

                runOnUiThread(() -> {
                    companyCode  = nextCompany;
                    prinCode     = nextPrin;
                    transBatchId = nextBatch;
                    jobNo        = nextJob;
                    siteCode     = nextSite;
                    locationCode = nextLocCode;
                    prodCode     = nextProd;

                    detail = nextDetail;
                    bindUi();
                    updateProgressUI();
                    checkAndMarkLocationComplete();

                    if (toastMessage != null && !toastMessage.isEmpty()) {
                        Toast.makeText(this, toastMessage + ": " + locationCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Failed to load location detail", e);
                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Failed to load location: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }


    private String extractLocationFromScan(String raw) {
        if (raw == null) return "";

        String trimmed = raw.trim();

        if (trimmed.length() >= 8) {
            String locPart = trimmed.substring(2);
            locPart = locPart.replaceAll("\\D", "");
            return locPart;
        }

        return trimmed.replaceAll("\\D", "");
    }





    private void maybeCompleteUsingServer() {
        final String loginId = n(pickUser);
        final String batchId = n(transBatchId);

        new Thread(() -> {
            boolean allDone = false;
            String errMsg = null;
            try {
                allDone = repo.tryCompleteConsolidatedPicking(loginId, batchId);
            } catch (IOException e) {
                errMsg = e.getMessage();
            }

            final boolean finalAllDone = allDone;
            final String finalErr = errMsg;
            runOnUiThread(() -> {
                if (finalErr != null && !finalErr.toLowerCase().contains("still need to be picked")) {
                    Toast.makeText(this, finalErr, Toast.LENGTH_LONG).show();
                }
                if (finalAllDone) {
                    navigatedAway = true;
                    goNext();
                }
            });
        }).start();
    }
    private void goNext() {
        if (detail == null) {
            Log.e(TAG, "goNext: detail is null, cannot continue");
            return;
        }

        String safeOrderNo = n(orderNo);

        if (safeOrderNo.isEmpty()) {
            String detailOrderNo = n(detail.getOrderNo());
            if (!detailOrderNo.isEmpty()) {
                safeOrderNo = detailOrderNo;
            }
        }

        Log.d(TAG, "goNext() - Passing to OrdecompleteconsolidatedActivity: " +
                "orderNo=" + safeOrderNo +
                ", jobNo=" + jobNo +
                ", user=" + pickUser);

        int totalForSummary = (batchTotalItems > 0) ? batchTotalItems : totalItems;

        Intent i = new Intent(this, OrdecompleteconsolidatedActivity.class);
        i.putExtra("PICK_DETAIL", detail);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("PRIN_CODE", prinCode);
        i.putExtra("PICK_USER", pickUser);
        i.putExtra("TOTAL_ITEMS", totalItems);
        i.putExtra("COMPLETED_ITEMS", completedItems);
        i.putExtra("JOB_NO", jobNo);

        i.putExtra("BATCH_ID",     n(transBatchId));
        i.putExtra("LOGIN_ID",     n(loginId));
        i.putExtra("TRANS_BATCH_ID", n(transBatchId));

        i.putExtra("ORDER_NO", safeOrderNo);

        i.putExtra("SUCCESSFUL_PICKS_COUNT", successfulPicksCount);
        i.putExtra("TOTAL_QUANTITY_SUM", getIntent().getIntExtra("TOTAL_QUANTITY_SUM", 0));

        startActivity(i);
    }


    private static String n(String s) { return s == null ? "" : s; }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_LOCATION) {
            if (resultCode == RESULT_OK && data != null) {
                siteCode     = n(data.getStringExtra("SITE_CODE"));
                locationCode = n(data.getStringExtra("LOCATION_CODE"));
                transBatchId = n(data.getStringExtra("TRANS_BATCH_ID"));
                companyCode  = n(data.getStringExtra("COMPANY_CODE"));
                prinCode     = n(data.getStringExtra("PRIN_CODE"));
                jobNo        = n(data.getStringExtra("JOB_NO"));
                prodCode     = n(data.getStringExtra("PROD_CODE"));

                totalItems = data.getIntExtra("TOTAL_ITEMS", totalItems);
                currentLocationIndex = data.getIntExtra("CURRENT_LOCATION_INDEX", currentLocationIndex);

                Log.d(TAG, "Returned from Jump To. totalItems=" + totalItems +
                        ", originalTotal (locked)=" + originalTotalLocations);

                persistProgress();
                updateProgressUI();

                stopCameraIfAny();
                showInitialState();

                new Thread(() -> {
                    try {
                        detail = repo.getConsolidatedPickDetailBlocking(
                                n(companyCode), n(prinCode), n(transBatchId),
                                n(jobNo), n(siteCode), n(locationCode), n(prodCode)
                        );
                        runOnUiThread(() -> {
                            bindUi();
                            checkAndMarkLocationComplete();
                            updateProgressUI();
                            Toast.makeText(this, "Loaded location: " + locationCode, Toast.LENGTH_SHORT).show();
                        });
                    } catch (IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Detail load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                }).start();
            }
        }
    }

    private void updateProgressUI() {
        if (progressBar == null) return;

        int skuCount = successfulPicksCount;
        int originalTotal = originalTotalLocations;

        if (originalTotal > 0) {
            int percentage = (int) ((skuCount * 100.0) / originalTotal);
            progressBar.setProgress(percentage);
            Log.d(TAG, "Progress Bar: SKU " + skuCount + "/" + originalTotal + " = " + percentage + "%");
        } else {
            progressBar.setProgress(0);
        }

        if (progressText != null) {
            int numerator = successfulPicksCount;
            int denominator = originalTotalLocations > 0 ? originalTotalLocations : totalItems;
            progressText.setText(numerator + "/" + denominator);
            Log.d(TAG, "Progress Text: Picks " + numerator + "/" + denominator);
        }


    }





    private void persistProgress() {
        String key = n(transBatchId);
        if (key.isEmpty()) return;
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .putInt(PREF_CONS_TOTAL + key, totalItems)
                .putInt(PREF_CONS_DONE  + key, completedItems)
                .putInt(PREF_CONS_CURRENT_INDEX + key, currentLocationIndex)
                .putInt(PREF_CONS_BATCH_TOTAL + key, originalTotalLocations)
                .apply();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraAndScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan.", Toast.LENGTH_LONG).show();
                showInitialState();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        stopCameraIfAny();
    }

    @Override
    protected void onResume() {
        super.onResume();

        com.example.Pickbyvision.voice.VoiceCommandCenter.initConsolidatedJob(this);



        if (startApiHitInThisSession) {
            Log.d(TAG, "Returning to activity - will check status and resume if needed");
            startApiHitInThisSession = false;
            handleStartOrResumeFlow();
        }

        if (timerRunnable != null) {
            timerHandler.post(timerRunnable);
        } else {
            startTimer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) cameraExecutor.shutdown();
        if (barcodeScanner != null) barcodeScanner.close();
    }

    private void startJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }
    private void pauseJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }
    private void resumeJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }

    private static boolean isBlankDate(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.equals("null") || lower.equals("n/a") || lower.equals("na") || lower.equals("{}")) return true;
        if (t.equals("--") || t.equals("0000-00-00")) return true;
        return false;
    }

    private void bindDates(ConsolidatedPickDetail d) {
        if (d == null) {
            setVisible(manufacturingText, false);
            setVisible(expirationText, false);
            setVisible(manufacturingLabelText, false);
            setVisible(expirationLabelText, false);
            return;
        }

        String mfgRaw = d.getMfgDate();
        String expRaw = d.getExpDate();

        boolean hasMfg = !isBlankDate(mfgRaw);
        boolean hasExp = !isBlankDate(expRaw);

        if (hasMfg) {
            String formatted = formatAnyDate(mfgRaw);
            if (!isBlankDate(formatted)) manufacturingText.setText(formatted);
            else manufacturingText.setText(mfgRaw);
        } else {
            manufacturingText.setText("");
        }

        if (hasExp) {
            String formatted = formatAnyDate(expRaw);
            if (!isBlankDate(formatted)) expirationText.setText(formatted);
            else expirationText.setText(expRaw);
        } else {
            expirationText.setText("");
        }

        setVisible(manufacturingText, hasMfg);
        setVisible(manufacturingLabelText, hasMfg);
        setVisible(expirationText, hasExp);
        setVisible(expirationLabelText, hasExp);
    }

    /** Normalize barcode/location strings for robust comparison. */
    private String normalizeForCompare(@Nullable String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        t = t.replaceAll("[\\p{C}\\s\\-\\_]", "");
        t = t.replaceAll("[^a-z0-9]", "");
        return t;
    }

    private String formatAnyDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return s;
        String[] fmts = new String[]{
                "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };
        for (String f : fmts) {
            try {
                java.util.Date dt = new java.text.SimpleDateFormat(f, java.util.Locale.US).parse(s);
                if (dt != null) return OUT_FMT.format(dt);
            } catch (Exception ignore) {}
        }
        return s;
    }

    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        final boolean shortVisible = shortQuantityInputContainer != null
                && shortQuantityInputContainer.getVisibility() == View.VISIBLE;

        boolean handled = com.example.Pickbyvision.voice.VoiceCommandCenter.handleKeyDownConsolidatedJob(
                keyCode,
                new com.example.Pickbyvision.voice.VoiceCommandCenter.Actions() {

                    @Override public void onScrollUp() {}

                    @Override public void onScrollDown() { startScrollingDown(); }

                    @Override public void onStop() { stopScrolling(); }

                    @Override public void onScan() {
                        if (btnScanBarcode != null) btnScanBarcode.performClick();
                    }

                    @Override public void onShort() { showShortQuantityInput(); }

                    @Override public void onJumpTo() {
                        if (btnJumpTo != null) btnJumpTo.performClick();
                    }

                    @Override public void onNextOrder() {
                        goNext();
                    }

                    @Override public void onLogout() {
                        pauseConsolidatedJobPicking();
                        Intent intent = new Intent(ConsolidatedJobActivity.this, Barcodescanner.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override public void onShortRemove() { if (shortVisible && btnBackspaceDigit != null) btnBackspaceDigit.performClick(); }
                    @Override public void onShortAdd()    { if (shortVisible && btnAddDigit != null) btnAddDigit.performClick(); }
                    @Override public void onShortBack()   { if (shortVisible && btnCancelShortInput != null) btnCancelShortInput.performClick(); }
                    @Override public void onShortNext()   { if (shortVisible && btnConfirmShortInput != null) btnConfirmShortInput.performClick(); }

                    @Override public void onDigit(int d) {
                        if (!shortVisible) return;
                        if (d == 0 && enteredQuantityBuilder.length() == 0) {
                            if (tvEnteredQuantity != null) tvEnteredQuantity.setText("0");
                            return;
                        }
                        final int maxDigits = 6;
                        if (enteredQuantityBuilder.length() < maxDigits) {
                            enteredQuantityBuilder.append(d);
                            if (tvEnteredQuantity != null) tvEnteredQuantity.setText(enteredQuantityBuilder.toString());
                        } else {
                            Toast.makeText(ConsolidatedJobActivity.this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onNext() {}
                    @Override public void onBack() {}
                    @Override public void onSelect() {}
                    @Override public void onInbound() {}
                    @Override public void onOutbound() {}
                    @Override public void onInventory() {}
                    @Override public void onIndividual() {}
                    @Override public void onConsolidated() {}
                }
        );

        if (handled) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void stopScrolling() {
        if (!isScrolling) return;

        isScrolling = false;
        scrollingHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Stopped continuous scrolling");
        Toast.makeText(this, "Scrolling stopped", Toast.LENGTH_SHORT).show();
    }

    private void startScrollingDown() {
        if (isScrolling) return;

        isScrolling = true;
        Log.d(TAG, "Starting continuous scrolling down");

        scrollDownContinuously();
    }

    private void scrollDownContinuously() {
        if (!isScrolling) return;

        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));

        scrollingHandler.postDelayed(this::scrollDownContinuously, SCROLL_DELAY);
    }
    private static class ConsStatus {
        String status;
    }


    @Override
    public void onBackPressed() {



        Log.d(TAG, "Back pressed – blocked (no navigation)");
        Toast.makeText(this, "Back disabled on this screen", Toast.LENGTH_SHORT).show();

    }


}