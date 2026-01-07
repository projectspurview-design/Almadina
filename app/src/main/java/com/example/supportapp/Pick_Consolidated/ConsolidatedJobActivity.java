package com.example.supportapp.Pick_Consolidated;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

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

import com.example.supportapp.LogoutManager;
import com.example.supportapp.Pick_Consolidated.model.ApiMessage;
import com.example.supportapp.Pick_Consolidated.model.ConsolidatedPickDetail;
import com.example.supportapp.Pick_Consolidated.model.Location;
import com.example.supportapp.Pick_Consolidated.model.ScanBarcodeRequest;
import com.example.supportapp.Pick_Consolidated.repo.ConsolidatedRepository;
import com.example.supportapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import android.os.Handler;
import android.os.Looper;
import java.util.Locale;
// add imports
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConsolidatedJobActivity extends AppCompatActivity {



    private static final String TAG = "ConsolidatedJob";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 4441;

    // ==== CUMULATIVE TOTALS (per-batch) ====
    private static final String PREF_CONS_PDA_SUM = "cons_pda_sum_";
    private static final String PREF_CONS_MAX_SUM = "cons_max_sum_";
    // we'll mark each location counted once: cons_cnt_<batch>_<site>_<loc>_<job>_<prod>
    private static final String PREF_CONS_CNT_PREFIX = "cons_cnt_";

    private int cumulativePdaSum = 0;   // sum(PDA_QUANTITY) for the whole batch
    private int cumulativeMaxSum = 0;   // sum(MAX_QUANTITY)  (expected), counted once/location
    private int accumulatedPdaQuantity = 0;
    private int accumulatedMaxQuantity = 0;
    // Jump To request code
    private static final int REQ_PICK_LOCATION = 3021;

//    @Nullable private String scannedOrderNo = null;
//    @Nullable private String scannedLocationCode = null;
//    @Nullable private String scannedProductCode = null;
//    @Nullable private Integer scannedQ1 = null, scannedQ2 = null;
//    @Nullable private String scannedPalletId = null;

    @Nullable private String scannedLocationCode = null;

    // Optional hardware keys for Short UI
    private static final int KEYCODE_SHORT_REMOVE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_SHORT_ADD    = KeyEvent.KEYCODE_F6;
    private static final int KEYCODE_SHORT_BACK   = KeyEvent.KEYCODE_F7;
    private static final int KEYCODE_SHORT_NEXT   = KeyEvent.KEYCODE_F8;
    private static final int KEYCODE_DIGIT_0 = KeyEvent.KEYCODE_0;
    private static final int KEYCODE_DIGIT_1 = KeyEvent.KEYCODE_1;

    private ImageView productImageView;
    private OkHttpClient httpClient;
    private ExecutorService imageExecutor;
    private VuzixSpeechClient vuzixSpeechClient;

    private Handler mainHandler;
    private static final int KEYCODE_DIGIT_2 = KeyEvent.KEYCODE_2;
    private static final int KEYCODE_DIGIT_3 = KeyEvent.KEYCODE_3;
    private static final int KEYCODE_DIGIT_4 = KeyEvent.KEYCODE_4;
    private static final int KEYCODE_DIGIT_5 = KeyEvent.KEYCODE_5;
    private static final int KEYCODE_DIGIT_6 = KeyEvent.KEYCODE_6;
    private static final int KEYCODE_DIGIT_7 = KeyEvent.KEYCODE_7;
    private static final int KEYCODE_DIGIT_8 = KeyEvent.KEYCODE_8;
    private static final int KEYCODE_DIGIT_9 = KeyEvent.KEYCODE_9;
    // Voice Command Keycodes
    private static final int KEYCODE_SCAN_BARCODE = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_SHORT        = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_JUMP_TO      = KeyEvent.KEYCODE_F3;

    private final ConsolidatedRepository repo = new ConsolidatedRepository();

    // Info UI
    private TextView tvLocation, tvQuantity, tvProduct, tvDesc;
    private MaterialButton btnJumpTo;

    // NEW: Mfg/Exp Date UI
    private TextView manufacturingText, expirationText;
    private TextView manufacturingLabelText, expirationLabelText;
    private static final java.text.SimpleDateFormat OUT_FMT =
            new java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.getDefault());

    // Scan UI
    private PreviewView cameraPreview;
    private TextView skuStatusText;
    private View orderInfoContainer;
    private View menuLayout;
    private Button btnScanBarcode;

    // SHORT UI
    private View shortQuantityInputContainer;
    private NumberPicker npSingleDigitInput;
    private TextView tvEnteredQuantity;
    private Button btnAddDigit, btnBackspaceDigit, btnCancelShortInput, btnConfirmShortInput;

    // Progress UI
    private ProgressBar progressBar;
    private TextView progressText;
    private int totalItems = 0;
    private int completedItems = 0;



    private static final String PREF_CONS_TOTAL = "cons_total_";
    private static final String PREF_CONS_DONE  = "cons_done_";

    // CameraX / ML Kit
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    // Inputs from previous screen
    private String companyCode, prinCode, transBatchId, jobNo, siteCode, locationCode, prodCode, pickUser, orderNo;

    // The detail we show/confirm
    private ConsolidatedPickDetail detail;

    // For the SCAN_BARCODE payload
    private int pickedQty = 0;
    private String lastScanned = "";  // keep for pallet/product if needed

    // builder for short qty digits
    private final StringBuilder enteredQuantityBuilder = new StringBuilder();
    private static final int KEYCODE_SHOW_SUMMARY = KeyEvent.KEYCODE_F9;
    private static final int KEYCODE_SKIP_NEXT = KeyEvent.KEYCODE_F10;

    // Timer
    private long startTime = 0L;
    private long elapsedTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private TextView timerTextView;
    private final AtomicBoolean orderNoLoaded = new AtomicBoolean(false);


    // Protect against double navigation
    private volatile boolean navigatedAway = false;

    // Voice keycodes for Short Input UI
    private static final int KEYCODE_VOICE_ADD    = KeyEvent.KEYCODE_F4;
    private static final int KEYCODE_VOICE_REMOVE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_VOICE_BACK   = KeyEvent.KEYCODE_F6;
    private static final int KEYCODE_VOICE_NEXT   = KeyEvent.KEYCODE_F7;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consolidated_job);


        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);



        productImageView = findViewById(R.id.productImageView);
        httpClient = new OkHttpClient();
        imageExecutor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());

        // Bind info UI
        tvLocation = findViewById(R.id.locationText);
        tvQuantity = findViewById(R.id.quantityText);
        tvProduct  = findViewById(R.id.productText);
        tvDesc     = findViewById(R.id.descriptionText);
        btnJumpTo  = findViewById(R.id.btnJumpTo);

        // Bind NEW date UI
        manufacturingText      = findViewById(R.id.manufacturingText);
        expirationText         = findViewById(R.id.expirationText);
        manufacturingLabelText = findViewById(R.id.manufacturingLabelText);
        expirationLabelText    = findViewById(R.id.expirationLabelText);

        // Start hidden
        setVisible(manufacturingText, false);
        setVisible(expirationText, false);
        setVisible(manufacturingLabelText, false);
        setVisible(expirationLabelText, false);

        // Bind scan UI
        cameraPreview      = findViewById(R.id.camera_preview);
        skuStatusText      = findViewById(R.id.skuStatusText);
        orderInfoContainer = findViewById(R.id.orderInfoContainer);
        menuLayout         = findViewById(R.id.menuLayout);
        btnScanBarcode     = findViewById(R.id.btnScanBarcode);
        btnScanBarcode.setEnabled(false); // wait until detail+ORDER_NO is ready


        // Bind SHORT UI
        shortQuantityInputContainer = findViewById(R.id.shortQuantityInputContainer);
        npSingleDigitInput          = findViewById(R.id.npSingleDigitInput);
        tvEnteredQuantity           = findViewById(R.id.tvEnteredQuantity);
        btnAddDigit                 = findViewById(R.id.btnAddDigit);
        btnBackspaceDigit           = findViewById(R.id.btnBackspaceDigit);
        btnCancelShortInput         = findViewById(R.id.btnCancelShortInput);
        btnConfirmShortInput        = findViewById(R.id.btnConfirmShortInput);


        timerTextView = findViewById(R.id.timerTextView);
        startTimer();

        // Progress UI
        progressBar  = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        // Setup SHORT controls
        setupNumberPicker();
        setupShortButtonHandlers();

        // Retrieve Intent extras
        orderNo     = getIntent().getStringExtra("ORDER_NO");
        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        prinCode    = getIntent().getStringExtra("PRIN_CODE");
        transBatchId= getIntent().getStringExtra("TRANS_BATCH_ID");
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String batchKey = n(transBatchId);
        jobNo       = getIntent().getStringExtra("JOB_NO");
        siteCode    = getIntent().getStringExtra("SITE_CODE");
        locationCode= getIntent().getStringExtra("LOCATION_CODE");
        prodCode    = getIntent().getStringExtra("PROD_CODE");
        pickUser    = getIntent().getStringExtra("PICK_USER");

        cumulativePdaSum = prefs.getInt(PREF_CONS_PDA_SUM + batchKey, 0);
        cumulativeMaxSum = prefs.getInt(PREF_CONS_MAX_SUM + batchKey, 0);

        Button logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> LogoutManager.performLogout(this));
        }
        if (orderNo == null || orderNo.isEmpty()) {
            fetchOrderNoFromApi();
        } else {
            orderNoLoaded.set(true);
        }
        // Add this in onCreate after retrieving intent extras:
        accumulatedPdaQuantity = 0;
        accumulatedMaxQuantity = 0;




        // Jump To
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
            startActivityForResult(i, REQ_PICK_LOCATION);
        });

        // Progress state (Intent → SharedPreferences fallback)
        // Determine total items properly
//        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
//        String batchKey = n(transBatchId);

// Read from Intent or SharedPreferences
        totalItems = getIntent().getIntExtra("TOTAL_ITEMS",
                prefs.getInt(PREF_CONS_TOTAL + batchKey, 0));
        completedItems = getIntent().getIntExtra("COMPLETED_ITEMS",
                prefs.getInt(PREF_CONS_DONE + batchKey, 0));

// ✅ If total is still 0, try fetching it from API later
        if (totalItems == 0) {
            new Thread(() -> {
                try {
                    List<Location> locations = repo.getConsolidatedPicklistBlocking(
                            n(companyCode), n(prinCode), n(transBatchId), n(pickUser)
                    );
                    totalItems = locations.size();
                    persistProgress();
                    runOnUiThread(this::updateProgressUI);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load total items: " + e.getMessage());
                }
            }).start();
        } else {
            // Immediate update if total already known
            persistProgress();
            updateProgressUI();
        }


        // Load detail
        new Thread(() -> {
            try {
                detail = repo.getConsolidatedPickDetailBlocking(
                        n(companyCode), n(prinCode), n(transBatchId), n(jobNo), n(siteCode), n(locationCode), n(prodCode)
                );
                runOnUiThread(this::bindUi);
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Detail load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();

        // Scan button
        btnScanBarcode.setOnClickListener(v -> {
            if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, "Confirm/close short quantity first.", Toast.LENGTH_SHORT).show();
                return;
            }
            startOrAskCamera();
        });

        // CameraX + ML Kit
        cameraExecutor = Executors.newSingleThreadExecutor();
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // Menu Short button
        View btnShort = findViewById(R.id.btnShort);
        if (btnShort != null) {
            btnShort.setOnClickListener(v -> showShortQuantityInput());
        }



        showInitialState();
        setupVoiceCommands();

    }
    private void setupVoiceCommands() {
        try {
            vuzixSpeechClient = new VuzixSpeechClient(this);

            // Clear any previous mappings
            vuzixSpeechClient.deletePhrase("Scan");
            vuzixSpeechClient.deletePhrase("Scan Barcode");
            vuzixSpeechClient.deletePhrase("Short");
            vuzixSpeechClient.deletePhrase("Jump");
            vuzixSpeechClient.deletePhrase("Jump To");
            // Voice commands for Short Quantity UI
            vuzixSpeechClient.insertKeycodePhrase("Add", KEYCODE_VOICE_ADD);
            vuzixSpeechClient.insertKeycodePhrase("Increase", KEYCODE_VOICE_ADD);

            vuzixSpeechClient.insertKeycodePhrase("Remove", KEYCODE_VOICE_REMOVE);
            vuzixSpeechClient.insertKeycodePhrase("Delete", KEYCODE_VOICE_REMOVE);

            vuzixSpeechClient.insertKeycodePhrase("Back", KEYCODE_VOICE_BACK);
            vuzixSpeechClient.insertKeycodePhrase("Cancel", KEYCODE_VOICE_BACK);

            vuzixSpeechClient.insertKeycodePhrase("Next", KEYCODE_VOICE_NEXT);
            vuzixSpeechClient.insertKeycodePhrase("Confirm", KEYCODE_VOICE_NEXT);



            // Register new voice commands mapped to keycodes
            vuzixSpeechClient.insertKeycodePhrase("Scan", KEYCODE_SCAN_BARCODE);
            vuzixSpeechClient.insertKeycodePhrase("Scan Barcode", KEYCODE_SCAN_BARCODE);

            vuzixSpeechClient.insertKeycodePhrase("Short", KEYCODE_SHORT);
            vuzixSpeechClient.insertKeycodePhrase("Add Short", KEYCODE_SHORT);

            vuzixSpeechClient.insertKeycodePhrase("Jump", KEYCODE_JUMP_TO);
            vuzixSpeechClient.insertKeycodePhrase("Jump To", KEYCODE_JUMP_TO);

            vuzixSpeechClient.insertKeycodePhrase("Add", KEYCODE_VOICE_ADD);
            vuzixSpeechClient.insertKeycodePhrase("Increase", KEYCODE_VOICE_ADD);
            vuzixSpeechClient.insertKeycodePhrase("Remove", KEYCODE_VOICE_REMOVE);
            vuzixSpeechClient.insertKeycodePhrase("Delete", KEYCODE_VOICE_REMOVE);
            vuzixSpeechClient.insertKeycodePhrase("Back", KEYCODE_VOICE_BACK);
            vuzixSpeechClient.insertKeycodePhrase("Cancel", KEYCODE_VOICE_BACK);
            vuzixSpeechClient.insertKeycodePhrase("Next", KEYCODE_VOICE_NEXT);
            vuzixSpeechClient.insertKeycodePhrase("Confirm", KEYCODE_VOICE_NEXT);

            Log.i(TAG, "Voice commands registered successfully");
            Toast.makeText(this, "Voice commands ready", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient", e);
            Toast.makeText(this, "Voice setup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
    private String itemKey() {
        return (PREF_CONS_CNT_PREFIX
                + n(transBatchId) + "_"
                + n(siteCode)     + "_"
                + n(locationCode) + "_"
                + n(jobNo)        + "_"
                + n(prodCode)).replaceAll("\\s+", "");
    }

    private boolean wasCounted(String key) {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean(key, false);
    }

    private void markCounted(String key) {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putBoolean(key, true).apply();
    }

    private void persistSums() {
        String batchKey = n(transBatchId);
        if (batchKey.isEmpty()) return;
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putInt(PREF_CONS_PDA_SUM + batchKey, cumulativePdaSum)
                .putInt(PREF_CONS_MAX_SUM + batchKey, cumulativeMaxSum)
                .apply();
    }

    private void loadImageFromAws(ImageView imageView, String awsPath) {
        imageView.setImageResource(android.R.drawable.ic_menu_gallery); // placeholder

        if (awsPath == null || awsPath.trim().isEmpty()) {
            Log.d(TAG, "No AWS path provided for image");
            return;
        }

        imageExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(awsPath)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "Failed to load image from: " + awsPath, e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try (InputStream inputStream = response.body().byteStream()) {
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                if (bitmap != null) {
                                    mainHandler.post(() -> {
                                        imageView.setImageBitmap(bitmap);
                                        Log.d(TAG, "Successfully loaded image from: " + awsPath);
                                    });
                                }
                            }
                        } else {
                            Log.e(TAG, "HTTP error " + response.code() + " from: " + awsPath);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error creating image request for: " + awsPath, e);
            }
        });
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
        tvLocation.setText(n(detail.getLocationCode()));

        tvDesc.setText(n(detail.getProdName()));
        tvProduct.setText(n(detail.getProdCode()));
        tvLocation.setText(n(detail.getLocationCode()));
        tvQuantity.setText(String.valueOf(pickedQty));

        String awsPath = detail.getAwsPath(); // make sure your model has this
        if (productImageView != null) {
            loadImageFromAws(productImageView, awsPath);
        }

        Integer totalQty = detail.getQuantity();
        Integer alreadyPickedQty = detail.getPickQty();

// Calculate remaining quantity
        int remaining = 0;
        if (totalQty != null) {
            remaining = totalQty - (alreadyPickedQty != null ? alreadyPickedQty : 0);
        }

// Set pickedQty to the remaining quantity
        pickedQty = remaining;
        tvQuantity.setText(String.valueOf(remaining));

        Log.i(TAG, "Total Qty: " + totalQty + ", Already Picked: " + alreadyPickedQty + ", Remaining: " + remaining);

        // Pull ORDER_NO from detail
        String ord = n(detail.getOrderNo());
        if (!ord.isEmpty()) {
            orderNo = ord;
            orderNoLoaded.set(true);
        }

        updateScanEnabled();   // enable only when ready
        bindDates(detail);
    }

    // ===== SHORT: setup & handlers =====
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
    private void fetchOrderNoFromApi() {
        new Thread(() -> {
            try {
                List<Location> locations = repo.getConsolidatedPicklistBlocking(
                        n(companyCode), n(prinCode), n(transBatchId), n(pickUser)
                );

                // Find the current location and get its orderNo
                for (Location loc : locations) {
                    if (n(locationCode).equals(n(loc.getLocationCode())) &&
                            n(jobNo).equals(n(loc.getJobNo()))) {
                            orderNo = n(loc.getOrderNo());
                        Log.i(TAG, "Fetched orderNo from API: " + orderNo);

                        if (orderNo != null && !orderNo.isEmpty()) {
                            orderNoLoaded.set(true);
                            runOnUiThread(() -> {
                                btnScanBarcode.setEnabled(true);
                                Toast.makeText(ConsolidatedJobActivity.this,
                                        "Ready to scan", Toast.LENGTH_SHORT).show();
                            });
                        }
                        break;
                    }
                }


            } catch (IOException e) {
                Log.e(TAG, "Failed to fetch orderNo: " + e.getMessage());
                runOnUiThread(() -> {
                    btnScanBarcode.setEnabled(false);
                    Toast.makeText(ConsolidatedJobActivity.this,
                            "Failed to load order: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
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

    // Extended Short — "direct set" quantity
    private void handleConfirmShortQuantity() {
        if (detail == null) {
            Toast.makeText(this, "Detail not loaded yet.", Toast.LENGTH_LONG).show();
            handleCancelShortQuantity();
            return;
        }
        if (enteredQuantityBuilder.length() == 0 || (enteredQuantityBuilder.length() == 1 && enteredQuantityBuilder.charAt(0) == '0')) {
            Toast.makeText(this, "Please enter a quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        int enteredQty;
        try {
            enteredQty = Integer.parseInt(enteredQuantityBuilder.toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredQty <= 0) {
            Toast.makeText(this, "Quantity must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer original = detail.getQuantity();
        if (original != null && original > 0 && enteredQty > original) {
            Toast.makeText(this,
                    "Entered qty (" + enteredQty + ") is more than expected (" + original + "). Proceeding anyway.",
                    Toast.LENGTH_LONG).show();
        }

        pickedQty = enteredQty;
        tvQuantity.setText(String.valueOf(pickedQty));
        Toast.makeText(this, "Quantity set to: " + pickedQty + ". Scan barcode to confirm pick.", Toast.LENGTH_LONG).show();

        hideShortQuantityInputAndShowMenu();
    }

    private void handleCancelShortQuantity() {
        enteredQuantityBuilder.setLength(0);
        if (tvEnteredQuantity != null) tvEnteredQuantity.setText("0");
        hideShortQuantityInputAndShowMenu();
    }

    // ===== UI states =====
    private void showInitialState() {
        setVisible(cameraPreview, false);
        setVisible(orderInfoContainer, true);
        setVisible(menuLayout, true);
        setVisible(skuStatusText, false);
        updateScanEnabled();   // <-- instead of forcing enabled=true
        if (btnScanBarcode != null) btnScanBarcode.setEnabled(true);
        updateProgressUI();
    }
    private void updateScanEnabled() {
        boolean ready = (detail != null) && !n(orderNo).isEmpty();
        if (btnScanBarcode != null) btnScanBarcode.setEnabled(ready);
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
        }, 1500);
    }

    private void setVisible(View v, boolean show) {
        if (v != null) v.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ===== Camera/Scan =====
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

    /**
     * NEW: Modified handleScan - let server handle all validation
     */
    // ===== handleScan: parse QR first, then send =====
    private void handleScan(String scannedValue) {
        if (scannedValue == null) {
            Toast.makeText(this, "No barcode detected.", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        lastScanned = scannedValue;

        // Extract only locationCode from QR
        extractFromQr(scannedValue);

        // Use scanned location if available
        if (scannedLocationCode != null && !scannedLocationCode.isEmpty()) {
            locationCode = scannedLocationCode;
            Log.i(TAG, "QR scanned locationCode: " + locationCode);
        } else {
            Toast.makeText(this, "No location code found in QR", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        // pickedQty comes from user input (quantityText or SHORT UI)
        // orderNo, prodCode, etc. come from Intent/previous responses

        sendScanToServer();
    }    // ===== QR parsers: JSON first, then key=value pairs =====

    private void extractFromQr(@NonNull String raw) {
        // reset - only extract locationCode now
        scannedLocationCode = null;

        // 1) Try JSON first
        try {
            JSONObject jo = new JSONObject(raw);
            scannedLocationCode = optNonEmpty(jo, "locationCode");
            return;
        } catch (JSONException ignored) {
            // not JSON; fall through
        }

        // 2) Fallback: key=value pairs
        Map<String, String> kv = parseKv(raw);
        scannedLocationCode = firstNonEmpty(kv, "locationCode", "LOC", "location", "loc");
    }
    private static String optNonEmpty(JSONObject jo, String key) {
        if (!jo.has(key) || jo.isNull(key)) {
            return null;
        }

        Object val = jo.opt(key);
        if (val == null) {
            return null;
        }

        // Handle case where palletId might be a JSON object instead of string
        if (val instanceof String) {
            String s = ((String) val).trim();
            return (s.isEmpty() || s.equals("{}")) ? null : s;
        }

        // If it's not a string (e.g., JSONObject), treat as absent
        return null;
    }

    private static Map<String, String> parseKv(String raw) {
        Map<String, String> out = new HashMap<>();
        for (String part : raw.split("[;&,\\n]")) {
            String[] kv = part.split("[:=]", 2);
            if (kv.length == 2) out.put(kv[0].trim(), kv[1].trim());
        }
        return out;
    }

    private static String firstNonEmpty(Map<String, String> m, String... keys) {
        for (String k : keys) {
            String v = m.get(k);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }
    private static String normalizePalletId(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("{}")) {
            return "All";
        }
        return value.trim();
    }


    /**
     * NEW: Enhanced scan to server with auto-progression
     */
    private void sendScanToServer() {
        if (detail == null) {
            Toast.makeText(this, "Detail not loaded yet", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        String palletId = "All";  // ✅ Always "All"
        String productCode = n(detail.getProdCode());

        ScanBarcodeRequest body = new ScanBarcodeRequest(
                n(companyCode),
                n(prinCode),
                n(transBatchId),
                n(jobNo),
                n(siteCode),
                n(detail.getLocationCode()),
                n(orderNo),
                productCode,
                palletId,
                n(pickUser),
                pickedQty  // This will now be sent as quantity1
        );

        Log.i(TAG, "Sending palletId: " + palletId);
        Log.i(TAG, "Sending pickedQty: " + pickedQty);

        new Thread(() -> {
            try {
                ApiMessage res = repo.scanBarcodeBlocking(body);
                runOnUiThread(() -> {
                    if (res != null && res.isSuccess()) {
                        // ✅ Accumulate picked quantity and expected max quantity
                        accumulatedPdaQuantity += pickedQty;
                        accumulatedMaxQuantity += (detail.getQuantity() != null ? detail.getQuantity() : 0);

                        Log.i(TAG, "=== Accumulation After Scan ===");
                        Log.i(TAG, "Just Picked: " + pickedQty);
                        Log.i(TAG, "Expected for this location: " + detail.getQuantity());
                        Log.i(TAG, "Accumulated PDA Qty (Total Picks): " + accumulatedPdaQuantity);
                        Log.i(TAG, "Accumulated Max Qty (Total Expected): " + accumulatedMaxQuantity);

                        // Update progress
                        if (totalItems > 0) {
                            completedItems = Math.min(completedItems + 1, totalItems);
                        }
                        persistProgress();
                        updateProgressUI();

                        stopCameraIfAny();

                        // ✅ Show test dialog with proper accumulated values
                        showTestSummaryDialog();

                    } else {
                        String msg = (res != null) ? res.getMessage() : "Unknown scan response";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        showInitialState();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showInitialState();
                });
            }
        }).start();
    }

    /**
     * TEST FEATURE: Show dialog with options after successful scan
     */
    private void showTestSummaryDialog() {
        // Hide current UI temporarily
        setVisible(orderInfoContainer, false);
        setVisible(menuLayout, false);

        // Create custom dialog
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("✅ Scan Successful")
                .setMessage(
                        "Picked: " + pickedQty + " (Total accumulated: " + accumulatedPdaQuantity + ")\n\n" +
                                "What would you like to do?"
                )
                .setCancelable(false);

        // Button 1: Show Summary (with accumulated totals)
        builder.setPositiveButton("📊 Show Summary", (dialog, which) -> {
            dialog.dismiss();
            navigatedAway = true;
            goNextWithAccumulated();
        });

        // Button 2: Skip to Next Location
        builder.setNegativeButton("➡️ Skip to Next", (dialog, which) -> {
            dialog.dismiss();
            // Continue to next location
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                autoProgressToNextLocation();
            }, 500);
        });

        builder.show();
    }

    /**
     * Go to summary screen with accumulated quantities
     */
    /**
     * Fetch latest status and then go to summary with all accumulated values
     */
    /**
     * Go to summary screen with accumulated quantities
     */
    private void goNextWithAccumulated() {
        if (detail == null) return;

        Intent i = new Intent(this, CompletedordersummaryconsolidatedActivity.class);

        // ✅ Required for API calls
        i.putExtra("LOGIN_ID",     n(pickUser));
        i.putExtra("ORDER_NO",     n(orderNo));
        i.putExtra("JOB_NO",       n(jobNo));
        i.putExtra("PRIN_CODE",    n(prinCode));

        // ✅ Required for forwarding to next screen
        i.putExtra("COMPANY_CODE", n(companyCode));
        i.putExtra("PICK_USER",    n(pickUser));
        i.putExtra("BATCH_ID",     n(transBatchId));

        // ✅ Accumulated quantities
        i.putExtra("PDA_QUANTITY", accumulatedPdaQuantity);
        i.putExtra("MAX_QUANTITY", accumulatedMaxQuantity);

        // Optional context
        i.putExtra("TOTAL_ITEMS", totalItems);
        i.putExtra("COMPLETED_ITEMS", completedItems);
        i.putExtra("PICK_DETAIL", detail);

        Log.i(TAG, "Going to summary with:");
        Log.i(TAG, "  PDA: " + accumulatedPdaQuantity);
        Log.i(TAG, "  MAX: " + accumulatedMaxQuantity);
        Log.i(TAG, "  ORDER_NO: " + orderNo);
        Log.i(TAG, "  JOB_NO: " + jobNo);
        Log.i(TAG, "  PRIN_CODE: " + prinCode);

        startActivity(i);
    }

    /**
     * NEW: Auto-progress to next available location
     */
    private void autoProgressToNextLocation() {
        new Thread(() -> {
            try {
                // Get updated location list from server
                List<Location> locations = repo.getConsolidatedPicklistBlocking(
                        n(companyCode), n(prinCode), n(transBatchId), n(pickUser)
                );

                // Filter to current batch and find next unpicked location
                // PICK_QTY < QUANTITY means still has items to pick
                List<Location> availableLocations = locations.stream()
                        .filter(loc -> n(transBatchId).equals(loc.getTransBatchId()))
                        .filter(loc -> loc.getPickQty() < loc.getQuantity()) // Still has items to pick
                        .filter(loc -> !n(locationCode).equals(n(loc.getLocationCode()))) // Skip current location
                        .collect(Collectors.toList());

                runOnUiThread(() -> {
                    if (availableLocations.isEmpty()) {
                        // All locations completed - try final completion
                        tryFinalCompletion();
                    } else {
                        // Auto-select next location
                        Location nextLocation = availableLocations.get(0);
                        loadLocation(nextLocation);
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    // If we can't get updated list, try completion check
                    tryFinalCompletion();
                });
            }
        }).start();
    }

    /**
     * NEW: Load a specific location automatically
     */
    private void loadLocation(Location location) {
        // Update current location data
        siteCode = n(location.getSiteCode());
        locationCode = n(location.getLocationCode());
        jobNo = n(location.getJobNo());
        prodCode = n(location.getProdCode());
        orderNo = n(location.getOrderNo());

        // Load detail for this location
        new Thread(() -> {
            try {
                detail = repo.getConsolidatedPickDetailBlocking(
                        n(companyCode), n(prinCode), n(transBatchId),
                        n(jobNo), n(siteCode), n(locationCode), n(prodCode)
                );
                runOnUiThread(() -> {
                    bindUi();
                    showInitialState();
                    Toast.makeText(this, "Next location: " + locationCode, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load next location: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // Try final completion as fallback
                    tryFinalCompletion();
                });
            }
        }).start();
    }

    /**
     * NEW: Try final completion check
     */
    private void tryFinalCompletion() {
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
                } else {
                    // Stay on current screen if completion failed
                    showInitialState();
                    Toast.makeText(this, "Continue picking remaining items", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /** LEGACY: Try to finalize with COMPLETE_CONSOLIDATED_PICKING. Navigate only if 200. */
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
                // fallback: remain on this screen; user continues picking
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
        if (detail == null) return;

        int maxQty = 0;
        if (detail.getQuantity() != null) {
            maxQty = detail.getQuantity();
        } else {
            maxQty = getIntent().getIntExtra("QUANTITY", 0);
        }

        Intent i = new Intent(this, CompletedordersummaryconsolidatedActivity.class);
        i.putExtra("PICK_DETAIL", detail);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("PRIN_CODE",  prinCode);
        i.putExtra("PICK_USER",  pickUser);
        i.putExtra("TOTAL_ITEMS", totalItems);
        i.putExtra("COMPLETED_ITEMS", completedItems);
        i.putExtra("JOB_NO", jobNo);
        i.putExtra("ORDER_NO", n(orderNo));

        // ✅ Use accumulated if available, otherwise use last pick
        i.putExtra("PDA_QUANTITY", accumulatedPdaQuantity > 0 ? accumulatedPdaQuantity : pickedQty);
        i.putExtra("MAX_QUANTITY", accumulatedMaxQuantity > 0 ? accumulatedMaxQuantity : maxQty);
        i.putExtra("LOGIN_ID",     n(pickUser));
        i.putExtra("BATCH_ID",     n(transBatchId));

        startActivity(i);
    }



    private static String n(String s) { return s == null ? "" : s; }

    // ===== Jump To result handling =====
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

                String newOrderNo = data.getStringExtra("ORDER_NO");
                if (newOrderNo != null && !newOrderNo.isEmpty()) {
                    orderNo = newOrderNo;
                    orderNoLoaded.set(true);
                } else {
                    // Fetch orderNo for the new location
                    orderNoLoaded.set(false);
                    fetchOrderNoFromApi();
                }


                int t = data.getIntExtra("TOTAL_ITEMS", 0);
                int d = data.getIntExtra("COMPLETED_ITEMS", 0);
                if (t > 0) totalItems = t;
                if (d >= 0) completedItems = d;
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
        if (progressBar == null || progressText == null) return;
        if (totalItems <= 0) {
            progressBar.setProgress(0);
            progressText.setText("0/0");
            return;
        }
        int percent = (int) ((completedItems * 100.0f) / totalItems);
        progressBar.setProgress(percent);

        if (progressText != null) {
            progressText.setText(completedItems + "/" + totalItems);
        }

    }

    private void persistProgress() {
        String key = n(transBatchId);
        if (key.isEmpty()) return;
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .putInt(PREF_CONS_TOTAL + key, totalItems)
                .putInt(PREF_CONS_DONE  + key, completedItems)
                .apply();
    }

    // ===== Permissions & lifecycle =====
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
        if (timerRunnable != null) {
            timerHandler.post(timerRunnable);
        } else {
            startTimer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (imageExecutor != null && !imageExecutor.isShutdown()) {
            imageExecutor.shutdown();
        }

        if (cameraExecutor != null && !cameraExecutor.isShutdown()) cameraExecutor.shutdown();
        if (barcodeScanner != null) barcodeScanner.close();
    }


    // LEGACY (kept to avoid breaking anything else you may call elsewhere)
    private void startJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }
    private void pauseJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }
    private void resumeJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }

    // ===== NEW: date binding helpers =====

    private boolean isBlankDate(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        // treat placeholders as blank
        if (t.equals("{}")) return true;
        if (t.equalsIgnoreCase("null")) return true;
        if (t.equalsIgnoreCase("N/A")) return true;
        if (t.equals("0000-00-00")) return true;
        return false;
    }

    private String cleanedDate(String s) {
        return isBlankDate(s) ? "" : s.trim();
    }

    private void bindDates(ConsolidatedPickDetail d) {
        if (d == null) {
            setVisible(manufacturingText, false);
            setVisible(expirationText, false);
            setVisible(manufacturingLabelText, false);
            setVisible(expirationLabelText, false);
            return;
        }

        // Use tolerant getters from the model
        String mfgRaw = safe(d.getMfgDate());
        String expRaw = safe(d.getExpDate());

        // Use the isBlankDate helper to check for empty or placeholder values
        boolean hasMfg = !isBlankDate(mfgRaw);
        boolean hasExp = !isBlankDate(expRaw);

        if (hasMfg) manufacturingText.setText(formatAnyDate(mfgRaw));
        if (hasExp) expirationText.setText(formatAnyDate(expRaw));

        // Set visibility based on whether the date is valid
        setVisible(manufacturingText, hasMfg);
        setVisible(manufacturingLabelText, hasMfg);
        setVisible(expirationText, hasExp);
        setVisible(expirationLabelText, hasExp);
    }



    private String formatAnyDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return s;

        // Handle epoch seconds/millis (10 or 13 digits)
        if (s.matches("^\\d{10,13}$")) {
            try {
                long v = Long.parseLong(s);
                if (s.length() == 10) v *= 1000L; // seconds → millis
                return OUT_FMT.format(new java.util.Date(v));
            } catch (Exception ignore) {}
        }

        String[] fmts = new String[]{
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "MM-dd-yyyy",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };
        for (String f : fmts) {
            try {
                java.util.Date dt = new java.text.SimpleDateFormat(f, java.util.Locale.US).parse(s);
                if (dt != null) return OUT_FMT.format(dt);
            } catch (Exception ignore) {}
        }
        // As a last resort, if it's already something like "2025-09-01", show as-is
        return s;
    }

    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }

    // Optional: keypad hotkeys for Short UI (unchanged)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown received: keyCode=" + keyCode + ", event=" + event);

        // Handle voice commands FIRST - regardless of UI state
        switch (keyCode) {
            case KEYCODE_SCAN_BARCODE:
                if (btnScanBarcode != null && btnScanBarcode.isEnabled()) {
                    btnScanBarcode.performClick();
                    Toast.makeText(this, "Scan command activated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Scan not available yet", Toast.LENGTH_SHORT).show();
                }
                return true;

            case KEYCODE_SHORT:
                View btnShort = findViewById(R.id.btnShort);
                if (btnShort != null && btnShort.getVisibility() == View.VISIBLE) {
                    btnShort.performClick();
                    Toast.makeText(this, "Short command activated", Toast.LENGTH_SHORT).show();
                }
                return true;

            case KEYCODE_JUMP_TO:
                if (btnJumpTo != null && btnJumpTo.getVisibility() == View.VISIBLE) {
                    btnJumpTo.performClick();
                    Toast.makeText(this, "Jump To command activated", Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        // NOW handle short quantity input specific keys
        if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
            switch (keyCode) {
                case KEYCODE_VOICE_ADD:
                    if (btnAddDigit != null) {
                        btnAddDigit.performClick();
                        Toast.makeText(this, "Add command activated", Toast.LENGTH_SHORT).show();
                    }
                    return true;

                case KEYCODE_VOICE_REMOVE:
                    if (btnBackspaceDigit != null) {
                        btnBackspaceDigit.performClick();
                        Toast.makeText(this, "Remove command activated", Toast.LENGTH_SHORT).show();
                    }
                    return true;

                case KEYCODE_VOICE_BACK:
                    if (btnCancelShortInput != null) {
                        btnCancelShortInput.performClick();
                        Toast.makeText(this, "Back command activated", Toast.LENGTH_SHORT).show();
                    }
                    return true;

                case KEYCODE_VOICE_NEXT:
                    if (btnConfirmShortInput != null) {
                        btnConfirmShortInput.performClick();
                        Toast.makeText(this, "Next command activated", Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
            switch (keyCode) {
                case KEYCODE_SHORT_REMOVE:
                    if (btnBackspaceDigit != null) btnBackspaceDigit.performClick();
                    return true;
                case KEYCODE_SHORT_ADD:
                    if (btnAddDigit != null) btnAddDigit.performClick();
                    return true;
                case KEYCODE_SHORT_BACK:
                    if (btnCancelShortInput != null) btnCancelShortInput.performClick();
                    return true;
                case KEYCODE_SHORT_NEXT:
                    if (btnConfirmShortInput != null) btnConfirmShortInput.performClick();
                    return true;
                case KEYCODE_DIGIT_0:
                case KEYCODE_DIGIT_1:
                case KEYCODE_DIGIT_2:
                case KEYCODE_DIGIT_3:
                case KEYCODE_DIGIT_4:
                case KEYCODE_DIGIT_5:
                case KEYCODE_DIGIT_6:
                case KEYCODE_DIGIT_7:
                case KEYCODE_DIGIT_8:
                case KEYCODE_DIGIT_9:
                    int digit = keyCode - KeyEvent.KEYCODE_0;
                    if (digit == 0 && enteredQuantityBuilder.length() == 0) {
                        if (tvEnteredQuantity != null) tvEnteredQuantity.setText("0");
                        return true;
                    }
                    final int maxDigits = 6;
                    if (enteredQuantityBuilder.length() < maxDigits) {
                        enteredQuantityBuilder.append(digit);
                        if (tvEnteredQuantity != null) tvEnteredQuantity.setText(enteredQuantityBuilder.toString());
                    } else {
                        Toast.makeText(this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}