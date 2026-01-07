// Fixed OrderSummaryActivity.java
package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OrderSummaryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        // FIXED: Next button should go to NextOrderReadyActivity, not ConfirmationPageActivity
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent intent = new Intent(OrderSummaryActivity.this, NextOrderReadyActivity.class);
            startActivity(intent);
            finish(); // Close this activity
        });
    }
}