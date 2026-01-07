package com.example.supportapp.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.R;
import com.example.supportapp.voice.VoiceCommandCenter;

import java.util.Arrays;
import java.util.List;

public class ConfirmationPageActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmationPageActivity";

    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;

    // --- INSTANCE VARIABLES FOR ALL DATA ---
    private String orderNumber;
    private String jobNumber;
    private String userId; // This will be your as_login_id
    private String referenceCode;
    private String prinCode;
    private int initialApiTotalPicks;
    private int initialApiTotalQuantity;

    // UI Components
    private Button nextButton;
    private Button logoutButton;

    // Voice actions routed via central VoiceCommandCenter
    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override
        public void onNext() {
            Log.d(TAG, "Voice command: NEXT triggered");
            runOnUiThread(() -> proceedToOrderSummary());
        }

        @Override
        public void onSelect() {
            Log.d(TAG, "Voice command: SELECT triggered");
            runOnUiThread(() -> proceedToOrderSummary());
        }

        @Override
        public void onLogout() {
            Log.d(TAG, "Voice command: LOGOUT triggered");
            runOnUiThread(() -> LogoutManager.performLogout(ConfirmationPageActivity.this));
        }

        @Override
        public void onBack() {
            Log.d(TAG, "Voice command: BACK blocked (order completed)");
            // Block back on this screen (order completed)
        }

        // Unused on this screen but required by interface
        @Override public void onScrollUp() {}
        @Override public void onScrollDown() {}
        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_page);

        Log.d(TAG, "onCreate: Activity starting");

        // Centralized voice initialization
        VoiceCommandCenter.init(this);

        // Initialize UI components
        initializeUI();

        // Retrieve data from intent
        retrieveIntentData();

        // Setup button listeners
        setupButtonListeners();
    }

    private void initializeUI() {
        nextButton = findViewById(R.id.btnNext);
        logoutButton = findViewById(R.id.logoutButton);

        if (nextButton == null) {
            Log.e(TAG, "ERROR: Next button not found in layout!");
        } else {
            Log.d(TAG, "Next button found successfully");
            Log.d(TAG, "Button enabled: " + nextButton.isEnabled());
            Log.d(TAG, "Button clickable: " + nextButton.isClickable());
            Log.d(TAG, "Button visible: " + (nextButton.getVisibility() == android.view.View.VISIBLE));

            // Ensure button is enabled and clickable
            nextButton.setEnabled(true);
            nextButton.setClickable(true);
            nextButton.setFocusable(true);
        }

        if (logoutButton == null) {
            Log.e(TAG, "ERROR: Logout button not found in layout!");
        } else {
            Log.d(TAG, "Logout button found successfully");
        }
    }

    private void retrieveIntentData() {
        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            orderNumber = incomingIntent.getStringExtra("ORDER_NUMBER");
            jobNumber = incomingIntent.getStringExtra("JOB_NUMBER");
            userId = incomingIntent.getStringExtra("USER_ID");
            referenceCode = incomingIntent.getStringExtra("REFERENCE_CODE");
            prinCode = incomingIntent.getStringExtra("PRIN_CODE");
            initialApiTotalPicks = incomingIntent.getIntExtra("INITIAL_API_TOTAL_PICKS", 0);
            initialApiTotalQuantity = incomingIntent.getIntExtra("INITIAL_API_TOTAL_QUANTITY", 0);

            Log.d(TAG, "Received data - Order: " + orderNumber + ", Job: " + jobNumber + ", User: " + userId);
            Log.d(TAG, "Received data - Reference: " + referenceCode + ", Prin: " + prinCode);
            Log.d(TAG, "Received data - Initial Picks: " + initialApiTotalPicks + ", Quantity: " + initialApiTotalQuantity);
        } else {
            Log.w(TAG, "No intent data received");
        }
    }

    private void setupButtonListeners() {
        // Next button listener
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Log.d(TAG, "=== NEXT BUTTON CLICKED ===");
                Log.d(TAG, "Button click registered, calling proceedToOrderSummary()");
                proceedToOrderSummary();
            });

            // Also set OnTouchListener for additional debugging
            nextButton.setOnTouchListener((v, event) -> {
                Log.d(TAG, "Next button touched: " + event.getAction());
                return false; // Return false to allow click to proceed
            });

            Log.d(TAG, "Next button listener attached successfully");
        } else {
            Log.e(TAG, "Cannot attach listener - Next button is null!");
        }

        // Logout button listener
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                LogoutManager.performLogout(ConfirmationPageActivity.this);
            });
            Log.d(TAG, "Logout button listener attached successfully");
        }
    }

    private void proceedToOrderSummary() {
        try {
            Log.d(TAG, "=== PROCEEDING TO ORDER SUMMARY ===");
            Log.d(TAG, "Creating intent for CompleteordersummaryActivity");

            Intent intent = new Intent(ConfirmationPageActivity.this, CompleteordersummaryActivity.class);

            // Pass all data to OrderSummaryActivity
            intent.putExtra("ORDER_NUMBER", orderNumber);
            intent.putExtra("JOB_NUMBER", jobNumber);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("REFERENCE_CODE", referenceCode);
            intent.putExtra("PRIN_CODE", prinCode);
            intent.putExtra("INITIAL_API_TOTAL_PICKS", initialApiTotalPicks);
            intent.putExtra("INITIAL_API_TOTAL_QUANTITY", initialApiTotalQuantity);

            Log.d(TAG, "Intent data prepared:");
            Log.d(TAG, "  Order: " + orderNumber);
            Log.d(TAG, "  Job: " + jobNumber);
            Log.d(TAG, "  User: " + userId);
            Log.d(TAG, "  Reference: " + referenceCode);
            Log.d(TAG, "  Prin: " + prinCode);
            Log.d(TAG, "  Initial Picks: " + initialApiTotalPicks);
            Log.d(TAG, "  Initial Quantity: " + initialApiTotalQuantity);

            Log.d(TAG, "Starting CompleteordersummaryActivity...");
            startActivity(intent);

            Log.d(TAG, "Finishing ConfirmationPageActivity");
            finish(); // Finish current activity

        } catch (Exception e) {
            Log.e(TAG, "ERROR navigating to OrderSummaryActivity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // PREVENT BACK NAVIGATION - ORDER IS COMPLETED
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed - Navigation blocked (order completed)");
        // Do nothing - user cannot go back as order is completed
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key pressed: " + keyCode + " (KeyEvent: " + KeyEvent.keyCodeToString(keyCode) + ")");

        // Block hardware back explicitly
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "Hardware back button blocked - Order completed");
            return true;
        }

        // Route key events to centralized voice handler
        boolean handled = VoiceCommandCenter.handleKeyDown(keyCode, voiceActions);
        if (handled) {
            Log.d(TAG, "Key event handled by VoiceCommandCenter");
            return true;
        }

        Log.d(TAG, "Key event not handled, passing to super");
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed");
    }
}