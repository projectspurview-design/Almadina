package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";

    private VuzixSpeechClient speechClient;

    // Define keycodes for voice command mappings
    private static final int KEYCODE_PAUSE = KeyEvent.KEYCODE_A;
    private static final int KEYCODE_MENU = KeyEvent.KEYCODE_B;
    private static final int KEYCODE_BACK = KeyEvent.KEYCODE_C;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Click listeners for buttons
        findViewById(R.id.btnPause).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("PAUSED", true);
            setResult(RESULT_OK, intent);
            finish();
        });

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("PAUSED", false);
            setResult(RESULT_OK, intent);
            finish();
        });

        findViewById(R.id.btnBackToNinth).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("PAUSED", false);
            setResult(RESULT_OK, intent);
            finish();
        });

        setupVoiceCommands();
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            // Voice phrases mapped to keycodes
            speechClient.insertKeycodePhrase("Pause", KEYCODE_PAUSE);
            speechClient.insertKeycodePhrase("Menu", KEYCODE_MENU);
            speechClient.insertKeycodePhrase("Back", KEYCODE_BACK);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Intent intent = new Intent();
        switch (keyCode) {
            case KEYCODE_PAUSE:
                intent.putExtra("PAUSED", true);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            case KEYCODE_MENU:
            case KEYCODE_BACK:
                intent.putExtra("PAUSED", false);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No shutdown() required for VuzixSpeechClient in current SDK
    }
}
