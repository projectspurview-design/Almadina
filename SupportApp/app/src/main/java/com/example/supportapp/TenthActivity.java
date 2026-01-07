package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TenthActivity extends AppCompatActivity {

    private static final int MENU_REQUEST_CODE = 1;

    private TextView pausedStatusText;
    private Button btnScanBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenth);

        pausedStatusText = findViewById(R.id.pausedStatusText);
        btnScanBarcode = findViewById(R.id.btnScanBarcode);

        // Open MenuActivity with startActivityForResult
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            Intent intent = new Intent(TenthActivity.this, MenuActivity.class);
            startActivityForResult(intent, MENU_REQUEST_CODE);
        });

        // Scan Barcode button click logic
        btnScanBarcode.setOnClickListener(v -> {
            String currentText = btnScanBarcode.getText().toString();

            if (currentText.equalsIgnoreCase("Resume")) {
                // Resume clicked: hide paused UI, restore Scan Barcode text and proceed
                pausedStatusText.setVisibility(View.GONE);
                btnScanBarcode.setText("Scan Barcode");

                // Proceed to next activity (confirmation page)
                Intent intent = new Intent(TenthActivity.this, ConfirmationPageActivity.class);
                startActivity(intent);
                finish();
            } else if (currentText.equalsIgnoreCase("Scan Barcode")) {
                // Normal Scan Barcode action
                pausedStatusText.setVisibility(View.GONE);
                btnScanBarcode.setText("Scan Barcode"); // ensure default text

                // Show Right SKU text
                TextView tvRightSKU = findViewById(R.id.tvRightSKU);
                tvRightSKU.setVisibility(View.VISIBLE);

                // Hide Scan button temporarily (optional, if you want)
                btnScanBarcode.setVisibility(View.GONE);

                // After 5 seconds, navigate to ConfirmationPageActivity
                btnScanBarcode.postDelayed(() -> {
                    Intent intent = new Intent(TenthActivity.this, ConfirmationPageActivity.class);
                    startActivity(intent);
                    finish();
                }, 5000);
            }
        });
    }

    // Handle result from MenuActivity pause/resume selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MENU_REQUEST_CODE && resultCode == RESULT_OK) {
            boolean paused = data.getBooleanExtra("PAUSED", false);

            if (paused) {
                pausedStatusText.setVisibility(View.VISIBLE);
                pausedStatusText.setText("Paused");
                btnScanBarcode.setText("Resume");
                btnScanBarcode.setVisibility(View.VISIBLE);  // Make sure visible if hidden
            } else {
                pausedStatusText.setVisibility(View.GONE);
                btnScanBarcode.setText("Scan Barcode");
                btnScanBarcode.setVisibility(View.VISIBLE);
            }
        }
    }
}
