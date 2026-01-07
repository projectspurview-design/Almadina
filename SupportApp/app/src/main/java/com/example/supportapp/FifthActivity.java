package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FifthActivity extends AppCompatActivity {
    private static final long DELAY_MILLIS = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifth);

        TextView welcomeText = findViewById(R.id.welcomeText);

        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = "User";
        }

        String welcomeMessage = "HI " + userName + "\n\nNice to see you again";
        welcomeText.setText(welcomeMessage);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(FifthActivity.this, SixthActivity.class);
            startActivity(intent);
            finish();
        }, DELAY_MILLIS);
    }
}
