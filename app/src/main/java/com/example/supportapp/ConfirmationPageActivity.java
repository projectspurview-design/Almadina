package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

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

    // Voice actions routed via central VoiceCommandCenter (no direct Vuzix client here)
    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onNext()     { proceedToOrderSummary(); }
        @Override public void onSelect()   { proceedToOrderSummary(); }
        @Override public void onLogout()   { LogoutManager.performLogout(ConfirmationPageActivity.this); }
        @Override public void onBack()     { /* Block back on this screen (order completed) */ }

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

        // Centralized voice registration (idempotent)
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
            Log.e(TAG, "Next button not found in layout!");
        }
        if (logoutButton == null) {
            Log.e(TAG, "Logout button not found in layout!");
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
        }
    }

    private void setupButtonListeners() {
        // Logout button listener
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                LogoutManager.performLogout(ConfirmationPageActivity.this);
            });
        }

        // Next button listener
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Log.d(TAG, "Next button clicked - proceeding to OrderSummaryActivity");
                proceedToOrderSummary();
            });
        }
    }

    private void proceedToOrderSummary() {
        try {
            Log.d(TAG, "Creating intent for OrderSummaryActivity");
            Intent intent = new Intent(ConfirmationPageActivity.this, OrderSummaryActivity.class);

            // Pass all data to OrderSummaryActivity
            intent.putExtra("ORDER_NUMBER", orderNumber);
            intent.putExtra("JOB_NUMBER", jobNumber);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("REFERENCE_CODE", referenceCode);
            intent.putExtra("PRIN_CODE", prinCode);
            intent.putExtra("INITIAL_API_TOTAL_PICKS", initialApiTotalPicks);
            intent.putExtra("INITIAL_API_TOTAL_QUANTITY", initialApiTotalQuantity);

            Log.d(TAG, "Starting OrderSummaryActivity with data:");
            Log.d(TAG, "Order: " + orderNumber + ", Job: " + jobNumber + ", User: " + userId);

            startActivity(intent);
            finish(); // Finish current activity

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to OrderSummaryActivity: " + e.getMessage());
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
        Log.d(TAG, "Key pressed: " + keyCode);

        // Block hardware back explicitly
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "Hardware back button blocked - Order completed");
            return true;
        }

        // Route key events to centralized voice handler
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
