package com.example.supportapp.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Date;

public class Welcomeactivity extends AppCompatActivity {

    private static final String TAG = "TrialCheck";
    private static final long DELAY_MILLIS = 5000; // 5 seconds

    // Firestore config document
    private static final String CONFIG_COLLECTION = "appConfig";
    private static final String CONFIG_DOC_ID = "supportAppTrial";

    private VuzixSpeechClient speechClient;


    private Handler delayHandler;
    private Runnable delayRunnable;

    private TextView welcomeText;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifth);

        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);

        // Disable logout until we know trial status
        logoutButton.setEnabled(false);
        welcomeText.setText("Checking trial status...");

        // 1) Check if trial is still valid
        checkTrialStatus();
        setupVoiceCommands();

    }
    private void setupVoiceCommands() {
        try {

            speechClient = new VuzixSpeechClient(this);

            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");




            Log.d(TAG, "Voice commands registered");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed: " + e.getMessage());
        }
    }

    /**
     * Fetches trial configuration from Firestore and decides whether to allow access or lock the app.
     */
    private void checkTrialStatus() {
        FirebaseFirestore.getInstance()
                .collection(CONFIG_COLLECTION)
                .document(CONFIG_DOC_ID)
                // IMPORTANT: force SERVER to avoid stale cached data
                .get(Source.SERVER)
                .addOnSuccessListener(this::handleTrialConfig)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load trial config from Firestore", e);

                    // SAFER: if we can't verify, DON'T allow app by default.
                    showTrialExpiredDialog("Unable to verify trial. Please check your internet connection or contact support.");
                });
    }

    /**
     * Handles the trial configuration read from Firestore.
     */
    private void handleTrialConfig(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            Log.w(TAG, "Trial config document does not exist. Blocking access by default.");
            showTrialExpiredDialog("Trial configuration not found. Please contact support.");
            return;
        }

        Boolean enabled = snapshot.getBoolean("enabled");
        Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
        String lockMessage = snapshot.getString("lockMessage");

        long now = System.currentTimeMillis();
        Date nowDate = new Date(now);

        Log.d(TAG, "Config loaded: enabled=" + enabled +
                ", expiresAt=" + (expiresAt != null ? expiresAt.toDate() : "null") +
                ", now=" + nowDate);

        if (Boolean.TRUE.equals(enabled) && expiresAt != null) {
            long expiryMillis = expiresAt.toDate().getTime();

            Log.d(TAG, "nowMillis=" + now + "  expiryMillis=" + expiryMillis);

            if (now > expiryMillis) {
                // Trial expired → show popup and prevent access.
                Log.d(TAG, "Trial EXPIRED. Blocking user.");
                showTrialExpiredDialog(lockMessage);
                return;
            } else {
                Log.d(TAG, "Trial still valid. Allowing user.");
            }
        } else {
            Log.d(TAG, "Trial not enabled or expiresAt is null. Allowing user.");
        }

        // If not enabled, or no expiry set, or not expired → proceed normally.
        initNormalFlow();
    }

    /**
     * Normal flow: show welcome text and auto-navigate to SixthActivity after 5 seconds.
     */
    private void initNormalFlow() {
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = "User";
        }

        String welcomeMessage = "HI " + userName + "\n\nNice to see you again";
        welcomeText.setText(welcomeMessage);

        logoutButton.setEnabled(true);

        // Logout button logic
        logoutButton.setOnClickListener(v -> {
            // Cancel auto navigation if logout clicked
            if (delayHandler != null && delayRunnable != null) {
                delayHandler.removeCallbacks(delayRunnable);
            }

            // Use your existing LogoutManager
            LogoutManager.performLogout(Welcomeactivity.this);
        });

        // Auto navigation to SixthActivity after delay
        delayHandler = new Handler(Looper.getMainLooper());
        delayRunnable = () -> {
            Intent intent = new Intent(Welcomeactivity.this, ProcessActivity.class);
            startActivity(intent);
            finish();
        };

        delayHandler.postDelayed(delayRunnable, DELAY_MILLIS);
    }

    /**
     * Shows "trial expired" popup and prevents the user from going further.
     */
    private void showTrialExpiredDialog(String messageFromServer) {
        String message = messageFromServer;
        if (message == null || message.trim().isEmpty()) {
            message = "Your trial period has expired. Please contact support.";
        }

        welcomeText.setText("Trial expired");

        new AlertDialog.Builder(this)
                .setTitle("Trial Expired")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Send user back to login or close app
                    LogoutManager.performLogout(Welcomeactivity.this);
                    // Or: finishAffinity();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler to prevent memory leaks
        if (delayHandler != null && delayRunnable != null) {
            delayHandler.removeCallbacks(delayRunnable);
        }
    }
}
