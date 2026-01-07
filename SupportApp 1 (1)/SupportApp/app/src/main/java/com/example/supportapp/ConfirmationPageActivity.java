package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ConfirmationPageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_page);


        // Next button - add your logic here
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmationPageActivity.this, OrderSummaryActivity.class);
            startActivity(intent);
        });

    }
}
