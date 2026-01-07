package com.example.supportapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FifthActivity extends AppCompatActivity {
    private static final long DELAY_MILLIS = 5000; // 5 seconds
    private Handler delayHandler;
    private Runnable delayRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifth);

        TextView welcomeText = findViewById(R.id.welcomeText);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button logoutButton = findViewById(R.id.logoutButton);

        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = "User";
        }

        String welcomeMessage = "HI " + userName + "\n\nNice to see you again";
        welcomeText.setText(welcomeMessage);

        // Set up logout button click listener
        logoutButton.setOnClickListener(v -> {
            // Cancel the automatic navigation if logout is clicked
            if (delayHandler != null && delayRunnable != null) {
                delayHandler.removeCallbacks(delayRunnable);
            }

            // Use LogoutManager to handle logout
            LogoutManager.performLogout(FifthActivity.this);
        });

        // Set up automatic navigation to SixthActivity after delay
        delayHandler = new Handler();
        delayRunnable = () -> {
            Intent intent = new Intent(FifthActivity.this, SixthActivity.class);
            startActivity(intent);
            finish();
        };

        delayHandler.postDelayed(delayRunnable, DELAY_MILLIS);
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