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

public class NextOrderReadyActivity extends AppCompatActivity {

    // Custom voice command key codes (avoiding system key codes)
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

    private VuzixSpeechClient speechClient;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_order_ready);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(NextOrderReadyActivity.this);
        });

        // Setup custom voice commands
        setupVoiceCommands();



        // Next button logic to go back to pick the next job (EighthActivity)
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            // Transition back to EighthActivity to pick the next job
            Intent intent = new Intent(NextOrderReadyActivity.this,EighthActivity.class); // <--- CORRECTED: Now goes to EighthActivity
            startActivity(intent);
            finish(); // Close this activity to remove it from the stack
        });
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);


            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);

            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);

            // Selection commands

            Log.d(TAG, "Custom voice commands initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient: " + e.getMessage());
        }
    }

    private void goToNextIfIndividual() {
        if (selectedPosition == 0) {
            Intent intent = new Intent(NextOrderReadyActivity.this, EighthActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
            Log.d(TAG, "Navigating to EighthActivity");
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
                LogoutManager.performLogout(NextOrderReadyActivity.this);
                return true;

        }

        return super.onKeyDown(keyCode, event);
    }
}
