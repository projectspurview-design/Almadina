package com.example.supportapp;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.io.IOException;
import java.net.URLEncoder; // Added for URL encoding
import java.nio.charset.StandardCharsets; // Added for URL encoding
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections; // Added for sorting
import java.util.Comparator; // Added for sorting
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody; // Added for API response parsing
import org.json.JSONArray; // Added for JSON parsing
import org.json.JSONException; // Added for JSON parsing
import org.json.JSONObject; // Added for JSON parsing
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class TenthActivity extends AppCompatActivity {

    private static final String TAG = "TenthActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int MENU_REQUEST_CODE = 101;
    private static final int PICK_LOCATION_REQUEST_CODE = 102;

    private static final String CONFIRM_ORDER_BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/confirm_order";
    private static final String ORDER_DETAILS_BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/ORDER_DETAILS";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";

    // NEW API Endpoints
    private static final String PAUSE_JOB_PICKING_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/PAUSE_JOB_PICKING";
    private static final String RESUME_JOB_PICKING_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/RESUME_JOB_PICKING";


    // SharedPreferences Keys for Last Picked Location
    private static final String PREF_LAST_PICKED_LOCATION_ID = "lastPickedLocationId";
    private static final String PREF_LAST_PICKED_LOCATION_CODE = "lastPickedLocationCode";
    private static final String PREF_LAST_PICKED_QUANTITY = "lastPickedQuantity";
    private static final String PREF_LAST_PICKED_PRODUCT = "lastPickedProduct";
    private static final String PREF_LAST_PICKED_DESCRIPTION = "lastPickedDescription";
    private static final String PREF_LAST_PICKED_EXPIRATION = "lastPickedExpiration";
    private static final String PREF_LAST_PICKED_MANUFACTURING = "lastPickedManufacturing";
    private static final String PREF_LAST_PICKED_PDA_VERIFIED = "lastPickedPdaVerified";

    // SharedPreferences Keys to store the Order/Job associated with the last picked location
    private static final String PREF_LAST_PICKED_ORDER_NO = "lastPickedOrderNo";
    private static final String PREF_LAST_PICKED_JOB_NO = "lastPickedJobNo";

    // SharedPreferences Keys for CURRENT Order/Job - used for detecting new orders
    private static final String PREF_CURRENT_ORDER_NO = "CURRENT_ORDER_NO";
    private static final String PREF_CURRENT_JOB_NO = "CURRENT_JOB_NO";

    // NEW: SharedPreferences key prefix to track if a job has been "started" before
    private static final String JOB_STARTED_PREFIX = "job_started_"; // e.g., "job_started_1025072980"


    private VuzixSpeechClient speechClient;
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

    private final StringBuilder enteredQuantityBuilder = new StringBuilder();

    private static final long SKU_MESSAGE_DISPLAY_DURATION_MS = 4000;

    private ArrayList<Location> orderLocations = new ArrayList<>();
    private int currentLocationIndex = -1; // Index of the current location in orderLocations
    private Location currentOrderLocation; // The actual Location object being processed

    private String currentUserId;
    private String orderNumber; // This will hold the current order number for this activity instance
    private String jobNumber; // This will hold the current job number for this activity instance
    private String referenceCode;
    private String prinCode;

    // UI elements for progress bar and text
    private ProgressBar progressBar;
    private TextView progressText;
    private int originalTotalLocations = 0; // NEW: To store the initial total count for progress bar


    private static final int KEYCODE_BACK_VOICE = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_SCAN_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_SHORT_VOICE = KeyEvent.KEYCODE_F3;
    private static final int KEYCODE_JUMP_TO_VOICE = KeyEvent.KEYCODE_F4;
    private static final int KEYCODE_SHORT_REMOVE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_SHORT_ADD = KeyEvent.KEYCODE_F6;
    private static final int KEYCODE_SHORT_BACK = KeyEvent.KEYCODE_F7;
    private static final int KEYCODE_SHORT_NEXT = KeyEvent.KEYCODE_F8;
    private static final String JOB_INITIAL_TOTAL_PICKS_PREFIX = "job_initial_total_picks_";
    private static final String JOB_INITIAL_TOTAL_QUANTITY_PREFIX = "job_initial_total_quantity_";
    private int initialApiTotalPicks = 0;
    private int initialApiTotalQuantity = 0;
    private static final int KEYCODE_SCROLL_UP = KeyEvent.KEYCODE_F9;
    private static final int KEYCODE_SCROLL_DOWN = KeyEvent.KEYCODE_F10;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F11;
    private static final int KEYCODE_DIGIT_0 = KeyEvent.KEYCODE_0; // You can use actual digit keycodes
    private static final int KEYCODE_DIGIT_1 = KeyEvent.KEYCODE_1;
    private static final int KEYCODE_DIGIT_2 = KeyEvent.KEYCODE_2;
    private static final int KEYCODE_DIGIT_3 = KeyEvent.KEYCODE_3;
    private static final int KEYCODE_DIGIT_4 = KeyEvent.KEYCODE_4;
    private static final int KEYCODE_DIGIT_5 = KeyEvent.KEYCODE_5;
    private static final int KEYCODE_DIGIT_6 = KeyEvent.KEYCODE_6;
    private static final int KEYCODE_DIGIT_7 = KeyEvent.KEYCODE_7;
    private static final int KEYCODE_DIGIT_8 = KeyEvent.KEYCODE_8;
    private static final int KEYCODE_DIGIT_9 = KeyEvent.KEYCODE_9;
    private long startTime = 0;
    private long elapsedTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private ImageView productImageView;



    private void initializeHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                            // Accept all client certificates
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                            // For production, you should implement proper certificate validation here
                            // For now, we'll accept all server certificates
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Create hostname verifier that accepts all hostnames
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // Accept all hostnames for development
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
            // Fallback to default client
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
    }

    // Now your onCreate method should look like this:
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenth);
        // Initialize the timer
        startTimer();
        setupVoiceCommands();

        initialApiTotalPicks = getIntent().getIntExtra("INITIAL_API_TOTAL_PICKS", 0);
        initialApiTotalQuantity = getIntent().getIntExtra("INITIAL_API_TOTAL_QUANTITY", 0);
        Log.d(TAG, "Received Initial Total Picks: " + initialApiTotalPicks + ", Initial Total Quantity: " + initialApiTotalQuantity);

        // Initialize OkHttpClient with SSL handling - CALL THE METHOD HERE
        initializeHttpClient();

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

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(TenthActivity.this);
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

        if (btnAddDigit != null) {
            btnAddDigit.setOnClickListener(v -> handleAddDigit());
        }
        if (btnBackspaceDigit != null) {
            btnBackspaceDigit.setOnClickListener(v -> handleBackspaceDigit());
        }
        btnConfirmShortInput.setOnClickListener(v -> handleConfirmShortQuantity());
        btnCancelShortInput.setOnClickListener(v -> handleCancelShortQuantity());

        // IMPORTANT: Call getUserDataAndReferences FIRST to determine the current order/job
        // and handle clearing of last picked location if it's a new order.
        getUserDataAndReferences();

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        // 1. Try to load the last picked location for the *current* order/job
        currentOrderLocation = loadLastPickedLocation(orderNumber, jobNumber); // Pass current order/job

        if (currentOrderLocation != null) {
            Log.d(TAG, "Loaded last picked location from SharedPreferences for current order: " + currentOrderLocation.getLocationCode());
            updateUIWithLocationDetails(currentOrderLocation);
            // If a last picked location is found, we assume orderLocations might not be fully loaded or updated yet
            // We should still fetch the full list to ensure progress bar and 'findNextUnverifiedLocationAndProceed' work correctly.
            fetchOrderLocations(false); // Fetch but don't force selection if last picked is already set
        } else {
            // 2. If no last picked location for this order, or if it was cleared/invalid,
            // then fetch the order details and display the first unverified location.
            updateUIWithLocationDetails(null); // Initialize UI with N/A temporarily
            Log.d(TAG, "No valid last picked location found for current order, fetching order details...");
            fetchOrderLocations(true); // Fetch and force selection of the first unverified
        }

        // Progress display will be updated after fetchOrderLocations or onActivityResult
        // showInitialScreenState() will also be called after location data is processed
    }

    private void startTimer() {
        startTime = System.currentTimeMillis(); // Start the timer
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedTime = System.currentTimeMillis() - startTime; // Calculate elapsed time
                // Update UI with the elapsed time if needed
                updateTimerUI(elapsedTime);

                // Schedule the next update in 1 second
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable); // Start the timer
    }

    private void updateTimerUI(long elapsedTime) {
        // You can format and display the elapsed time if needed
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        // For example, update a TextView with the elapsed time
        TextView timerTextView = findViewById(R.id.timerTextView);
        timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
    }


    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.insertKeycodePhrase("Back", KEYCODE_BACK_VOICE);
            speechClient.insertKeycodePhrase("Scan", KEYCODE_SCAN_VOICE);
            speechClient.insertKeycodePhrase("Short", KEYCODE_SHORT_VOICE);
            speechClient.insertKeycodePhrase("Jump To", KEYCODE_JUMP_TO_VOICE);
            speechClient.insertKeycodePhrase("Remove", KEYCODE_SHORT_REMOVE);
            speechClient.insertKeycodePhrase("Add", KEYCODE_SHORT_ADD);
            speechClient.insertKeycodePhrase("Back", KEYCODE_SHORT_BACK);
            speechClient.insertKeycodePhrase("Next", KEYCODE_SHORT_NEXT);
            speechClient.insertKeycodePhrase("Scroll Up", KEYCODE_SCROLL_UP);
            speechClient.insertKeycodePhrase("Scroll Down", KEYCODE_SCROLL_DOWN);
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);
            speechClient.insertKeycodePhrase("Zero", KEYCODE_DIGIT_0);
            speechClient.insertKeycodePhrase("One", KEYCODE_DIGIT_1);
            speechClient.insertKeycodePhrase("Two", KEYCODE_DIGIT_2);
            speechClient.insertKeycodePhrase("Three", KEYCODE_DIGIT_3);
            speechClient.insertKeycodePhrase("Four", KEYCODE_DIGIT_4);
            speechClient.insertKeycodePhrase("Five", KEYCODE_DIGIT_5);
            speechClient.insertKeycodePhrase("Six", KEYCODE_DIGIT_6);
            speechClient.insertKeycodePhrase("Seven", KEYCODE_DIGIT_7);
            speechClient.insertKeycodePhrase("Eight", KEYCODE_DIGIT_8);
            speechClient.insertKeycodePhrase("Nine", KEYCODE_DIGIT_9);


            Log.d(TAG, "Voice commands registered with VuzixSpeechClient.");
        } catch (Exception e) {
            Log.e(TAG, "Voice command setup failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_BACK_VOICE:
                onBackPressed(); // This will trigger the finish logic which sends PAUSE
                return true;
            case KEYCODE_SCAN_VOICE:
                if (btnScanBarcode != null) {
                    btnScanBarcode.performClick();
                }
                return true;
            case KEYCODE_SHORT_VOICE:
                View shortBtn = findViewById(R.id.btnShort);
                if (shortBtn != null) shortBtn.performClick();
                return true;
            case KEYCODE_JUMP_TO_VOICE:
                View jumpBtn = findViewById(R.id.btnJumpTo);
                if (jumpBtn != null) jumpBtn.performClick();
                return true;
            case KEYCODE_SHORT_REMOVE:
                View btnRemove = findViewById(R.id.btnBackspaceDigit);
                if (btnRemove != null && btnRemove.getVisibility() == View.VISIBLE) {
                    btnRemove.performClick();
                }
                return true;

            case KEYCODE_SHORT_ADD:
                View btnAdd = findViewById(R.id.btnAddDigit);
                if (btnAdd != null && btnAdd.getVisibility() == View.VISIBLE) {
                    btnAdd.performClick();
                }
                return true;

            case KEYCODE_SHORT_BACK:
                View btnBack = findViewById(R.id.btnCancelShortInput);
                if (btnBack != null && btnBack.getVisibility() == View.VISIBLE) {
                    btnBack.performClick();
                }
                return true;

            case KEYCODE_SHORT_NEXT:
                View btnNext = findViewById(R.id.btnConfirmShortInput);
                if (btnNext != null && btnNext.getVisibility() == View.VISIBLE) {
                    btnNext.performClick();
                }
                return true;
            case KEYCODE_SCROLL_UP:
                if (npSingleDigitInput != null && npSingleDigitInput.getVisibility() == View.VISIBLE) {
                    int currentVal = npSingleDigitInput.getValue();
                    if (currentVal > npSingleDigitInput.getMinValue()) {
                        npSingleDigitInput.setValue(currentVal - 1);
                    }
                }
                return true;

            case KEYCODE_SCROLL_DOWN:
                if (npSingleDigitInput != null && npSingleDigitInput.getVisibility() == View.VISIBLE) {
                    int currentVal = npSingleDigitInput.getValue();
                    if (currentVal < npSingleDigitInput.getMaxValue()) {
                        npSingleDigitInput.setValue(currentVal + 1);
                    }
                }
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(TenthActivity.this);
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
                if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
                    handleVoiceDigitInput(keyCode - KeyEvent.KEYCODE_0); // converts keycode to digit
                    return true;
                }
                break;
        }

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


    // Override onBackPressed to send PAUSE API call and finish
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed. Sending RESULT_CANCELED to NinthActivity.");
        setResult(Activity.RESULT_CANCELED); // Signal NinthActivity to send PAUSE
        super.onBackPressed();
    }

    private void getUserDataAndReferences() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Get the order/job from the Intent (if this is a new launch/order selection)
        // These are the parameters received from NinthActivity
        String intentOrderNumber = getIntent().getStringExtra("ORDER_NUMBER");
        String intentJobNumber = getIntent().getStringExtra("JOB_NUMBER");
        String intentUserId = getIntent().getStringExtra("USER_ID");
        String intentPrinCode = getIntent().getStringExtra("PRIN_CODE");
        String intentReferenceCode = getIntent().getStringExtra("REFERENCE_CODE");


        // Get the previously saved current order/job from SharedPreferences
        String prevSavedOrderNo = prefs.getString(PREF_CURRENT_ORDER_NO, ""); // Use specific key
        String prevSavedJobNo = prefs.getString(PREF_CURRENT_JOB_NO, ""); // Use specific key

        // Determine the actual order/job for this session, prioritizing Intent extras
        // These will become the 'currentOrderNumber' and 'currentJobNumber' for *this activity instance*
        orderNumber = intentOrderNumber != null ? intentOrderNumber : prefs.getString(PREF_CURRENT_ORDER_NO, "");
        jobNumber = intentJobNumber != null ? intentJobNumber : prefs.getString(PREF_CURRENT_JOB_NO, "");

        // Determine currentUserId, referenceCode, prinCode, prioritizing Intent extras
        currentUserId = intentUserId != null ? intentUserId : prefs.getString("CURRENT_USER_ID", prefs.getString("LOGGED_IN_USER_ID", ""));
        String currentUserName = getIntent().getStringExtra("USER_NAME"); // From intent, if any
        currentUserName = currentUserName != null ? currentUserName : prefs.getString("CURRENT_USER_NAME", prefs.getString("LOGGED_IN_USER_NAME", "User"));
        referenceCode = intentReferenceCode != null ? intentReferenceCode : prefs.getString("CURRENT_REFERENCE", "");
        prinCode = intentPrinCode != null ? intentPrinCode : prefs.getString("PRIN_CODE", "029");


        // Compare the *newly determined* order/job for this activity instance with the *previously saved* order/job from SharedPreferences.
        boolean isNewOrder = !orderNumber.equals(prevSavedOrderNo) || !jobNumber.equals(prevSavedJobNo);

        if (isNewOrder && (!orderNumber.isEmpty() || !jobNumber.isEmpty())) { // Only clear if we actually have a new, non-empty order
            Log.d(TAG, "Detected new order/job. Clearing last picked location from SharedPreferences. Old: " + prevSavedOrderNo + "/" + prevSavedJobNo + ", New: " + orderNumber + "/" + jobNumber);
            saveLastPickedLocation(null); // Clear the last picked location for the old order
            // NEW: If it's a new order, also reset the "job started" flag and initial totals for the new job
            if (!jobNumber.isEmpty()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(JOB_STARTED_PREFIX + jobNumber, false); // Mark the NEW job as not yet started
                editor.remove(JOB_INITIAL_TOTAL_PICKS_PREFIX + jobNumber); // Clear initial picks
                editor.remove(JOB_INITIAL_TOTAL_QUANTITY_PREFIX + jobNumber); // Clear initial quantity
                editor.apply();
                Log.d(TAG, "Cleared 'job started' flag and initial totals for new job: " + jobNumber);
            }
        } else if (orderNumber.isEmpty() && jobNumber.isEmpty()) {
            // If both are empty, it means no order is selected. Ensure last picked is cleared.
            Log.d(TAG, "No order/job detected. Clearing last picked location from SharedPreferences.");
            saveLastPickedLocation(null);
        }

        // Save the currently active order/job/user details for persistence
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("CURRENT_USER_ID", currentUserId);
        editor.putString("CURRENT_USER_NAME", currentUserName);
        editor.putString(PREF_CURRENT_ORDER_NO, orderNumber); // Save the *current activity's* order number
        editor.putString(PREF_CURRENT_JOB_NO, jobNumber); // Save the *current activity's* job number
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

        // NEW: Retrieve the order/job associated with the *saved* last picked location
        String savedLastPickedOrderNo = prefs.getString(PREF_LAST_PICKED_ORDER_NO, "");
        String savedLastPickedJobNo = prefs.getString(PREF_LAST_PICKED_JOB_NO, "");

        // Crucial: Check if the loaded location's order/job matches the *current* activity's order/job
        if (lastLocationId != null && !lastLocationId.isEmpty() &&
                lastLocationCode != null && !lastLocationCode.isEmpty() &&
                currentOrderNo.equals(savedLastPickedOrderNo) && currentJobNo.equals(savedLastPickedJobNo) && // THIS IS THE KEY CHECK
                lastPdaVerified.equalsIgnoreCase("N")) { // Only load if it's still unverified
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
            // NEW: Save the order/job this location belongs to
            editor.putString(PREF_LAST_PICKED_ORDER_NO, orderNumber); // Use the activity's current orderNumber
            editor.putString(PREF_LAST_PICKED_JOB_NO, jobNumber); // Use the activity's current jobNumber
            Log.d(TAG, "Saved last picked location to SharedPreferences: " + location.getLocationCode() + " (Verified: " + location.getPdAVerified() + ") for order/job: " + orderNumber + "/" + jobNumber);
        } else {
            // If location is null, clear all related saved data
            editor.remove(PREF_LAST_PICKED_LOCATION_ID);
            editor.remove(PREF_LAST_PICKED_LOCATION_CODE);
            editor.remove(PREF_LAST_PICKED_QUANTITY);
            editor.remove(PREF_LAST_PICKED_PRODUCT);
            editor.remove(PREF_LAST_PICKED_DESCRIPTION);
            editor.remove(PREF_LAST_PICKED_EXPIRATION);
            editor.remove(PREF_LAST_PICKED_MANUFACTURING);
            editor.remove(PREF_LAST_PICKED_PDA_VERIFIED);
            // NEW: Also clear the associated order/job numbers
            editor.remove(PREF_LAST_PICKED_ORDER_NO);
            editor.remove(PREF_LAST_PICKED_JOB_NO);
            Log.d(TAG, "Cleared last picked location from SharedPreferences.");
        }
        editor.apply();
    }

    // --- NEW METHOD TO FETCH ORDER LOCATIONS (similar to PickLocationActivity's) ---
    private void fetchOrderLocations(boolean forceSelectionOfFirstUnverified) { // Reverted to original signature
        if (orderNumber == null || orderNumber.isEmpty() || jobNumber == null || jobNumber.isEmpty()) {
            Log.d(TAG, "Order or Job number is missing. Cannot fetch locations.");
            Toast.makeText(this, "Order or Job number is missing. Please select an order.", Toast.LENGTH_LONG).show();
            updateUIWithLocationDetails(null);
            updateProgressDisplay();
            showInitialScreenState();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        progressText.setText("Loading order details...");
        orderInfoContainer.setVisibility(View.GONE); // Hide main info while loading

        try {
            StringBuilder urlBuilder = new StringBuilder(ORDER_DETAILS_BASE_URL);
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
                    .addHeader("accept", "application/json")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Network request failed for order details", e);
                    // Make error message effectively final for the lambda
                    final String errorMessage = e.getMessage();
                    runOnUiThread(() -> {
                        // progressBar.setVisibility(View.GONE); // REMOVED THIS LINE
                        orderInfoContainer.setVisibility(View.VISIBLE); // Show info container again
                        Toast.makeText(TenthActivity.this, "Network error fetching order details: " + errorMessage, Toast.LENGTH_LONG).show();
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
                        if (responseBody != null) {
                            responseString = responseBody.string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body or closing response for order details", e);
                        // Make error message effectively final for the lambda
                        final String errorMessage = e.getMessage();
                        runOnUiThread(() -> {
                            // progressBar.setVisibility(View.GONE); // REMOVED THIS LINE
                            orderInfoContainer.setVisibility(View.VISIBLE);
                            Toast.makeText(TenthActivity.this, "Failed to read server response for order details: " + errorMessage, Toast.LENGTH_LONG).show();
                            updateUIWithLocationDetails(null);
                            updateProgressDisplay();
                            showInitialScreenState();
                        });
                        return;
                    }

                    // Make responseString effectively final for all subsequent lambdas
                    final String finalResponseString = responseString; // This is the key fix for lines 428, 429, 431
                    // Make response code effectively final for the lambda
                    final int finalResponseCode = response.code();
                    Log.d(TAG, "Order Details API Response Code: " + finalResponseCode);
                    Log.d(TAG, "Order Details API Response Body: " + finalResponseString);
                    // Make forceSelectionOfFirstUnverified effectively final for the lambda
                    final boolean finalForceSelection = forceSelectionOfFirstUnverified;

                    runOnUiThread(() -> {
                        orderInfoContainer.setVisibility(View.VISIBLE); // Show info container again
                        if (response.isSuccessful()) {
                            try {
                                JSONArray locationsArray = null;
                                // Attempt to parse as array or object with "Details" / "data" / single object
                                if (finalResponseString.trim().startsWith("[")) { // Use finalResponseString
                                    locationsArray = new JSONArray(finalResponseString); // Use finalResponseString
                                } else {
                                    JSONObject responseObject = new JSONObject(finalResponseString); // Use finalResponseString
                                    if (responseObject.has("Details")) {
                                        Object detailsObject = responseObject.get("Details");
                                        if (detailsObject instanceof JSONArray) {
                                            locationsArray = (JSONArray) detailsObject;
                                        } else if (detailsObject instanceof JSONObject) {
                                            locationsArray = new JSONArray();
                                            locationsArray.put(detailsObject);
                                        }
                                    } else if (responseObject.has("data")) {
                                        Object dataObject = responseObject.get("data");
                                        if (dataObject instanceof JSONArray) {
                                            locationsArray = (JSONArray) dataObject;
                                        } else if (dataObject instanceof JSONObject) {
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
                                    Toast.makeText(TenthActivity.this, "No location data found for this order.", Toast.LENGTH_LONG).show();
                                    orderLocations.clear();
                                    originalTotalLocations = 0;
                                    currentOrderLocation = null;
                                    currentLocationIndex = -1;
                                    updateUIWithLocationDetails(null);
                                    saveLastPickedLocation(null); // Clear last picked if no locations
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
                                        Log.w(TAG, "Error parsing individual location object from API response at index " + i + ", skipping.", e);
                                    }
                                }

                                orderLocations.clear();
                                orderLocations.addAll(newFetchedLocations);
                                // Sort the fetched locations
                                Collections.sort(orderLocations, new Comparator<Location>() {
                                    @Override
                                    public int compare(Location l1, Location l2) {
                                        int locCompare = l1.getLocationCode().compareTo(l2.getLocationCode());
                                        if (locCompare == 0) {
                                            return l1.getProduct().compareTo(l2.getProduct());
                                        }
                                        return locCompare;
                                    }
                                });

                                originalTotalLocations = orderLocations.size();
                                Log.d(TAG, "Fetched " + originalTotalLocations + " locations. Original total set.");

                                // --- INITIAL LOCATION SELECTION LOGIC based on finalForceSelection ---
                                Location targetLocation = null;
                                int targetIndex = -1;

                                if (finalForceSelection) { // Use the effectively final variable
                                    // Find the first unverified location
                                    for (int i = 0; i < orderLocations.size(); i++) {
                                        if (orderLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) {
                                            targetLocation = orderLocations.get(i);
                                            targetIndex = i;
                                            Log.d(TAG, "Set current location to first unverified (forced): " + targetLocation.getLocationCode());
                                            break;
                                        }
                                    }
                                } else {
                                    // Try to load the last picked location first
                                    Location loadedLastPicked = loadLastPickedLocation(orderNumber, jobNumber);
                                    if (loadedLastPicked != null) {
                                        // Find the loadedLastPicked in the newly fetched list
                                        for (int i = 0; i < orderLocations.size(); i++) {
                                            if (orderLocations.get(i).getId().equals(loadedLastPicked.getId()) &&
                                                    orderLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) { // Ensure it's still unverified
                                                targetLocation = orderLocations.get(i);
                                                // Apply the locally saved quantity (from short pick) to the fetched object
                                                targetLocation.setQuantity(loadedLastPicked.getQuantity());
                                                targetIndex = i;
                                                Log.d(TAG, "Found and set last picked location (not forced first): " + targetLocation.getLocationCode() + " with quantity " + targetLocation.getQuantity());
                                                break;
                                            }
                                        }
                                    }

                                    // Fallback to first unverified if last picked is not found or is now verified
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
                                    saveLastPickedLocation(currentOrderLocation); // Save the chosen current location
                                    Toast.makeText(TenthActivity.this, "Order loaded. Showing location: " + currentOrderLocation.getLocationCode(), Toast.LENGTH_SHORT).show();
                                } else {
                                    // All locations are verified or list is empty after filtering
                                    Toast.makeText(TenthActivity.this, "All locations for this order are already verified.", Toast.LENGTH_LONG).show();
                                    currentOrderLocation = null;
                                    currentLocationIndex = -1;
                                    updateUIWithLocationDetails(null);
                                    saveLastPickedLocation(null); // Clear last picked if all are verified
                                }
                                // --- END INITIAL LOCATION SELECTION LOGIC ---

                                updateProgressDisplay();
                                showInitialScreenState(); // Ensure UI is in initial state after loading

                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error for order details response: " + e.getMessage(), e);
                                // Make error message effectively final for the lambda
                                final String jsonErrorMessage = e.getMessage();
                                runOnUiThread(() -> {
                                    Toast.makeText(TenthActivity.this, "Error parsing order details: " + jsonErrorMessage, Toast.LENGTH_LONG).show();
                                    updateUIWithLocationDetails(null);
                                    orderLocations.clear();
                                    originalTotalLocations = 0;
                                    updateProgressDisplay();
                                    showInitialScreenState();
                                });
                            }

                        } else {
                            // Use finalResponseString and finalResponseCode
                            Toast.makeText(TenthActivity.this, "Server error fetching order details: " + finalResponseCode + " " + finalResponseString, Toast.LENGTH_SHORT).show();
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
            // Make error message effectively final for the lambda
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
        // Pass the existing list of locations to PickLocationActivity to potentially highlight the one
        // and allow it to manage the list for return.
        if (orderLocations != null && !orderLocations.isEmpty()) {
            intent.putParcelableArrayListExtra(PickLocationActivity.ORDER_LOCATIONS_LIST_KEY, orderLocations);
        }
        // Also pass the actual currentOrderLocation object so PickLocationActivity can intelligently
        // pre-select or scroll to it if it exists in the fetched list.
        intent.putExtra(PickLocationActivity.CURRENT_LOCATION_OBJECT_KEY, currentOrderLocation);
        startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
    }

    private void toggleScanState() {
        if (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please confirm or cancel short quantity first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // IMPORTANT: Prevent scanning if no location is selected
        if (currentOrderLocation == null) {
            Toast.makeText(this, "Please select a location first by loading an order or using 'Jump To'.", Toast.LENGTH_LONG).show();
            // No need to open PickLocationActivity automatically here, as per new requirement
            return;
        }

        // Using btnScanBarcode directly
        if (btnScanBarcode.getText().toString().equalsIgnoreCase(getString(R.string.resume_text))) {
            updateUIForPauseState(false); // Resume from paused state
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
                // Make error message effectively final for the lambda
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
        if (!isError) {
            showInitialScreenState();
        }
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

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (!isScanning.get()) {
                image.close();
                return;
            }

            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage =
                        InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                barcodeScanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            if (!isScanning.get() || barcodes.isEmpty()) {
                                if (!isScanning.get()) {
                                    Log.d(TAG, "Scanning stopped before barcode processing completed or no barcode found.");
                                }
                                return;
                            }

                            if (isScanning.compareAndSet(true, false)) { // Ensure only one barcode is processed
                                if (mCameraProvider != null) {
                                    mCameraProvider.unbindAll();
                                }
                                String scannedBarcodeValue = barcodes.get(0).getRawValue();
                                Log.d(TAG, "Barcode detected: " + scannedBarcodeValue);

                                // For barcode scan, we assume full quantity pick
                                verifyBarcodeWithServer(scannedBarcodeValue, currentOrderLocation.getQuantity());
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isScanning.get()) {
                                Log.e(TAG, "Barcode scanning failed", e);
                            }
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

        Log.d(TAG, "DEBUG: Scanned Barcode Value (raw): '" + scannedBarcodeValue + "'");
        Log.d(TAG, "DEBUG: Expected Location Code (raw): '" + expectedLocationCode + "'");
        Log.d(TAG, "DEBUG: Scanned Barcode Value (trimmed, lowercase): '" + scannedBarcodeValue.trim().toLowerCase() + "'");
        Log.d(TAG, "DEBUG: Expected Location Code (trimmed, lowercase): '" + (expectedLocationCode != null ? expectedLocationCode.trim().toLowerCase() : "") + "'");


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

            if (productImageView != null) {
                // show placeholder or hide
                productImageView.setImageResource(R.drawable.ic_placeholder_image);
                // or: productImageView.setVisibility(View.GONE);
            }
            Log.d(TAG, "UI cleared: No current location set.");
            return;
        }

        locationText.setText(location.getLocationCode() != null && !location.getLocationCode().isEmpty() ? location.getLocationCode() : "N/A");
        quantityText.setText(location.getQuantity() != 0 ? String.valueOf(location.getQuantity()) : "");
        productText.setText(location.getProduct() != null && !location.getProduct().isEmpty() ? location.getProduct() : "N/A");
        descriptionText.setText(location.getDescription() != null && !location.getDescription().isEmpty() ? location.getDescription() : "N/A");

        // ✅ Format expiration and manufacturing dates to show only date (no time)
        expirationText.setText(formatDateOnly(location.getExpiration()));
        manufacturingText.setText(formatDateOnly(location.getManufacturing()));

        // ✅ Load product image
        String imageUrl = null;
        try {
            // use whichever getter your Location class exposes
            // (common names used in your project/list screen)
            imageUrl = (String) location.getClass().getMethod("getAwsPath").invoke(location);
        } catch (Exception ignore) {
            // fallback if your getter is named differently
            try { imageUrl = (String) location.getClass().getMethod("getImageUrl").invoke(location); } catch (Exception ignored) {}
            try { if (imageUrl == null) imageUrl = (String) location.getClass().getMethod("getImagePath").invoke(location); } catch (Exception ignored2) {}
        }
        loadProductImage(imageUrl);

        Log.d(TAG, String.format("UI updated for location: %s, Product: %s, Qty: %d, PDA Verified: %s",
                location.getLocationCode(), location.getProduct(), location.getQuantity(), location.getPdAVerified()));
    }

    /**
     * Helper method to format date strings to show only the date portion (no time)
     * Handles various date formats commonly used in applications
     */
    private String formatDateOnly(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "N/A";
        }

        try {
            // List of common date formats your app might use
            String[] possibleFormats = {
                    "yyyy-MM-dd HH:mm:ss",     // 2024-01-15 10:30:00
                    "yyyy-MM-dd'T'HH:mm:ss",   // 2024-01-15T10:30:00
                    "yyyy-MM-dd'T'HH:mm:ss.SSS", // 2024-01-15T10:30:00.123
                    "yyyy-MM-dd'T'HH:mm:ss'Z'", // 2024-01-15T10:30:00Z
                    "MM/dd/yyyy HH:mm:ss",     // 01/15/2024 10:30:00
                    "dd/MM/yyyy HH:mm:ss",     // 15/01/2024 10:30:00
                    "yyyy-MM-dd",              // 2024-01-15 (already date only)
                    "MM/dd/yyyy",              // 01/15/2024 (already date only)
                    "dd/MM/yyyy"               // 15/01/2024 (already date only)
            };

            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (String format : possibleFormats) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat(format, Locale.getDefault());
                    Date date = inputFormat.parse(dateString);
                    if (date != null) {
                        return outputFormat.format(date);
                    }
                } catch (ParseException e) {
                    // Try next format
                    continue;
                }
            }

            // If no format matches, check if it's already in a simple date format
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateString; // Already in yyyy-MM-dd format
            }

            // If still no match, return the original string but log it
            Log.w(TAG, "Could not parse date format: " + dateString);
            return dateString;

        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + dateString, e);
            return dateString; // Return original if parsing fails
        }
    }

    private void loadProductImage(@Nullable String url) {
        if (productImageView == null) return;

        if (url == null || url.trim().isEmpty()) {
            runOnUiThread(() -> {
                // show placeholder or hide if no URL
                productImageView.setImageResource(R.drawable.ic_placeholder_image);
                // or: productImageView.setVisibility(View.GONE);
            });
            return;
        }

        // Optional: avoid reloading the same URL
        Object currentTag = productImageView.getTag();
        if (currentTag != null && url.equals(currentTag.toString())) return;
        productImageView.setTag(url);

        Request req = new Request.Builder().url(url).get().build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Product image load failed: " + e.getMessage());
                runOnUiThread(() -> {
                    // placeholder on failure
                    productImageView.setImageResource(R.drawable.ic_placeholder_image);
                });
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
        // Now using the activity's current `jobNumber` which is determined in getUserDataAndReferences
        String jobNo = this.jobNumber;
        if (jobNo == null || jobNo.trim().isEmpty()) {
            Log.e(TAG, "Job number missing in TenthActivity instance.");
        }
        return jobNo;
    }

    private String getOrderNoFromSession() {
        // Now using the activity's current `orderNumber` which is determined in getUserDataAndReferences
        String orderNo = this.orderNumber;
        if (orderNo == null || orderNo.trim().isEmpty()) {
            Log.e(TAG, "Order number missing in TenthActivity instance.");
        }
        return orderNo;
    }

    private String getSiteCodeFromSession() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getString("CURRENT_SITE_CODE", "A1");
    }

    private String getLoginIdFromSession() {
        // Now using the activity's current `currentUserId` which is determined in getUserDataAndReferences
        String loginId = this.currentUserId;
        if (loginId == null || loginId.trim().isEmpty()) {
            Log.e(TAG, "Login ID missing in TenthActivity instance.");
        }
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
        Log.d(TAG, "UI set to: Initial State");
        if (btnScanBarcode != null && btnScanBarcode.getVisibility() == View.VISIBLE) {
            btnScanBarcode.requestFocus();
        }
        updateProgressDisplay();
    }

    private void showScanningScreenState() {
        if (previewView != null) previewView.setVisibility(View.VISIBLE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);
        Log.d(TAG, "UI set to: Scanning State");
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
        Log.d(TAG, "UI set to: Scan Result State (Right SKU: " + isRightSku + ")");

        uiHandler.postDelayed(() -> {
            if (isRightSku) {
                if (currentOrderLocation != null) {
                    currentOrderLocation.setPdAVerified("Y");
                    saveLastPickedLocation(currentOrderLocation);
                    Log.d(TAG, "Locally marked location " + currentOrderLocation.getLocationCode() + " as verified (Y).");
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
            Log.d(TAG, "Short quantity requested during scan. Stopping camera.");
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
            if (npSingleDigitInput != null) {
                npSingleDigitInput.requestFocus();
            } else if (btnAddDigit != null) {
                btnAddDigit.requestFocus();
            } else if (btnConfirmShortInput != null) {
                btnConfirmShortInput.requestFocus();
            }
        }
        Log.d(TAG, "UI set to: Short Quantity Input State");
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
        Log.d(TAG, String.format("Added digit: %d, Current number: %s", currentDigit, enteredQuantityBuilder.toString()));

        npSingleDigitInput.setValue(0);
        npSingleDigitInput.requestFocus();
    }

    private void handleBackspaceDigit() {
        if (enteredQuantityBuilder.length() > 0) {
            enteredQuantityBuilder.setLength(enteredQuantityBuilder.length() - 1);
            tvEnteredQuantity.setText(enteredQuantityBuilder.length() == 0 ? "0" : enteredQuantityBuilder.toString());
            Log.d(TAG, "Backspace. Current number: " + tvEnteredQuantity.getText().toString());
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

        int enteredQuantity; // Changed variable name from missingQuantity
        if (enteredQuantityBuilder.length() == 0 || (enteredQuantityBuilder.length() == 1 && enteredQuantityBuilder.charAt(0) == '0')) {
            Toast.makeText(this, "Please enter a quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            enteredQuantity = Integer.parseInt(enteredQuantityBuilder.toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        int originalExpectedQuantity = currentOrderLocation.getQuantity();
        if (enteredQuantity <= 0) {
            Toast.makeText(this, "Quantity must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optional: You can remove this validation if you want to allow quantities higher than expected
        if (enteredQuantity > originalExpectedQuantity) {
            Toast.makeText(this, String.format("Entered quantity (%d) is more than expected (%d). Proceeding anyway.", enteredQuantity, originalExpectedQuantity), Toast.LENGTH_LONG).show();
        }

        // DIRECT ASSIGNMENT - No subtraction, just use the entered value
        int newDisplayedQuantity = enteredQuantity;

        // Update the quantity of the currentOrderLocation object locally
        currentOrderLocation.setQuantity(newDisplayedQuantity);

        // Update the UI to reflect the new quantity
        updateUIWithLocationDetails(currentOrderLocation);

        // Save this updated location as the last picked one.
        saveLastPickedLocation(currentOrderLocation);
        Toast.makeText(this, String.format("Quantity set to: %d. Scan barcode to confirm pick.", newDisplayedQuantity), Toast.LENGTH_LONG).show();
        Log.d(TAG, String.format("Direct quantity entered. Location quantity set to %d. Location remains unverified.", newDisplayedQuantity));

        // Hide the short quantity input and return to the initial screen state
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

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(CONFIRM_ORDER_BASE_URL))
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
                .addHeader("XApiKey", API_KEY)
                .addHeader("accept", "*/*")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API Request failed for picking details", e);
                // Make error message effectively final for the lambda
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(TenthActivity.this, "Network error during picking confirmation: " + errorMessage, Toast.LENGTH_LONG).show();
                    showInitialScreenState();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Picking Details API Response Code: " + response.code());
                Log.d(TAG, "Picking Details API Response Body: " + responseBody);

                // Make responseBody and response.code() effectively final for the lambda
                final String finalResponseBody = responseBody;
                final int finalResponseCode = response.code();
                final boolean finalIsSuccessful = response.isSuccessful(); // Capture isSuccessful state

                runOnUiThread(() -> {
                    if (finalIsSuccessful) { // Use finalIsSuccessful
                        boolean isSuccess = finalResponseBody.toUpperCase().contains("UPDATE SUCCESS") ||
                                finalResponseBody.toUpperCase().contains("SUCCESS");
                        if (isSuccess) {
                            Log.d(TAG, "Picking details confirmed successfully.");
                            if (currentOrderLocation != null) {
                                currentOrderLocation.setPdAVerified("Y");
                                saveLastPickedLocation(currentOrderLocation);
                                Log.d(TAG, "Locally marked location " + currentOrderLocation.getLocationCode() + " as verified (Y) after API success.");
                            }
                            updateProgressDisplay();

                            // This block is only for successful barcode scans now
                            // The `isBarcodeScan` parameter ensures this logic is only triggered by a scan.
                            if (isBarcodeScan) {
                                showScanResultScreenState(true); // Show "RIGHT SKU" and then proceed
                            } else {
                                // This else block should ideally not be reached if short quantity doesn't call API
                                // But if it were, it would proceed without "RIGHT SKU" message.
                                Toast.makeText(TenthActivity.this, "Pick confirmed. Proceeding to next location.", Toast.LENGTH_LONG).show();
                                findNextUnverifiedLocationAndProceed(true);
                            }
                        } else {
                            Toast.makeText(TenthActivity.this, "Picking confirmation failed: " + finalResponseBody, Toast.LENGTH_LONG).show();
                            showInitialScreenState();
                        }
                    } else {
                        Toast.makeText(TenthActivity.this, "Server error during picking confirmation: " + finalResponseCode + " " + finalResponseBody, Toast.LENGTH_SHORT).show();
                        showInitialScreenState();
                    }
                });
            }
        });
    }

    private void findNextUnverifiedLocationAndProceed(boolean startFromNextIndex) {
        if (orderLocations == null || orderLocations.isEmpty()) {
            Log.d(TAG, "findNextUnverifiedLocationAndProceed: orderLocations list is null or empty. Cannot proceed.");
            Toast.makeText(this, "No order locations available. Please use 'Jump To' to load an order.", Toast.LENGTH_LONG).show();
            updateUIWithLocationDetails(null); // Clear UI if no locations
            showInitialScreenState();
            saveLastPickedLocation(null); // Clear last picked if no locations
            return;
        }

        // Update the currentOrderLocation in the main list to reflect its 'Y' status
        // This ensures the main list reflects the confirmed status for progress bar and future navigation
        if (currentOrderLocation != null && currentLocationIndex != -1 && currentLocationIndex < orderLocations.size()) {
            // Ensure the object in the main list is updated, not just the 'currentOrderLocation' reference
            orderLocations.get(currentLocationIndex).setPdAVerified("Y");
        }

        int startIndex = startFromNextIndex ? currentLocationIndex + 1 : 0;
        if (startIndex < 0) startIndex = 0;

        boolean foundNext = false;
        Location nextLocation = null;
        int nextLocationIdx = -1;

        // Iterate through the list to find the first unverified location
        for (int i = startIndex; i < orderLocations.size(); i++) {
            Location location = orderLocations.get(i);
            if (location.getPdAVerified().equalsIgnoreCase("N")) {
                nextLocation = location;
                nextLocationIdx = i;
                foundNext = true;
                break;
            }
        }

        if (foundNext) {
            currentLocationIndex = nextLocationIdx;
            currentOrderLocation = nextLocation;
            updateUIWithLocationDetails(currentOrderLocation);
            saveLastPickedLocation(currentOrderLocation); // Save the new current location
            showInitialScreenState();
            Toast.makeText(TenthActivity.this, "Proceeding to next unverified location: " + currentOrderLocation.getLocationCode(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "Moved to next unverified location: " + currentOrderLocation.getLocationCode());
        } else {
            boolean allLocationsVerified = true;
            for (Location loc : orderLocations) {
                if (loc.getPdAVerified().equalsIgnoreCase("N")) {
                    allLocationsVerified = false;
                    break;
                }
            }

            if (allLocationsVerified) {
                Toast.makeText(TenthActivity.this, "All locations picked. Finishing order.", Toast.LENGTH_LONG).show();
                // No need to send PAUSE API here, as the job is considered 'finished' in backend
                // and should be handled implicitly by backend's job completion logic.

                // When a job is completed, remove the initial total picks and quantity from SharedPreferences
                // as they are no longer needed for a new session of this specific job.
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (jobNumber != null && !jobNumber.isEmpty()) {
                    editor.remove(JOB_INITIAL_TOTAL_PICKS_PREFIX + jobNumber);
                    editor.remove(JOB_INITIAL_TOTAL_QUANTITY_PREFIX + jobNumber);
                    editor.apply();
                    Log.d(TAG, "Removed initial total picks and quantity from SharedPreferences for job: " + jobNumber);

                    // Also clear the "job started" flag for this job as it's completed
                    prefs.edit().remove(JOB_STARTED_PREFIX + jobNumber).apply();
                    Log.d(TAG, "Cleared 'job started' flag for completed job: " + jobNumber);
                }


                Intent intent = new Intent(TenthActivity.this, ConfirmationPageActivity.class);
                intent.putExtra("ORDER_NUMBER", orderNumber);
                intent.putExtra("JOB_NUMBER", jobNumber);
                intent.putExtra("USER_ID", currentUserId);
                intent.putExtra("REFERENCE_CODE", referenceCode); // Ensure these are passed
                intent.putExtra("PRIN_CODE", prinCode); // Ensure these are passed
                intent.putExtra("INITIAL_API_TOTAL_PICKS", initialApiTotalPicks); // Pass initial total picks
                intent.putExtra("INITIAL_API_TOTAL_QUANTITY", initialApiTotalQuantity); // Pass initial total quantity
                startActivity(intent);
                finish(); // Finish TenthActivity
                saveLastPickedLocation(null); // Clear last picked when order is finished
                Log.d(TAG, "No more unverified locations. Navigating to ConfirmationPageActivity.");
            } else {
                Log.w(TAG, "findNextUnverifiedLocationAndProceed: No new unverified location found from current point, but some unverified locations still exist. User may need to manually 'Jump To' a previous location.");
                Toast.makeText(TenthActivity.this, "No more unverified locations found from current point. Use 'Jump To' to select another.", Toast.LENGTH_LONG).show();
                showInitialScreenState();
                // Do not clear last picked location here, as there might be unverified ones elsewhere
            }
        }
        updateProgressDisplay();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted.");
                if (currentOrderLocation != null) { // Check if a location is already loaded
                    startCameraAndScanner();
                } else {
                    // If no location is loaded, and permission granted, try to fetch order details again.
                    // This might happen if permission was denied on first launch, then granted.
                    fetchOrderLocations(true); // Default to fetching and selecting first unverified
                }
            } else {
                Log.w(TAG, "Camera permission denied.");
                Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_LONG).show();
                showInitialScreenState();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, String.format("onActivityResult called. RequestCode: %d, ResultCode: %d", requestCode, resultCode));

        if (requestCode == MENU_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                boolean paused = data.getBooleanExtra("PAUSED", false);
                updateUIForPauseState(paused);
            }
        } else if (requestCode == PICK_LOCATION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<Parcelable> parcelableList = data.getParcelableArrayListExtra(PickLocationActivity.ORDER_LOCATIONS_LIST_KEY);
                int selectedIndex = data.getIntExtra(PickLocationActivity.SELECTED_LOCATION_INDEX_KEY, -1);

                Log.d(TAG, String.format("Received from PickLocationActivity - Selected Index: %d, Parcelable List Size: %d",
                        selectedIndex, (parcelableList != null ? parcelableList.size() : 0)));

                ArrayList<Location> returnedLocations = new ArrayList<>();
                if (parcelableList != null) {
                    for (Parcelable p : parcelableList) {
                        if (p instanceof Location) {
                            returnedLocations.add((Location) p);
                        } else {
                            Log.e(TAG, "Non-Location Parcelable found in ORDER_LOCATIONS list! Type: " + (p != null ? p.getClass().getName() : "null"));
                        }
                    }
                }

                if (!returnedLocations.isEmpty()) {
                    this.orderLocations = returnedLocations; // Update main list with the merged list from PickLocationActivity
                    this.originalTotalLocations = returnedLocations.size(); // Total count is the size of this merged list
                    Log.d(TAG, "Initial originalTotalLocations set to: " + originalTotalLocations);

                    Location selectedLocationFromPicker = null;
                    if (selectedIndex != -1 && selectedIndex < returnedLocations.size()) {
                        selectedLocationFromPicker = returnedLocations.get(selectedIndex);
                    }

                    Location targetLocation = null;
                    int targetIndex = -1;

                    // Prioritize the selected location from PickLocationActivity
                    if (selectedLocationFromPicker != null && selectedLocationFromPicker.getPdAVerified().equalsIgnoreCase("N")) {
                        targetLocation = selectedLocationFromPicker;
                        targetIndex = selectedIndex;
                        Log.d(TAG, "Using selected location from PickLocationActivity: " + targetLocation.getLocationCode());
                    } else {
                        // Fallback: If no valid selection from picker, try to find the first unverified location
                        for (int i = 0; i < returnedLocations.size(); i++) {
                            if (returnedLocations.get(i).getPdAVerified().equalsIgnoreCase("N")) {
                                targetLocation = returnedLocations.get(i);
                                targetIndex = i;
                                Log.d(TAG, "Found first unverified location in returned list: " + targetLocation.getLocationCode());
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
                        Log.d(TAG, String.format("Current Order Location set: %s, Product: %s, Quantity: %d, PDA Verified: %s",
                                currentOrderLocation.getLocationCode(), currentOrderLocation.getProduct(), currentOrderLocation.getQuantity(), currentOrderLocation.getPdAVerified()));
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
                    Log.w(TAG, "PickLocationActivity returned an empty list of unverified locations.");
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
                Log.d(TAG, "PickLocationActivity returned RESULT_CANCELED.");
                // If cancelled and no location was previously set, ensure UI is N/A
                if (currentOrderLocation == null) {
                    updateUIWithLocationDetails(null);
                }
                showInitialScreenState();
                updateProgressDisplay();
            }
        }
    }


    private void updateUIForPauseState(boolean isPaused) {
        if (isPaused) {
            if (isScanning.compareAndSet(true, false)) {
                Log.d(TAG, "External pause requested during scan. Stopping camera.");
                if (mCameraProvider != null) mCameraProvider.unbindAll();
            }
            if (previewView != null) previewView.setVisibility(View.GONE);
            if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
            if (btnScanBarcode != null) btnScanBarcode.setText(getString(R.string.resume_text));
            if (menuLayout != null) menuLayout.setVisibility(View.GONE);
            if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
            if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
            if (pausedStatusText != null) pausedStatusText.setVisibility(View.VISIBLE);
            Log.d(TAG, "UI set to: Paused State");
            if (btnScanBarcode != null && btnScanBarcode.getVisibility() == View.VISIBLE) {
                btnScanBarcode.requestFocus();
            }
        } else {
            showInitialScreenState();
        }
    }

    private void updateProgressDisplay() {
        if (progressBar == null || progressText == null) {
            Log.w(TAG, "Progress bar or text view not initialized. Cannot update progress display.");
            return;
        }

        // Use the stored original total, not the dynamically changing list size
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
            Log.d(TAG, "No locations to track progress (originalTotalLocations is 0). Progress set to 0/0.");
            return;
        }

        progressText.setText(String.format("%d/%d", completedLocations, totalLocations));
        int progressPercentage = (completedLocations * 100) / totalLocations;
        progressBar.setProgress(progressPercentage);
        Log.d(TAG, String.format("Progress updated: %d/%d (%.2f%%)", completedLocations, totalLocations, (float) progressPercentage));
    }

    // NEW: API call methods for PAUSE, RESUME
    private void sendPauseJobPickingApiCall() {
        String loginId = getLoginIdFromSession();
        String orderNo = getOrderNoFromSession();
        String jobNo = getJobNoFromSession();

        if (loginId == null || loginId.isEmpty() ||
                orderNo == null || orderNo.isEmpty() ||
                jobNo == null || jobNo.isEmpty()) {
            Log.w(TAG, "Missing user/order/job details for pause. Skipping PAUSE API call.");
            return;
        }

        // Only send PAUSE if the job was previously marked as "started"
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + jobNo, false);

        if (!hasJobBeenStarted) {
            Log.d(TAG, "Job " + jobNo + " was not marked as started. Skipping PAUSE API call.");
            return;
        }

        try {
            String encodedLoginId = URLEncoder.encode(loginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(orderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(jobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    PAUSE_JOB_PICKING_URL, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8"))) // Empty POST body
                    .addHeader("accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            Log.d(TAG, "Sending PAUSE_JOB_PICKING request from TenthActivity to: " + url);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "PAUSE_JOB_PICKING API call failed (TenthActivity): " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "PAUSE_JOB_PICKING API Response (TenthActivity): " + response.code() + ", Body: " + responseBody);
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Job picking paused successfully (TenthActivity).");
                    } else {
                        Log.e(TAG, "Failed to pause picking (TenthActivity): " + response.message());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing PAUSE_JOB_PICKING request (TenthActivity): " + e.getMessage());
        }
    }

    private void sendResumeJobPickingApiCall() {
        String loginId = getLoginIdFromSession();
        String orderNo = getOrderNoFromSession();
        String jobNo = getJobNoFromSession();

        if (loginId == null || loginId.isEmpty() ||
                orderNo == null || orderNo.isEmpty() ||
                jobNo == null || jobNo.isEmpty()) {
            Log.w(TAG, "Missing user/order/job details for resume. Skipping RESUME API call.");
            return;
        }

        // Only send RESUME if the job was previously marked as "started"
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasJobBeenStarted = prefs.getBoolean(JOB_STARTED_PREFIX + jobNo, false);

        if (!hasJobBeenStarted) {
            Log.d(TAG, "Job " + jobNo + " was not marked as started. Skipping RESUME API call.");
            return;
        }

        try {
            String encodedLoginId = URLEncoder.encode(loginId.trim(), StandardCharsets.UTF_8.toString());
            String encodedOrderNo = URLEncoder.encode(orderNo.trim(), StandardCharsets.UTF_8.toString());
            String encodedJobNo = URLEncoder.encode(jobNo.trim(), StandardCharsets.UTF_8.toString());

            String url = String.format("%s?as_login_id=%s&order_no=%s&job_no=%s",
                    RESUME_JOB_PICKING_URL, encodedLoginId, encodedOrderNo, encodedJobNo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8"))) // Empty POST body
                    .addHeader("accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            Log.d(TAG, "Sending RESUME_JOB_PICKING request from TenthActivity to: " + url);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "RESUME_JOB_PICKING API call failed (TenthActivity): " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "RESUME_JOB_PICKING API Response (TenthActivity): " + response.code() + ", Body: " + responseBody);
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Job picking resumed successfully (TenthActivity).");
                    } else {
                        Log.e(TAG, "Failed to resume picking (TenthActivity): " + response.message());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing RESUME_JOB_PICKING request (TenthActivity): " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);
        // We only explicitly stop camera if scanning and not in short input mode
        if (isScanning.get() && !isChangingConfigurations() && !isShortQuantityInputVisible) {
            Log.d(TAG, "onPause: Actively scanning (and not in short input), stopping camera.");
            stopCameraAndScanner(false);
        }
        // Stop the timer when leaving the activity
        timerHandler.removeCallbacks(timerRunnable); // Stop the timer updates
        Log.d(TAG, "Timer stopped at: " + elapsedTime / 1000 + " seconds");

        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }

        // If activity is being finished (e.g., due to onBackPressed) or if it's going to background
        // and not just a configuration change, and not completing the order
        // We need to check if the activity is finishing (going back to NinthActivity)
        // or just temporarily paused (e.g., home button, app switch).
        // If `isFinishing()` is true, it means onBackPressed was called or finish() was called directly.
        // In this case, NinthActivity's onActivityResult will handle the PAUSE call.
        // If not finishing, it's going to background.
        if (!isFinishing()) {
            Log.d(TAG, "TenthActivity is going to background (not finishing). Sending PAUSE API call.");
            sendPauseJobPickingApiCall();
        } else {
            Log.d(TAG, "TenthActivity is finishing. NinthActivity will handle PAUSE API call via onActivityResult.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);
        boolean isShowingSkuStatus = (skuStatusText != null && skuStatusText.getVisibility() == View.VISIBLE);
        boolean isInPausedState = (pausedStatusText != null && pausedStatusText.getVisibility() == View.VISIBLE);

        Log.d(TAG, String.format("onResume: ShortInputVisible=%b, ScanningFlag=%b, SkuStatusVisible=%b, PausedState=%b, CurrentLocation: %s",
                isShortQuantityInputVisible, isScanning.get(), isShowingSkuStatus, isInPausedState,
                (currentOrderLocation != null ? currentOrderLocation.getLocationCode() : "null")));

        // Send RESUME API call when TenthActivity is brought to foreground
        // This covers cases where the app was sent to background and now returned,
        // or if another activity was over it (like PickLocationActivity) and now it's back.
        // It's important not to send RESUME if `onCreate` has just happened and `fetchOrderLocations` is running.
        // A simple check like `currentOrderLocation != null` implies we've processed an initial load.
        // And importantly, only if the job was previously STARTED (checked within sendResumeJobPickingApiCall)
        if (currentOrderLocation != null && !isShortQuantityInputVisible && !isShowingSkuStatus) {
            sendResumeJobPickingApiCall();
            Log.d(TAG, "TenthActivity resumed. Sending RESUME API call.");
        }


        if (isShortQuantityInputVisible) {
            Log.d(TAG, "onResume: Resuming to Short Quantity Input state. Ensuring focus.");
            View focusedChild = shortQuantityInputContainer.findFocus();
            if (focusedChild == null) {
                if (npSingleDigitInput != null && npSingleDigitInput.getVisibility() == View.VISIBLE) {
                    npSingleDigitInput.requestFocus();
                } else if (btnAddDigit != null && btnAddDigit.getVisibility() == View.VISIBLE) {
                    btnAddDigit.requestFocus();
                } else if (btnConfirmShortInput != null && btnConfirmShortInput.getVisibility() == View.VISIBLE) {
                    btnConfirmShortInput.requestFocus();
                }
            }
            if (enteredQuantityBuilder.length() > 0) {
                tvEnteredQuantity.setText(enteredQuantityBuilder.toString());
            } else {
                tvEnteredQuantity.setText("0");
            }
        } else if (isScanning.get() && !isShowingSkuStatus && !isInPausedState &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onResume: Was scanning and not showing status/paused. Restarting camera.");
            startCameraAndScanner();
        } else if (!isShowingSkuStatus && !isInPausedState) {
            Log.d(TAG, "onResume: Not in special state. Ensuring initial screen state.");
            showInitialScreenState();
        } else if (isInPausedState) {
            Log.d(TAG, "onResume: Resuming to Paused state.");
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
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        Log.d(TAG, "onDestroy called.");
    }
}