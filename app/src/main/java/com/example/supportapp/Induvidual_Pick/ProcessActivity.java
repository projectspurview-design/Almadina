package com.example.supportapp.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportapp.Induvidual_Pick.adapter.OptionAdapter;
import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.R;
import com.example.supportapp.voice.VoiceCommandCenter;
import com.example.supportapp.voice.VoiceCommandCenter.Actions;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class ProcessActivity extends AppCompatActivity {

    private static final String TAG = "ProcessActivity";

    private VuzixSpeechClient speechClient;


    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private final List<String> options = Arrays.asList("Inbound", "Outbound", "Inventory");
    private int selectedPosition = 1; // Initially "Outbound"
    private OptionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.processactivity);

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button nextButton = findViewById(R.id.btnNext);
        Button backButton = findViewById(R.id.btnBack);
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
        backButton.setOnClickListener(v -> goBack());
        nextButton.setOnClickListener(v -> goToNextIfOutbound());

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            LogoutManager.performLogout(ProcessActivity.this);
        });

        // Optional: keep this — harmless if also done in onResume()
        VoiceCommandCenter.init(this);

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

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Re-register phrases every time the screen returns to foreground
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

        if (selectedPosition == 1) {
            goToNextIfOutbound();
        } else {
            Log.d(TAG, "Option selected: " + selectedOption);
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

    private void goBack() {
        Log.d(TAG, "Going back to previous activity");
        finish();
    }

    private void goToNextIfOutbound() {
        if (selectedPosition == 1) {
            Intent intent = new Intent(ProcessActivity.this, OutBoundActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
            Log.d(TAG, "Navigating to SeventhActivity with option: " + options.get(selectedPosition));
        } else {
            Log.d(TAG, "Cannot proceed - Outbound not selected");
        }
    }

    // Voice actions mapping (unchanged behavior)
    private final Actions voiceActions = new Actions() {
        @Override public void onNext()        { goToNextIfOutbound(); }
        @Override public void onBack()        { goBack(); }
        @Override public void onScrollUp()    { moveUp(); }
        @Override public void onScrollDown()  { moveDown(); }
        @Override public void onSelect()      { selectCurrentOption(); }

        @Override public void onInbound()     { selectOptionDirectly(0); }
        @Override public void onOutbound()    { selectOptionDirectly(1); }
        @Override public void onInventory()   { selectOptionDirectly(2); }

        @Override public void onIndividual()  { /* not used here */ }
        @Override public void onConsolidated(){ /* not used here */ }

        @Override public void onLogout()      {
            Log.d(TAG, "Voice logout command triggered");
            LogoutManager.performLogout(ProcessActivity.this);
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
