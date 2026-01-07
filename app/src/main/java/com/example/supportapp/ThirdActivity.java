package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

public class ThirdActivity extends AppCompatActivity {

    private Button nextButton;
    private VuzixSpeechClient speechClient;
    private static final int NEXT_KEY_CODE = KeyEvent.KEYCODE_N; // Custom key code
    private static final String TAG = "ThirdActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            Intent intent = new Intent(ThirdActivity.this, FourthActivity.class);
            startActivity(intent);
        });

        setupSpeechClient();
    }

    private void setupSpeechClient() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.insertKeycodePhrase("NEXT", NEXT_KEY_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == NEXT_KEY_CODE) {
            nextButton.performClick();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No shutdown() needed
    }
}
