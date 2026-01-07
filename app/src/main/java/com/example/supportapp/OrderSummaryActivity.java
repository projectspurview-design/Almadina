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

        // Menu button goes to MenuActivity
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            Intent intent = new Intent(OrderSummaryActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        // Next button logic (replace with your actual next step)
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent intent = new Intent(OrderSummaryActivity.this, ConfirmationPageActivity.class);
            startActivity(intent);
        });
    }
}
