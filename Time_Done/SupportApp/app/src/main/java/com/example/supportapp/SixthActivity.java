package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class SixthActivity extends AppCompatActivity {

    private static final String TAG = "SixthActivity";

    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<String> options = Arrays.asList("Inbound", "Outbound", "Inventory");
    private int selectedPosition = 1; // Initially "Outbound"
    private OptionAdapter adapter;

    private VuzixSpeechClient speechClient;

    // Custom voice command key codes (avoiding system key codes)
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_SCROLLUP_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_SCROLLDOWN_VOICE = KeyEvent.KEYCODE_F3;
    private static final int KEYCODE_SELECT_VOICE = KeyEvent.KEYCODE_F4;
    private static final int KEYCODE_INBOUND_VOICE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_OUTBOUND_VOICE = KeyEvent.KEYCODE_F6;
    private static final int KEYCODE_INVENTORY_VOICE = KeyEvent.KEYCODE_F7;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sixth);

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button nextButton = findViewById(R.id.btnNext);
        Button logoutButton = findViewById(R.id.logoutButton);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        optionsRecyclerView.setLayoutManager(layoutManager);

        adapter = new OptionAdapter(options);
        optionsRecyclerView.setAdapter(adapter);

        // Set initial selection
        adapter.setSelectedPosition(selectedPosition);
        optionsRecyclerView.scrollToPosition(selectedPosition);

        // Button Listeners
        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());
        nextButton.setOnClickListener(v -> goToNextIfOutbound());

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(SixthActivity.this);
        });

        setupVoiceCommands();
    }

    private void setupBubbleNumbers() {
        try {
            // Enable bubble numbers on buttons
            // The bubble system will automatically assign numbers to clickable views

            // You can also manually assign bubble numbers if needed:
            // findViewById(R.id.btnNext).setContentDescription("1");
            // findViewById(R.id.arrowUp).setContentDescription("2");
            // findViewById(R.id.arrowDown).setContentDescription("3");
            // findViewById(R.id.logoutButton).setContentDescription("4");

            Log.d(TAG, "Bubble number system enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bubble numbers: " + e.getMessage());
        }
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            // Navigation commands
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);

            speechClient.insertKeycodePhrase("Scroll Up", KEYCODE_SCROLLUP_VOICE);

            speechClient.insertKeycodePhrase("Scroll Down", KEYCODE_SCROLLDOWN_VOICE);

            // Selection commands
            speechClient.insertKeycodePhrase("Select", KEYCODE_SELECT_VOICE);

            // Direct option selection
            speechClient.insertKeycodePhrase("Inbound", KEYCODE_INBOUND_VOICE);

            speechClient.insertKeycodePhrase("Outbound", KEYCODE_OUTBOUND_VOICE);

            speechClient.insertKeycodePhrase("Inventory", KEYCODE_INVENTORY_VOICE);

            // Logout command
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);

            Log.d(TAG, "Custom voice commands initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient: " + e.getMessage());
        }
    }

    private void moveUp() {
        if (selectedPosition > 0) {
            selectedPosition--;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Moved up to position: " + selectedPosition + " (" + options.get(selectedPosition) + ")");
        }
    }

    private void moveDown() {
        if (selectedPosition < options.size() - 1) {
            selectedPosition++;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Moved down to position: " + selectedPosition + " (" + options.get(selectedPosition) + ")");
        }
    }

    private void selectCurrentOption() {
        String selectedOption = options.get(selectedPosition);
        Log.d(TAG, "Selected option: " + selectedOption);

        // If Outbound is selected, go to next activity
        if (selectedPosition == 1) {
            goToNextIfOutbound();
        } else {
            // Handle other options if needed
            Log.d(TAG, "Option selected: " + selectedOption);
            // You can add specific logic for Inbound and Inventory here
        }
    }

    private void selectOptionDirectly(int optionIndex) {
        if (optionIndex >= 0 && optionIndex < options.size()) {
            selectedPosition = optionIndex;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Directly selected: " + options.get(selectedPosition));
        }
    }

    private void goToNextIfOutbound() {
        if (selectedPosition == 1) {
            Intent intent = new Intent(SixthActivity.this, SeventhActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
            Log.d(TAG, "Navigating to SeventhActivity with option: " + options.get(selectedPosition));
        } else {
            Log.d(TAG, "Cannot proceed - Outbound not selected");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle custom voice commands
        switch (keyCode) {
            case KEYCODE_NEXT_VOICE:
                goToNextIfOutbound();
                return true;

            case KEYCODE_SCROLLUP_VOICE:
                moveUp();
                return true;

            case KEYCODE_SCROLLDOWN_VOICE:
                moveDown();
                return true;

            case KEYCODE_SELECT_VOICE:
                selectCurrentOption();
                return true;

            case KEYCODE_INBOUND_VOICE:
                selectOptionDirectly(0); // Inbound
                return true;

            case KEYCODE_OUTBOUND_VOICE:
                selectOptionDirectly(1); // Outbound
                return true;

            case KEYCODE_INVENTORY_VOICE:
                selectOptionDirectly(2); // Inventory
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(SixthActivity.this);
                return true;
        }

        // Handle bubble number system (system will handle these automatically)
        // Numbers 1-9 will automatically trigger click events on numbered views

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // speechClient.shutdown(); // Not needed in latest SDK
    }
}