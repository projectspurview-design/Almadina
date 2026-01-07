package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NinthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ninth);

        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView skuText = findViewById(R.id.skuText);
        TextView quantityText = findViewById(R.id.quantityText);

        // Get the order number, job number, and location from the Intent
        String orderNumber = getIntent().getStringExtra("ORDER_NUMBER");
        String jobNumber = getIntent().getStringExtra("JOB_NUMBER");
        String location = getIntent().getStringExtra("LOCATION");

        // Display order number or a default message if not available
        if (orderNumber != null) {
            orderNumberText.setText("Order Number: " + orderNumber);
        } else {
            orderNumberText.setText("Order Number: Not Available");
        }

        // Set placeholder values for SKU and Quantity (can be made dynamic)
        skuText.setText("SKU: 3");
        quantityText.setText("Quantity: 99");

        // Handle Menu button click to go to MenuActivity
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            Intent intent = new Intent(NinthActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        // Handle Back button click to return to EighthActivity
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Handle Next button click to go to TenthActivity
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent intent = new Intent(NinthActivity.this, TenthActivity.class);
            intent.putExtra("ORDER_NUMBER", orderNumber);
            intent.putExtra("JOB_NUMBER", jobNumber);
            intent.putExtra("LOCATION", location);
            startActivity(intent);
        });
    }
}