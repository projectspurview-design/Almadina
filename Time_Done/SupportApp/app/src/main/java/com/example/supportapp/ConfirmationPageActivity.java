package com.example.supportapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class ConfirmationPageActivity extends AppCompatActivity {
    // Custom voice command key codes (avoiding system key codes)
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter; // This adapter seems unused in this activity, might be a leftover.

    private VuzixSpeechClient speechClient;

    // --- ADD THESE INSTANCE VARIABLES ---
    private String orderNumber;
    private String jobNumber;
    private String userId; // This will be your as_login_id
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;
    private int initialApiTotalPicks;   // MODIFIED: Changed to int
    private int initialApiTotalQuantity; // MODIFIED: Changed to int
    // ------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_page);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(ConfirmationPageActivity.this);
        });

        // --- RETRIEVE DATA FROM INTENT ---
        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            orderNumber = incomingIntent.getStringExtra("ORDER_NUMBER");
            jobNumber = incomingIntent.getStringExtra("JOB_NUMBER");
            userId = incomingIntent.getStringExtra("USER_ID");
            initialApiTotalPicks = incomingIntent.getIntExtra("INITIAL_API_TOTAL_PICKS", 0);   // MODIFIED: getIntExtra
            initialApiTotalQuantity = incomingIntent.getIntExtra("INITIAL_API_TOTAL_QUANTITY", 0); // MODIFIED: getIntExtra
            Log.d(TAG, "Received from TenthActivity - Order: " + orderNumber + ", Job: " + jobNumber + ", User: " + userId);
            Log.d(TAG, "Received from TenthActivity - Initial API Total Picks: " + initialApiTotalPicks + ", Total Quantity: " + initialApiTotalQuantity); // NEW
        }
        // ---------------------------------

        // Setup custom voice commands
        setupVoiceCommands();

        // Next button - add your logic here
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmationPageActivity.this, OrderSummaryActivity.class);
            // --- PASS DATA ALONG TO ORDER SUMMARY ACTIVITY ---
            intent.putExtra("ORDER_NUMBER", orderNumber);
            intent.putExtra("JOB_NUMBER", jobNumber);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("INITIAL_API_TOTAL_PICKS", initialApiTotalPicks);   // MODIFIED: Passing int
            intent.putExtra("INITIAL_API_TOTAL_QUANTITY", initialApiTotalQuantity); // MODIFIED: Passing int
            // -------------------------------------------------
            startActivity(intent);
            finish(); // Finish ConfirmationPageActivity after navigating
        });

    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);
            // Selection commands - if any, ensure they handle `selectedPosition` correctly if used.
            Log.d(TAG, "Custom voice commands initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient: " + e.getMessage());
        }
    }

    private void goToNextIfIndividual() {
        if (selectedPosition == 0) { // This condition implies "Pick - Individual" is selected
            Intent intent = new Intent(ConfirmationPageActivity.this, OrderSummaryActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            // --- PASS DATA ALONG TO ORDER SUMMARY ACTIVITY (for voice command flow) ---
            intent.putExtra("ORDER_NUMBER", orderNumber);
            intent.putExtra("JOB_NUMBER", jobNumber);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("INITIAL_API_TOTAL_PICKS", initialApiTotalPicks);   // MODIFIED: Passing int
            intent.putExtra("INITIAL_API_TOTAL_QUANTITY", initialApiTotalQuantity); // MODIFIED: Passing int
            // -------------------------------------------------------------------------
            startActivity(intent);
            finish(); // Finish ConfirmationPageActivity after navigating via voice command
            Log.d(TAG, "Navigating to OrderSummaryActivity via voice command"); // Corrected log message
        } else {
            Log.d(TAG, "Cannot proceed - Pick Individual not selected");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle custom voice commands
        switch (keyCode) {
            case KEYCODE_NEXT_VOICE:
                goToNextIfIndividual();
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(ConfirmationPageActivity.this);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}