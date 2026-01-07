package com.example.supportapp.Consolidated_Pick;

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
import com.google.android.material.snackbar.Snackbar;
import com.example.supportapp.Consolidated_Pick.model.Location;

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

import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.Consolidated_Pick.model.ApiMessage;
import com.example.supportapp.Consolidated_Pick.model.ConsolidatedPickDetail;
import com.example.supportapp.Consolidated_Pick.model.ScanBarcodeRequest;
import com.example.supportapp.Consolidated_Pick.repo.ConsolidatedRepository;
import com.example.supportapp.R;
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
import java.util.Locale;

public class ConsolidatedJobActivity extends AppCompatActivity {

    private static final String TAG = "ConsolidatedJob";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 4441;

    private static final int KEYCODE_NEXT_SUMMARY = KeyEvent.KEYCODE_FORWARD;


    // Jump To request code
    private static final int REQ_PICK_LOCATION = 3021;

    private static final String PREF_CONS_BATCH_TOTAL = "cons_batch_total_";


    // Optional hardware keys for Short UI
    private static final int KEYCODE_SHORT_REMOVE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_SHORT_ADD    = KeyEvent.KEYCODE_F6;
    private static final int KEYCODE_SHORT_BACK   = KeyEvent.KEYCODE_F7;
    private static final int KEYCODE_SHORT_NEXT   = KeyEvent.KEYCODE_F8;
    private static final int KEYCODE_DIGIT_0 = KeyEvent.KEYCODE_0;
    private static final int KEYCODE_DIGIT_1 = KeyEvent.KEYCODE_1;
    private static final int KEYCODE_DIGIT_2 = KeyEvent.KEYCODE_2;
    private static final int KEYCODE_DIGIT_3 = KeyEvent.KEYCODE_3;
    private static final int KEYCODE_DIGIT_4 = KeyEvent.KEYCODE_4;
    private static final int KEYCODE_DIGIT_5 = KeyEvent.KEYCODE_5;
    private static final int KEYCODE_DIGIT_6 = KeyEvent.KEYCODE_6;
    private static final int KEYCODE_DIGIT_7 = KeyEvent.KEYCODE_7;
    private static final int KEYCODE_DIGIT_8 = KeyEvent.KEYCODE_8;
    private static final int KEYCODE_DIGIT_9 = KeyEvent.KEYCODE_9;
    private static final int KEYCODE_SHORT_COMMAND = KeyEvent.KEYCODE_F4;
    private static final int KEYCODE_JUMP_TO_COMMAND = KeyEvent.KEYCODE_F11;
    private static final int KEYCODE_SCAN_COMMAND = KeyEvent.KEYCODE_F3;
    private final ConsolidatedRepository repo = new ConsolidatedRepository();
    private static final int KEYCODE_SCROLL_DOWN  = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_STOP_SCROLL  = KeyEvent.KEYCODE_F2;
    // Info UI
    private TextView tvLocation, tvQuantity, tvProduct, tvDesc;
    private MaterialButton btnJumpTo;

    private boolean isScrolling = false;

    private static final int SCROLL_DELAY = 300; // milliseconds between scroll actions



    // NEW: Mfg/Exp Date UI
    private TextView manufacturingText, expirationText;
    private TextView manufacturingLabelText, expirationLabelText;

    private static final java.text.SimpleDateFormat OUT_FMT =
            new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());


    // Scan UI
    private Handler scrollingHandler = new Handler(Looper.getMainLooper());

    private PreviewView cameraPreview;
    private TextView skuStatusText;
    private View orderInfoContainer;
    private View menuLayout;
    private Button btnScanBarcode;

    // Track which locations have been fully completed (by location code)
    private final Set<String> completedLocationCodes = new HashSet<>();
    private static final String PREF_CONS_COMPLETED_LOCS = "cons_completed_locs_";

    // SHORT UI
    private View shortQuantityInputContainer;
    private NumberPicker npSingleDigitInput;
    private TextView tvEnteredQuantity;
    private Button btnAddDigit, btnBackspaceDigit, btnCancelShortInput, btnConfirmShortInput;

    // Progress UI
    private ProgressBar progressBar;
    private TextView progressText;
    private int totalItems = 0;

    private int originalTotalLocations = 0;  // ✅ This NEVER changes
    private int completedItems = 0;

    private int batchTotalItems = 0;   // fixed total for this batch (used for progress bar%)


    // NEW: Position tracking (current location index)
    private int currentLocationIndex = 0;  // 0-based index
    private static final String PREF_CONS_TOTAL = "cons_total_";
    private static final String PREF_CONS_DONE  = "cons_done_";
    private static final String PREF_CONS_CURRENT_INDEX = "cons_current_index_";

    // CameraX / ML Kit
    private ExecutorService cameraExecutor;
    private com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient vuzixSpeechClient;

    private ProcessCameraProvider cameraProvider;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    // Inputs from previous screen
    private String companyCode, prinCode, transBatchId, jobNo, siteCode, locationCode, prodCode, pickUser, orderNo;

    // The detail we show/confirm

    private String loginId;

    private ConsolidatedPickDetail detail;

    // For the SCAN_BARCODE payload
    private int pickedQty = 0;
    private String lastScanned = "";  // keep for pallet/product if needed

    // builder for short qty digits
    private final StringBuilder enteredQuantityBuilder = new StringBuilder();

    // Timer
    private long startTime = 0L;
    private long elapsedTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private TextView timerTextView;
    private int successfulPicksCount = 0;

    private List<Location> allLocations = Collections.emptyList();


    // Protect against double navigation
    private volatile boolean navigatedAway = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consolidated_job);

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

        // ========== RETRIEVE INTENT EXTRAS ==========
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

        // ========== INITIALIZE PROGRESS TRACKING ==========
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String batchKey = n(transBatchId);

        boolean isFreshStart = getIntent().getBooleanExtra("FRESH_START", false);

        if (isFreshStart) {
            Log.d(TAG, "🔄 FRESH START detected - Clearing previous locked value (was: " + originalTotalLocations + ")");

            // Clear ALL progress data including the locked total
            prefs.edit()
                    .remove(PREF_CONS_BATCH_TOTAL + batchKey)
                    .remove(PREF_CONS_TOTAL + batchKey)
                    .remove(PREF_CONS_DONE + batchKey)
                    .remove(PREF_CONS_CURRENT_INDEX + batchKey)
                    .remove(PREF_CONS_COMPLETED_LOCS + batchKey)
                    .apply();

            originalTotalLocations = -1;  // Force re-locking
        }
        // 🔒 STEP 1: Check if original total is already locked
        originalTotalLocations = prefs.getInt(PREF_CONS_BATCH_TOTAL + batchKey, -1);

        if (originalTotalLocations == -1) {
            // ✅ First time only - lock the original total from Intent
            int intentTotal = getIntent().getIntExtra("TOTAL_ITEMS", 0);

            if (intentTotal > 0) {
                originalTotalLocations = intentTotal;

                // 🔒 Lock it in SharedPreferences IMMEDIATELY
                prefs.edit()
                        .putInt(PREF_CONS_BATCH_TOTAL + batchKey, originalTotalLocations)
                        .apply();

                Log.d(TAG, "🔒 LOCKED original total locations at: " + originalTotalLocations);

                // Clear other progress data for fresh start (but NOT the batch total!)
                prefs.edit()
                        .remove(PREF_CONS_TOTAL + batchKey)
                        .remove(PREF_CONS_DONE + batchKey)
                        .remove(PREF_CONS_CURRENT_INDEX + batchKey)
                        .remove(PREF_CONS_COMPLETED_LOCS + batchKey)
                        .apply();
            } else {
                Log.w(TAG, "⚠️ No TOTAL_ITEMS in Intent and no saved total. Defaulting to 0.");
                originalTotalLocations = 0;
            }
        } else {
            Log.d(TAG, "✅ Using LOCKED original total: " + originalTotalLocations);
        }

        // ✅ Keep batchTotalItems in sync with originalTotalLocations
        batchTotalItems = originalTotalLocations;

        // 🔹 Load completed locations (for skipping logic only)
        loadCompletedLocations();

        // 🔹 Load progress state (Intent → SharedPreferences fallback)
        int t   = getIntent().getIntExtra("TOTAL_ITEMS", -1);
        int d   = getIntent().getIntExtra("COMPLETED_ITEMS", -1);
        int idx = getIntent().getIntExtra("CURRENT_LOCATION_INDEX", -1);

        if (t == -1)   t   = prefs.getInt(PREF_CONS_TOTAL + batchKey, originalTotalLocations);
        if (d == -1)   d   = prefs.getInt(PREF_CONS_DONE  + batchKey, 0);
        if (idx == -1) idx = prefs.getInt(PREF_CONS_CURRENT_INDEX + batchKey, 0);

        // Safety bounds
        if (t < 0)   t   = originalTotalLocations;
        if (d < 0)   d   = 0;
        if (idx < 0) idx = 0;

        totalItems           = t;
        completedItems       = d;
        currentLocationIndex = idx;
        successfulPicksCount = completedItems;  // sku_cnt starts from saved progress

        Log.d(TAG, "📊 Progress loaded: totalItems=" + totalItems +
                ", completedItems=" + completedItems +
                ", currentIndex=" + currentLocationIndex +
                ", originalTotal(locked)=" + originalTotalLocations);

        persistProgress();
        updateProgressUI();

        // ========== LOAD DATA ==========
        loadAllLocations();

        // Load detail for current location
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

        // ========== SETUP BUTTONS ==========
        Button logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> LogoutManager.performLogout(this));
        }

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
            i.putExtra("SELECTED_INDEX", currentLocationIndex);
            startActivityForResult(i, REQ_PICK_LOCATION);
        });

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
        updateProgressUI();
        setupVoiceCommands();
    }

    private void setupVoiceCommands() {
        try {
            vuzixSpeechClient = new com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient(this);

            // Clean up any existing phrases first
            vuzixSpeechClient.deletePhrase("Next");
            vuzixSpeechClient.deletePhrase("Next Order");
            vuzixSpeechClient.deletePhrase("Go to Next");
            vuzixSpeechClient.deletePhrase("Short");
            vuzixSpeechClient.deletePhrase("Jump To");
            vuzixSpeechClient.deletePhrase("Scan Barcode");
            vuzixSpeechClient.deletePhrase("Scan");
            vuzixSpeechClient.deletePhrase("Add");
            vuzixSpeechClient.deletePhrase("Remove");
            vuzixSpeechClient.deletePhrase("Back");
            vuzixSpeechClient.deletePhrase("OK");
            vuzixSpeechClient.deletePhrase("Ok");
            vuzixSpeechClient.deletePhrase("Okay");
            vuzixSpeechClient.deletePhrase("CLOSE");
            vuzixSpeechClient.deletePhrase("Close");

            // ✨ NEW: Clean up number phrases
            for (int i = 0; i <= 9; i++) {
                vuzixSpeechClient.deletePhrase("Number " + i);
            }

            // Register voice commands for next button
            vuzixSpeechClient.insertKeycodePhrase("Next", KEYCODE_SHORT_NEXT);
            vuzixSpeechClient.insertKeycodePhrase("Next Order", KEYCODE_NEXT_SUMMARY);
            vuzixSpeechClient.insertKeycodePhrase("Go to Next", KEYCODE_NEXT_SUMMARY);

            // Register voice commands for the 3 main functionalities
            vuzixSpeechClient.insertKeycodePhrase("Short", KEYCODE_SHORT_COMMAND);
            vuzixSpeechClient.insertKeycodePhrase("Jump To", KEYCODE_JUMP_TO_COMMAND);
            vuzixSpeechClient.insertKeycodePhrase("Scan Barcode", KEYCODE_SCAN_COMMAND);
            vuzixSpeechClient.insertKeycodePhrase("Scan", KEYCODE_SCAN_COMMAND);

            // Register voice commands for short UI actions
            vuzixSpeechClient.insertKeycodePhrase("Add", KEYCODE_SHORT_ADD);
            vuzixSpeechClient.insertKeycodePhrase("Remove", KEYCODE_SHORT_REMOVE);
            vuzixSpeechClient.insertKeycodePhrase("Back", KEYCODE_SHORT_BACK);

            // ✨ NEW: Register voice commands for numbers 0-9
            vuzixSpeechClient.insertKeycodePhrase("0", KEYCODE_DIGIT_0);
            vuzixSpeechClient.insertKeycodePhrase("1", KEYCODE_DIGIT_1);
            vuzixSpeechClient.insertKeycodePhrase("2", KEYCODE_DIGIT_2);
            vuzixSpeechClient.insertKeycodePhrase("3", KEYCODE_DIGIT_3);
            vuzixSpeechClient.insertKeycodePhrase("4", KEYCODE_DIGIT_4);
            vuzixSpeechClient.insertKeycodePhrase("5", KEYCODE_DIGIT_5);
            vuzixSpeechClient.insertKeycodePhrase("6", KEYCODE_DIGIT_6);
            vuzixSpeechClient.insertKeycodePhrase("7", KEYCODE_DIGIT_7);
            vuzixSpeechClient.insertKeycodePhrase("8", KEYCODE_DIGIT_8);
            vuzixSpeechClient.insertKeycodePhrase("9", KEYCODE_DIGIT_9);

            Log.i(TAG, "Voice commands registered successfully including numbers 0-9");
            Toast.makeText(this, "Voice commands ready: Next, Short, Jump To, Scan, Add, Remove, Back, Numbers 0-9", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient", e);
            Toast.makeText(this, "Voice commands unavailable", Toast.LENGTH_SHORT).show();
        }
    }


    /** Load all locations for this batch so we can auto-advance to the next index. */
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
                    // ✅ Update totalItems for navigation (dynamic count)
                    totalItems = allLocations.size();

                    // 🔒 NEVER override originalTotalLocations - it's locked forever!
                    Log.d(TAG, "Loaded " + totalItems + " locations. " +
                            "Original total (locked): " + originalTotalLocations);
                }

                runOnUiThread(this::updateProgressUI);

            } catch (IOException e) {
                Log.e(TAG, "Failed to load picklist in job screen", e);
            }
        }).start();
    }


    /**
     * After a successful RIGHT scan, move to the next location in the list
     * (currentLocationIndex + 1) and reload detail for that location.
     */
    /**
     * Jump back to the first location (index 0) and reload its detail.
     */
    /**
     * Jump back to the first INCOMPLETE location (index 0 or higher) and reload its detail.
     */
    private void moveToFirstLocationAndReload() {
        if (allLocations == null || allLocations.isEmpty()) {
            Log.w(TAG, "No locations available to loop back to.");
            return;
        }

        // ✅ Find the first incomplete location
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
            // ✅ All locations are completed, navigate to summary
            Log.d(TAG, "All locations are completed. Navigating to summary.");
            goNext();
            return;
        }

        // ✅ Set to first incomplete location and reload
        currentLocationIndex = firstIncompleteIndex;
        Location firstLoc = allLocations.get(firstIncompleteIndex);
        String batchId = n(transBatchId);

        Log.d(TAG, "Looping back to first incomplete location at index: " + currentLocationIndex);
        loadLocationDetail(firstLoc, batchId);
    }


    /**
     * Move to the next INCOMPLETE location and reload its detail.
     */
    private void moveToNextLocationAndReload() {
        if (allLocations == null || allLocations.isEmpty()) {
            Log.w(TAG, "No locations to move to.");
            return;
        }

        // ✅ Find the next incomplete location
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
            // ✅ No incomplete locations ahead, loop back to the beginning
            Log.d(TAG, "No incomplete locations ahead. Looping back to first incomplete location.");
            moveToFirstLocationAndReload();
            return;
        }

        // ✅ Move to next incomplete location
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
        String site = n(detail.getSiteCode());      // e.g. "A1"
        String loc  = n(detail.getLocationCode());  // e.g. "064901"

        tvLocation.setText(site + loc);             // A1064901


        // ✅ Remaining quantity = QUANTITY (max) - PDA_QUANTITY
        int maxQty = 0;
        int pdaQty = 0;

        Integer apiMax = detail.getQuantity();      // QUANTITY from API
        Integer apiPda = detail.getPdaQuantity();   // PDA_QUANTITY from API

        if (apiMax != null) maxQty = apiMax;
        if (apiPda != null) pdaQty = apiPda;

        int remainingQty = maxQty - pdaQty;
        if (remainingQty < 0) remainingQty = 0;     // safety

        pickedQty = remainingQty;                   // this is what we will send by default
        tvQuantity.setText(String.valueOf(remainingQty));

        // NEW: dates for this location
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

    /**
     * Load the set of completed location codes from SharedPreferences.
     */
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

    /**
     * Check if the current location is now fully completed (remaining qty = 0).
     * If so, mark it as completed and reduce the denominator.
     */


    /**
     * Save the set of completed location codes to SharedPreferences.
     */
    private void saveCompletedLocations() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String batchKey = n(transBatchId);

        // Join all completed location codes with commas
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

        // First digit cannot be just 0
        if (enteredQuantityBuilder.length() == 0 && d == 0) {
            tvEnteredQuantity.setText("0");
            return;
        }

        final int maxDigits = 6;
        if (enteredQuantityBuilder.length() >= maxDigits) {
            Toast.makeText(this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Candidate value if we add this digit
        String candidateStr = enteredQuantityBuilder.toString() + d;
        int candidateValue;
        try {
            candidateValue = Integer.parseInt(candidateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Validate against remaining = QUANTITY - PDA_QUANTITY
        if (detail != null) {
            Integer apiMax = detail.getQuantity();      // QUANTITY from API
            Integer apiPda = detail.getPdaQuantity();   // PDA_QUANTITY from API

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
                return; // ⛔ don’t add this digit
            }
        }

        // ✅ Accept digit and update UI
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
    // Extended Short — confirm quantity (now allows 0)
    // Extended Short — user enters SHORT (missing) quantity
    private void handleConfirmShortQuantity() {
        if (detail == null) {
            Toast.makeText(this, "Detail not loaded yet.", Toast.LENGTH_LONG).show();
            handleCancelShortQuantity();
            return;
        }

        // ✅ Interpret empty or "0" as short = 0
        int shortQty;
        if (enteredQuantityBuilder.length() == 0 ||
                (enteredQuantityBuilder.length() == 1 && enteredQuantityBuilder.charAt(0) == '0')) {
            shortQty = 0;   // allow 0 short
        } else {
            try {
                shortQty = Integer.parseInt(enteredQuantityBuilder.toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid short quantity entered.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ✅ Only block negative values (shouldn't happen via UI)
        if (shortQty < 0) {
            Toast.makeText(this, "Short quantity cannot be negative.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Remaining BEFORE applying short = QUANTITY (max) - PDA_QUANTITY
        Integer apiMax = detail.getQuantity();      // total qty from API
        Integer apiPda = detail.getPdaQuantity();   // already picked (PDA) qty

        int maxQty = (apiMax != null) ? apiMax : 0;
        int pdaQty = (apiPda != null) ? apiPda : 0;
        int remainingBeforeShort = maxQty - pdaQty;
        if (remainingBeforeShort < 0) remainingBeforeShort = 0;

        // Safety: short cannot be more than remaining
        if (shortQty > remainingBeforeShort) {
            Toast.makeText(
                    this,
                    "Short quantity (" + shortQty + ") cannot be more than remaining (" + remainingBeforeShort + ").",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // ✅ Final picked quantity = remaining - short
        int finalPicked = remainingBeforeShort - shortQty;   // e.g. 10 - 2 = 8

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

    // ===== UI states =====
    private void showInitialState() {
        setVisible(cameraPreview, false);
        setVisible(orderInfoContainer, true);
        setVisible(menuLayout, true);
        setVisible(skuStatusText, false);
        if (btnScanBarcode != null) btnScanBarcode.setEnabled(true);

        // Just refresh UI; don’t touch counters here
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

    private void handleScan(String scannedValue) {
        if (scannedValue == null) {
            Toast.makeText(this, "No barcode detected.", Toast.LENGTH_SHORT).show();
            showInitialState();
            return;
        }

        // Expected location from API / current detail
        String expectedRaw = n(locationCode);

        // Full scanned string from camera
        String gotRawFull = scannedValue == null ? "" : scannedValue;

        // 👉 Extract the location part: drop first 2 chars and keep digits
        String gotRaw = extractLocationFromScan(gotRawFull);

        // Normalized versions for comparison (still okay, mostly digits now)
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

        // RIGHT → call SCAN_BARCODE with current pickedQty
        sendScanToServer();
    }


    /** Calls SCAN_BARCODE after a RIGHT match. */
    /** Calls SCAN_BARCODE after a RIGHT match. */
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
                        // ✅ Increment successful pick count
                        successfulPicksCount++;
                        Log.d(TAG, "Successful pick count (sku_cnt): " + successfulPicksCount);

                        completedItems = successfulPicksCount;

                        // ✅ Mark location as complete (for skipping logic)
                        checkAndMarkLocationComplete();

                        persistProgress();

                        updateProgressUI();

                        // 🔹 Show debug toast AFTER successful scan
                        showDebugToast(true);

                        showScanResult(true);

                        // 🔹 Check if ALL picks are done using LOCKED denominator
                        boolean allSkusPicked = (successfulPicksCount >= originalTotalLocations);
                        if (allSkusPicked) {
                            Log.d(TAG, "All locations picked (" + successfulPicksCount + "/" + totalItems + "). Navigating to summary.");
                            goNext();
                            return;
                        } else {
                            // ✅ ADD THIS: Move to next incomplete location
                            Log.d(TAG, "Pick successful. Moving to next location...");

                            // Delay slightly so user sees "RIGHT SKU" message
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                moveToNextLocationAndReload();
                            }, 3000);  // matches the delay in showScanResult()
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // 🔹 Show debug toast on exception
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

        // Get current Intent values (what we received)
        Intent currentIntent = getIntent();
        int intentTotal = currentIntent.getIntExtra("TOTAL_ITEMS", -1);
        int intentCompleted = currentIntent.getIntExtra("COMPLETED_ITEMS", -1);
        int intentIndex = currentIntent.getIntExtra("CURRENT_LOCATION_INDEX", -1);

        // Calculate progress percentage
        int progressPct = (originalTotalLocations > 0) ? (successfulPicksCount * 100 / originalTotalLocations) : 0;

        // ✅ Get PREVIOUS location details
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

        // ✅ Get CURRENT location details (from detail object which has more info)
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

        // ✅ Get NEXT location details
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

        // ✅ Calculate total quantities across all locations
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

        // ✅ Check if at last location
        boolean isLastLocation = (currentLocationIndex >= totalItems - 1);
        boolean shouldExit = (successfulPicksCount >= originalTotalLocations);

        String debugMsg = "";



        Log.d(TAG, debugMsg.replace("\n", " | "));
    }

    /**
     * Check if the current location is now fully completed (remaining qty = 0).
     * If so, mark it as completed and reduce the denominator.
     */
    /**
     * Check if the current location is now fully completed (remaining qty = 0).
     * Mark it as completed for skipping logic, but DO NOT touch the denominator.
     */
    private void checkAndMarkLocationComplete() {
        if (detail == null) return;

        String currentLoc = n(detail.getLocationCode());
        if (currentLoc.isEmpty()) return;

        // Already marked as complete? Skip
        if (completedLocationCodes.contains(currentLoc)) {
            return;
        }

        // Check if remaining quantity is now 0
        Integer apiMax = detail.getQuantity();
        Integer apiPda = detail.getPdaQuantity();

        int maxQty = (apiMax != null) ? apiMax : 0;
        int pdaQty = (apiPda != null) ? apiPda : 0;
        int remainingQty = maxQty - pdaQty;

        // ✅ If this location is now fully picked (remaining = 0)
        if (remainingQty <= 0) {
            Log.d(TAG, "Location " + currentLoc + " is now fully completed. Marking as done.");

            // Mark as completed (for skipping logic only)
            completedLocationCodes.add(currentLoc);
            saveCompletedLocations();

            // ❌ DO NOT decrement batchTotalItems here!
            // The denominator is LOCKED and never changes.
            Log.d(TAG, "✅ Denominator remains locked at: " + batchTotalItems);
        }
    }

    /**
     * Load detail for a given location and update the UI.
     */
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
                    // Switch context to this location
                    companyCode  = nextCompany;
                    prinCode     = nextPrin;
                    transBatchId = nextBatch;
                    jobNo        = nextJob;
                    siteCode     = nextSite;
                    locationCode = nextLocCode;
                    prodCode     = nextProd;

                    detail = nextDetail;
                    bindUi();
                    updateProgressUI();  // Updates progress bar
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

    /**
     * Extract the 6-digit location code from the scanned QR/Barcode.
     * Example:
     *   raw = "A1029302"  -> substring(2) = "029302" -> digits-only "029302"
     *   raw = "029302"    -> stays "029302"
     */
    private String extractLocationFromScan(String raw) {
        if (raw == null) return "";

        String trimmed = raw.trim();

        // New format: 8 characters, where index 2..end is the location part
        // Example: "A1029302" -> "029302"
        if (trimmed.length() >= 8) {
            String locPart = trimmed.substring(2);  // from index 2 onwards
            // keep only digits from that part
            locPart = locPart.replaceAll("\\D", "");
            return locPart;
        }

        // Old format (already just the location code, maybe with noise) -> keep only digits
        return trimmed.replaceAll("\\D", "");
    }





    /** Try to finalize with COMPLETE_CONSOLIDATED_PICKING. Navigate only if 200. */
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
        if (detail == null) {
            Log.e(TAG, "goNext: detail is null, cannot continue");
            return;
        }

        // 🔹 Try to get order number from activity-level field
        String safeOrderNo = n(orderNo);

        // 🔹 If empty, fall back to detail.getOrderNo()
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

        i.putExtra("BATCH_ID",     n(transBatchId));  // matches what summary expects
        i.putExtra("LOGIN_ID",     n(loginId));
        i.putExtra("TRANS_BATCH_ID", n(transBatchId));

        // ✅ use the fixed safeOrderNo
        i.putExtra("ORDER_NO", safeOrderNo);

        // Counts
        i.putExtra("SUCCESSFUL_PICKS_COUNT", successfulPicksCount);
        i.putExtra("TOTAL_QUANTITY_SUM", getIntent().getIntExtra("TOTAL_QUANTITY_SUM", 0));

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

                // ✅ Update totalItems for navigation display (current visible count)
                totalItems = data.getIntExtra("TOTAL_ITEMS", totalItems);
                currentLocationIndex = data.getIntExtra("CURRENT_LOCATION_INDEX", currentLocationIndex);

                // 🔒 NEVER touch originalTotalLocations here - it stays locked!
                // ❌ DO NOT pass FRESH_START flag here - we want to keep the locked value!
                Log.d(TAG, "Returned from Jump To. totalItems=" + totalItems +
                        ", originalTotal (locked)=" + originalTotalLocations);

                // Do NOT reset completedItems / successfulPicksCount
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

        // ✅ Progress BAR = SKU Count / ORIGINAL Total Locations (locked)
        int skuCount = successfulPicksCount;
        int originalTotal = originalTotalLocations;

        if (originalTotal > 0) {
            int percentage = (int) ((skuCount * 100.0) / originalTotal);
            progressBar.setProgress(percentage);
            Log.d(TAG, "Progress Bar: SKU " + skuCount + "/" + originalTotal + " = " + percentage + "%");
        } else {
            progressBar.setProgress(0);
        }

        // ✅ Progress TEXT = Current Location / Total Locations (dynamic)
        // ✅ Progress TEXT = Successful Picks / Total Locations (or total items)
        if (progressText != null) {
            int numerator = successfulPicksCount;   // how many scans done so far
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
                .putInt(PREF_CONS_BATCH_TOTAL + key, originalTotalLocations)  // ✅ Always save locked value
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
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) cameraExecutor.shutdown();
        if (barcodeScanner != null) barcodeScanner.close();
    }

    // LEGACY (kept to avoid breaking anything else you may call elsewhere)
    private void startJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }
    private void pauseJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }
    private void resumeJobPicking(String orderNo, String jobNo) { /* unchanged — your existing code */ }

    private static boolean isBlankDate(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        // common API placeholders we want to treat as "no date"
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.equals("null") || lower.equals("n/a") || lower.equals("na") || lower.equals("{}")) return true;
        if (t.equals("--") || t.equals("0000-00-00")) return true;
        return false;
    }

    // ===== NEW: date binding helpers =====
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

        // Format and set only when we actually have a usable date
        if (hasMfg) {
            String formatted = formatAnyDate(mfgRaw);
            if (!isBlankDate(formatted)) manufacturingText.setText(formatted);
            else manufacturingText.setText(mfgRaw); // fallback to raw if formatting fails but original looked non-blank
        } else {
            manufacturingText.setText(""); // ensure cleared
        }

        if (hasExp) {
            String formatted = formatAnyDate(expRaw);
            if (!isBlankDate(formatted)) expirationText.setText(formatted);
            else expirationText.setText(expRaw);
        } else {
            expirationText.setText("");
        }

        // show/hide labels + values
        setVisible(manufacturingText, hasMfg);
        setVisible(manufacturingLabelText, hasMfg);
        setVisible(expirationText, hasExp);
        setVisible(expirationLabelText, hasExp);
    }

    /** Normalize barcode/location strings for robust comparison. */
    private String normalizeForCompare(@Nullable String s) {
        if (s == null) return "";
        // Trim, lowercase, remove invisible/control chars and optionally non-alphanumerics
        String t = s.trim().toLowerCase(Locale.ROOT);
        // Remove common noise (CR/LF, spaces, non-alphanum). Adjust regex if you want to keep punctuation.
        t = t.replaceAll("[\\p{C}\\s\\-\\_]", ""); // remove control chars, whitespace, hyphens, underscores
        t = t.replaceAll("[^a-z0-9]", ""); // keep only a-z0-9 (remove other punctuation)
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

    // Optional: keypad hotkeys for Short UI (unchanged)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean shortVisible = shortQuantityInputContainer != null
                && shortQuantityInputContainer.getVisibility() == View.VISIBLE;

        // 1️⃣ GLOBAL VOICE COMMANDS (always active)
        switch (keyCode) {
            case KEYCODE_SCROLL_DOWN:
                Log.i(TAG, "Voice command: Scrolling down");
                startScrollingDown();
                return true;

            case KEYCODE_STOP_SCROLL:
                Log.i(TAG, "Voice command: Stop scrolling");
                stopScrolling();
                return true;

            case KEYCODE_SHORT_COMMAND:
                Log.i(TAG, "Voice command: Short quantity input");
                showShortQuantityInput();
                return true;

            case KEYCODE_JUMP_TO_COMMAND:
                Log.i(TAG, "Voice command: Jump to location");
                if (btnJumpTo != null) {
                    btnJumpTo.performClick();
                } else {
                    Toast.makeText(this, "Jump To not available", Toast.LENGTH_SHORT).show();
                }
                return true;

            case KEYCODE_SCAN_COMMAND:
                Log.i(TAG, "Voice command: Scan barcode");
                if (btnScanBarcode != null) {
                    btnScanBarcode.performClick();
                } else {
                    Toast.makeText(this, "Scan not available", Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        // 2️⃣ SHORT-UI-SPECIFIC COMMANDS (only when short UI is visible)
        if (shortVisible) {
            switch (keyCode) {
                case KEYCODE_SHORT_REMOVE:
                    Log.i(TAG, "Voice command: Remove (Backspace)");
                    if (btnBackspaceDigit != null) btnBackspaceDigit.performClick();
                    return true;

                case KEYCODE_SHORT_ADD:
                    Log.i(TAG, "Voice command: Add digit");
                    if (btnAddDigit != null) btnAddDigit.performClick();
                    return true;

                case KEYCODE_SHORT_BACK:
                    Log.i(TAG, "Voice command: Back (Cancel)");
                    if (btnCancelShortInput != null) btnCancelShortInput.performClick();
                    return true;

                case KEYCODE_SHORT_NEXT:
                    Log.i(TAG, "Voice command: Next (Confirm)");
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
                        if (tvEnteredQuantity != null) {
                            tvEnteredQuantity.setText(enteredQuantityBuilder.toString());
                        }
                    } else {
                        Toast.makeText(this, "Maximum digits reached.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
        }

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

        // Simulate down arrow key press continuously
        scrollDownContinuously();
    }

    private void scrollDownContinuously() {
        if (!isScrolling) return;

        // Simulate down arrow key press
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));

        // Continue scrolling after delay
        scrollingHandler.postDelayed(this::scrollDownContinuously, SCROLL_DELAY);
    }

    @Override
    public void onBackPressed() {

        // 🔹 If camera/scanner is currently open → just close it
        if (isScanning.get() || (cameraPreview != null && cameraPreview.getVisibility() == View.VISIBLE)) {

            Log.d(TAG, "Back pressed while scanning – closing camera only");

            stopCameraIfAny();      // unbind CameraX
            showInitialState();     // return to menu UI
            return;                 // DO NOT finish activity
        }

        // 🔹 Otherwise behave normally
        super.onBackPressed();
    }


}