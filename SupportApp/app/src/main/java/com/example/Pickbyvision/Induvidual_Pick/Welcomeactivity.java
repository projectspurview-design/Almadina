package com.example.Pickbyvision.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.R;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

public class Welcomeactivity extends AppCompatActivity {

    private static final String TAG = "WelcomeActivity";
    private static final long DELAY_MILLIS = 5000;

    private VuzixSpeechClient speechClient;

    private Handler delayHandler;
    private Runnable delayRunnable;

    private TextView welcomeText;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcomeactivity);

        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);


        logoutButton.setEnabled(false);

        setupVoiceCommands();
        initNormalFlow();
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            // Remove unwanted default phrases
            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");

            Log.d(TAG, "Vuzix voice commands cleaned successfully");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed", e);
        }
    }

    private void initNormalFlow() {
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.trim().isEmpty()) {
            userName = "User";
        }

        String welcomeMessage = "HI " + userName + "\n\nNice to see you again";
        welcomeText.setText(welcomeMessage);

        logoutButton.setEnabled(true);


        logoutButton.setOnClickListener(v -> {
            cancelAutoNavigation();
            LogoutManager.performLogout(Welcomeactivity.this);
        });


        delayHandler = new Handler(Looper.getMainLooper());
        delayRunnable = () -> {
            Intent intent = new Intent(Welcomeactivity.this, ProcessActivity.class);
            startActivity(intent);
            finish();
        };

        delayHandler.postDelayed(delayRunnable, DELAY_MILLIS);
    }

    private void cancelAutoNavigation() {
        if (delayHandler != null && delayRunnable != null) {
            delayHandler.removeCallbacks(delayRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAutoNavigation();
    }
}

