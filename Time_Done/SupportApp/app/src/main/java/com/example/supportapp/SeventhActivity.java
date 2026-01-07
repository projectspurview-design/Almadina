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

import com.example.supportapp.Pick_Consolidated.ConsolidatedTransactionActivity;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class SeventhActivity extends AppCompatActivity {

    private static final String TAG = "SeventhActivity";

    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

    private VuzixSpeechClient speechClient;

    // Custom voice command key codes (avoiding system key codes)
    private static final int KEYCODE_BACK_VOICE = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_SCROLL_UP_VOICE = KeyEvent.KEYCODE_F3;
    private static final int KEYCODE_SCROLL_DOWN_VOICE = KeyEvent.KEYCODE_F4;
    private static final int KEYCODE_SELECT_VOICE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_INDIVIDUAL_VOICE = KeyEvent.KEYCODE_F6;
    private static final int KEYCODE_CONSOLIDATED_VOICE = KeyEvent.KEYCODE_F7;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seventh);

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button backButton = findViewById(R.id.btnBack);
        Button nextButton = findViewById(R.id.btnNext);
        Button logoutButton = findViewById(R.id.logoutButton);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        optionsRecyclerView.setLayoutManager(layoutManager);

        adapter = new OptionAdapter(options);
        optionsRecyclerView.setAdapter(adapter);

        adapter.setSelectedPosition(selectedPosition);
        optionsRecyclerView.scrollToPosition(selectedPosition);

        // Button Listeners
        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());
        backButton.setOnClickListener(v -> goBack());
        nextButton.setOnClickListener(v -> goToNext()); // Changed this line

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(SeventhActivity.this);
        });

        // Enable bubble numbers for buttons
        setupBubbleNumbers();

        // Setup custom voice commands
        setupVoiceCommands();
    }

    private void setupBubbleNumbers() {
        try {
            // Enable bubble numbers on buttons
            // The bubble system will automatically assign numbers to clickable views

            // You can also manually assign bubble numbers if needed:
            // findViewById(R.id.btnBack).setContentDescription("1");
            // findViewById(R.id.btnNext).setContentDescription("2");
            // findViewById(R.id.arrowUp).setContentDescription("3");
            // findViewById(R.id.arrowDown).setContentDescription("4");
            // findViewById(R.id.logoutButton).setContentDescription("5");

            Log.d(TAG, "Bubble number system enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bubble numbers: " + e.getMessage());
        }
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            // Navigation commands
            speechClient.insertKeycodePhrase("Back", KEYCODE_BACK_VOICE);
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);
            speechClient.insertKeycodePhrase("Scroll Up", KEYCODE_SCROLL_UP_VOICE);
            speechClient.insertKeycodePhrase("Scroll Down", KEYCODE_SCROLL_DOWN_VOICE);

            // Selection commands
            speechClient.insertKeycodePhrase("Select", KEYCODE_SELECT_VOICE);

            // Direct option selection
            speechClient.insertKeycodePhrase("Individual", KEYCODE_INDIVIDUAL_VOICE);
            speechClient.insertKeycodePhrase("Consolidated", KEYCODE_CONSOLIDATED_VOICE);

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
            Log.d(TAG, "Moved up to: " + options.get(selectedPosition));
        }
    }

    private void moveDown() {
        if (selectedPosition < options.size() - 1) {
            selectedPosition++;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Moved down to: " + options.get(selectedPosition));
        }
    }

    private void selectCurrentOption() {
        String selectedOption = options.get(selectedPosition);
        Log.d(TAG, "Selected option: " + selectedOption);
        goToNext(); // Use the unified next method
    }

    private void selectOptionDirectly(int optionIndex) {
        if (optionIndex >= 0 && optionIndex < options.size()) {
            selectedPosition = optionIndex;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
            Log.d(TAG, "Directly selected: " + options.get(selectedPosition));
        }
    }

    private void goBack() {
        Log.d(TAG, "Going back to previous activity");
        finish();
    }

    // New unified method to handle both Individual and Consolidated navigation
    private void goToNext() {
        String selectedOption = options.get(selectedPosition);

        if (selectedPosition == 0) {
            // Pick - Individual selected
            Intent intent = new Intent(SeventhActivity.this, EighthActivity.class);
            intent.putExtra("SELECTED_OPTION", selectedOption);
            startActivity(intent);
            Log.d(TAG, "Navigating to EighthActivity for Individual pick");

        } else if (selectedPosition == 1) {
            // Pick - Consolidated selected
            Intent intent = new Intent(SeventhActivity.this, ConsolidatedTransactionActivity.class);
            intent.putExtra("SELECTED_OPTION", selectedOption);
            startActivity(intent);
            Log.d(TAG, "Navigating to ConsolidatedTransactionActivity for Consolidated pick");

        } else {
            Log.d(TAG, "No valid option selected");
        }
    }

    // Keep the old method for backward compatibility, but make it use the new unified method
    private void goToNextIfIndividual() {
        goToNext();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle custom voice commands
        switch (keyCode) {
            case KEYCODE_BACK_VOICE:
                goBack();
                return true;

            case KEYCODE_NEXT_VOICE:
                goToNext(); // Changed this line
                return true;

            case KEYCODE_SCROLL_UP_VOICE:
                moveUp();
                return true;

            case KEYCODE_SCROLL_DOWN_VOICE:
                moveDown();
                return true;

            case KEYCODE_SELECT_VOICE:
                selectCurrentOption();
                return true;

            case KEYCODE_INDIVIDUAL_VOICE:
                selectOptionDirectly(0);
                return true;

            case KEYCODE_CONSOLIDATED_VOICE:
                selectOptionDirectly(1);
                return true;

            case KEYCODE_LOGOUT_VOICE:
                Log.d(TAG, "Voice logout command triggered");
                LogoutManager.performLogout(SeventhActivity.this);
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