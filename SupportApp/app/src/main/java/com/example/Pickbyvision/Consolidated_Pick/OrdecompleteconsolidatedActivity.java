package com.example.Pickbyvision.Consolidated_Pick;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedPickDetail;
import com.example.Pickbyvision.R;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class OrdecompleteconsolidatedActivity extends AppCompatActivity {

    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;

    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private VuzixSpeechClient speechClient;

    // Data variables
    private String orderNumber;
    private String jobNumber;
    private String userId;
    private String companyCode;
    private String prinCode;
    private int totalItems;
    // 🔹 NEW
    private String batchId;
    private String loginId;
    private int completedItems;
    private int successfulPicksCount;
    private int totalQuantitySum;

    private ConsolidatedPickDetail pickDetail;

    // UI Components
    private Button nextButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_page);

        initializeUI();
        retrieveIntentData();
        setupVoiceCommands();
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
            pickDetail = incomingIntent.getParcelableExtra("PICK_DETAIL");
            companyCode = incomingIntent.getStringExtra("COMPANY_CODE");
            prinCode = incomingIntent.getStringExtra("PRIN_CODE");
            userId = incomingIntent.getStringExtra("PICK_USER");
            totalItems = incomingIntent.getIntExtra("TOTAL_ITEMS", 0);
            completedItems = incomingIntent.getIntExtra("COMPLETED_ITEMS", 0);
            jobNumber = incomingIntent.getStringExtra("JOB_NO");
            orderNumber = incomingIntent.getStringExtra("ORDER_NO");

            batchId = incomingIntent.getStringExtra("BATCH_ID");
            loginId = incomingIntent.getStringExtra("LOGIN_ID");
            if (loginId == null) loginId = userId; // optional fallback
            // 🔹 Fallback: if ORDER_NO is empty, try from pickDetail
            if ((orderNumber == null || orderNumber.trim().isEmpty()) && pickDetail != null) {
                String detailOrderNo = pickDetail.getOrderNo();
                if (detailOrderNo != null && !detailOrderNo.trim().isEmpty()) {
                    orderNumber = detailOrderNo.trim();
                }
            }

            Log.d(TAG, "Received data - Order: " + orderNumber + ", Job: " + jobNumber + ", User: " + userId);


            // ✅ NEW: Get picks count and total quantity
            successfulPicksCount = incomingIntent.getIntExtra("SUCCESSFUL_PICKS_COUNT", 0);
            totalQuantitySum = incomingIntent.getIntExtra("TOTAL_QUANTITY_SUM", 0);

            Log.d(TAG, "Received data - Order: " + orderNumber + ", Job: " + jobNumber + ", User: " + userId);
            Log.d(TAG, "Received data - Company: " + companyCode + ", Prin: " + prinCode);
            Log.d(TAG, "Received data - Total Items: " + totalItems + ", Completed: " + completedItems);
            Log.d(TAG, "Received data - Successful Picks: " + successfulPicksCount + ", Total Quantity: " + totalQuantitySum);

            if (pickDetail != null) {
                Log.d(TAG, "Pick Detail - Location: " + pickDetail.getLocationCode() +
                        ", Product: " + pickDetail.getProdCode() +
                        ", Quantity: " + pickDetail.getQuantity());
            }
        }
    }

    private void setupButtonListeners() {
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                LogoutManager.performLogout(OrdecompleteconsolidatedActivity.this);
            });
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Log.d(TAG, "Next button clicked - proceeding to CompletedordersummaryconsolidatedActivity");
                proceedToOrderSummary();
            });
        }
    }

    private void proceedToOrderSummary() {
        try {
            Log.d(TAG, "Creating intent for CompletedordersummaryconsolidatedActivity");
            Intent intent = new Intent(
                    OrdecompleteconsolidatedActivity.this,
                    CompletedordersummaryconsolidatedActivity.class
            );

            String safeOrder = (orderNumber == null) ? "" : orderNumber;
            String safeJob   = (jobNumber   == null) ? "" : jobNumber;
            String safeUser  = (userId      == null) ? "" : userId;

            intent.putExtra("ORDER_NO",  safeOrder);
            intent.putExtra("JOB_NO",    safeJob);
            intent.putExtra("PICK_USER", safeUser);

            intent.putExtra("ORDER_NUMBER", safeOrder);
            intent.putExtra("JOB_NUMBER",   safeJob);
            intent.putExtra("USER_ID",      safeUser);

            intent.putExtra("COMPANY_CODE", companyCode);
            intent.putExtra("PRIN_CODE",    prinCode);
            intent.putExtra("TOTAL_ITEMS",  totalItems);
            intent.putExtra("COMPLETED_ITEMS", completedItems);

            // 🔹 NEW: pass through to summary
            intent.putExtra("BATCH_ID", batchId);
            intent.putExtra("LOGIN_ID", loginId);

            // Counts
            intent.putExtra("SUCCESSFUL_PICKS_COUNT", successfulPicksCount);
            intent.putExtra("TOTAL_QUANTITY_SUM",     totalQuantitySum);

            if (pickDetail != null) {
                intent.putExtra("PICK_DETAIL", pickDetail);
            }

            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to CompletedordersummaryconsolidatedActivity: " + e.getMessage());
        }
    }



    @Override
    public void onBackPressed() {

        // If camera/scanner is open -> just close scanner UI


        // If Short UI is open -> close it (optional, but recommended)


        // 🚫 Block navigation to previous screen (hardware back included)
        Log.d(TAG, "Back pressed – blocked (no navigation)");
        Toast.makeText(this, "Back disabled on this screen", Toast.LENGTH_SHORT).show();

        // DO NOT call super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key pressed: " + keyCode);

        switch (keyCode) {
            case KEYCODE_NEXT_VOICE:
                Log.d(TAG, "Next voice command triggered");
                proceedToOrderSummary();
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(OrdecompleteconsolidatedActivity.this);
                return true;

            case KeyEvent.KEYCODE_BACK:
                Log.d(TAG, "Hardware back button blocked - Order completed");
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.deletePhrase("BACK");
            speechClient.deletePhrase("GO BACK");

            speechClient.deletePhrase("Go Back");
            speechClient.deletePhrase("Logout");
            speechClient.deletePhrase("Log Out");
            speechClient.deletePhrase("Next");

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
        if (speechClient != null) {
            try {
                speechClient = null;
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up speech client: " + e.getMessage());
            }
        }
    }


}