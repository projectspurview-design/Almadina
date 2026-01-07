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
import com.example.supportapp.voice.VoiceCommandCenter;
import com.example.supportapp.voice.VoiceCommandCenter.Actions;

import java.util.Arrays;
import java.util.List;

public class SeventhActivity extends AppCompatActivity {

    private static final String TAG = "SeventhActivity";

    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private final List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

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
        nextButton.setOnClickListener(v -> goToNext()); // unified next

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(SeventhActivity.this);
        });

        // Enable bubble numbers for buttons (as in your original)
        setupBubbleNumbers();

        // Centralized voice commands (safe even if also done in onResume)
        VoiceCommandCenter.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Ensure voice phrases are active every time you return to this screen
        VoiceCommandCenter.init(this);
    }

    private void setupBubbleNumbers() {
        try {
            Log.d(TAG, "Bubble number system enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bubble numbers: " + e.getMessage());
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

    // Unified method to handle both Individual and Consolidated navigation
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

    // Keep the old method for backward compatibility, but use the unified method
    private void goToNextIfIndividual() {
        goToNext();
    }

    // Map voice → actions for this screen (unchanged)
    private final Actions voiceActions = new Actions() {
        @Override public void onNext()        { goToNext(); }
        @Override public void onBack()        { goBack(); }
        @Override public void onScrollUp()    { moveUp(); }
        @Override public void onScrollDown()  { moveDown(); }
        @Override public void onSelect()      { selectCurrentOption(); }

        // Not used on this screen
        @Override public void onInbound()     { /* ignore */ }
        @Override public void onOutbound()    { /* ignore */ }
        @Override public void onInventory()   { /* ignore */ }

        // Used here
        @Override public void onIndividual()  { selectOptionDirectly(0); }
        @Override public void onConsolidated(){ selectOptionDirectly(1); }

        @Override public void onLogout()      {
            Log.d(TAG, "Voice logout command triggered");
            LogoutManager.performLogout(SeventhActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No explicit shutdown needed
    }
}
