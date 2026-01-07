package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class NextOrderReadyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_order_ready);

        // Menu button to go to MenuActivity
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            Intent intent = new Intent(NextOrderReadyActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        // Next button logic - you can define what happens next here
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            // TODO: Add next action or restart order flow
        });
    }
}
