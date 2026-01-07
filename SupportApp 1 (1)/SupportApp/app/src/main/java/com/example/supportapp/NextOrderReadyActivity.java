package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class NextOrderReadyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_order_ready);


        // Next button logic to go back to scanning process
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            // Transition back to TenthActivity (the scanning process)
            Intent intent = new Intent(NextOrderReadyActivity.this, TenthActivity.class);
            startActivity(intent);
            finish(); // Close this activity to remove it from the stack
        });
    }
}