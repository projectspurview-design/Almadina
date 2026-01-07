package com.example.supportapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.voice.VoiceCommandCenter;

import java.util.Arrays;
import java.util.List;

public class NextOrderReadyActivity extends AppCompatActivity {

    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

    // Centralized voice actions for this screen
    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onNext() {
            goToNextIfIndividual();
        }
        @Override public void onLogout() {
            Log.d(TAG, "Voice logout command triggered");
            LogoutManager.performLogout(NextOrderReadyActivity.this);
        }

        // Unused here but required by interface
        @Override public void onBack() {}
        @Override public void onScrollUp() {}
        @Override public void onScrollDown() {}
        @Override public void onSelect() {}
        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_order_ready);

        // Register shared voice phrases (idempotent)
        VoiceCommandCenter.init(this);

        Button logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                LogoutManager.performLogout(NextOrderReadyActivity.this);
            });
        }

        // Next button → go to EighthActivity (pick next job)
        Button nextBtn = findViewById(R.id.btnNext);
        if (nextBtn != null) {
            nextBtn.setOnClickListener(v -> {
                Intent intent = new Intent(NextOrderReadyActivity.this, EighthActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register after lifecycle changes so voice keeps working
        VoiceCommandCenter.init(this);
    }

    private void goToNextIfIndividual() {
        if (selectedPosition == 0) {
            Intent intent = new Intent(NextOrderReadyActivity.this, EighthActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
            finish();
            Log.d(TAG, "Navigating to EighthActivity");
        } else {
            Log.d(TAG, "Cannot proceed - Pick Individual not selected");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Centralized key routing for voice
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
