package com.example.supportapp.Pick_Consolidated;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.supportapp.ConfirmationPageActivity;
import com.example.supportapp.LogoutManager;
import com.example.supportapp.OrderSummaryActivity;
import com.example.supportapp.R;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class OrdecompleteconsolidatedActivity extends AppCompatActivity {

    // Custom voice command key codes (avoiding system key codes)
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;

    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private VuzixSpeechClient speechClient;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_page);

        // Initialize UI components
        initializeUI();

        // Retrieve data from intent
        retrieveIntentData();

        // Setup voice commands
        setupVoiceCommands();

        // Setup button listeners
        setupButtonListeners();
    }

    private void initializeUI() {
        nextButton = findViewById(R.id.btnNext);
        logoutButton = findViewById(R.id.logoutButton);

        // Ensure buttons are found
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
                LogoutManager.performLogout(OrdecompleteconsolidatedActivity.this);
            });
        }

        // Next button listener - THIS IS THE FIX
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Log.d(TAG, "Next button clicked - proceeding to CompleteordersummaryconsolidatedActivity");
                proceedToOrderSummary();
            });
        }
    }

    private void proceedToOrderSummary() {
        try {
            Log.d(TAG, "Creating intent for OrderSummaryActivity");
            Intent intent = new Intent(OrdecompleteconsolidatedActivity.this, OrderSummaryActivity.class);

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

        // Handle custom voice commands
        switch (keyCode) {
            case KEYCODE_NEXT_VOICE:
                Log.d(TAG, "Next voice command triggered");
                proceedToOrderSummary();
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(OrdecompleteconsolidatedActivity.this);
                return true;

            // BLOCK BACK BUTTON
            case KeyEvent.KEYCODE_BACK:
                Log.d(TAG, "Hardware back button blocked - Order completed");
                return true; // Consume the event, don't let it propagate
        }

        return super.onKeyDown(keyCode, event);
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);

            Log.d(TAG, "Custom voice commands initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up speech client if needed
        if (speechClient != null) {
            try {
                speechClient = null;
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up speech client: " + e.getMessage());
            }
        }
    }
}