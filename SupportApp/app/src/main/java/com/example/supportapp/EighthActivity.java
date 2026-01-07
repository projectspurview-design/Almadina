package com.example.supportapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EighthActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eighth);

        TextView mohebiText = findViewById(R.id.mohebiText);
        TextView orderNumberText = findViewById(R.id.orderNumberText);
        TextView jobNumberText = findViewById(R.id.jobNumberText);
        TextView jobNumberText1 = findViewById(R.id.jobNumberText1);
        TextView pausedStatusText = findViewById(R.id.pausedStatusText);
        Button nextButton = findViewById(R.id.btnBottomRight);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String orderNumber = prefs.getString("ORDER_NUMBER", "OM-GO20/08012");
        String jobNumber = prefs.getString("JOB_NUMBER", "1024017038");
        String location = prefs.getString("LOCATION", "O20 Oman - Barka");

        // Clear pause state on fresh app start
        prefs.edit().remove("EighthActivityPaused").apply();

        mohebiText.setText("006 Mohebi Enterprises (Oman branch)");
        orderNumberText.setText("Order number: " + orderNumber);
        jobNumberText.setText("Job number: " + jobNumber);
        jobNumberText1.setText(location);

        // Initialize UI to default state (not paused)
        pausedStatusText.setVisibility(TextView.GONE);
        nextButton.setText("Next");

        findViewById(R.id.btnTopLeft).setOnClickListener(v -> {
            Intent intent = new Intent(EighthActivity.this, MenuActivity.class);
            startActivityForResult(intent, 1);
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        nextButton.setOnClickListener(v -> {
            Toast.makeText(EighthActivity.this, "Finalizing and moving to next step...", Toast.LENGTH_SHORT).show();

            prefs.edit()
                    .putBoolean("finishedStep8", true)
                    .putString("ORDER_NUMBER", orderNumber)
                    .putString("JOB_NUMBER", jobNumber)
                    .putString("LOCATION", location)
                    .remove("EighthActivityPaused")
                    .apply();

            // Reset UI state
            pausedStatusText.setVisibility(TextView.GONE);
            nextButton.setText("Next");

            Intent intent = new Intent(EighthActivity.this, NinthActivity.class);
            intent.putExtra("ORDER_NUMBER", orderNumber);
            intent.putExtra("JOB_NUMBER", jobNumber);
            intent.putExtra("LOCATION", location);
            startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            boolean paused = data.getBooleanExtra("PAUSED", false);
            TextView pausedStatusText = findViewById(R.id.pausedStatusText);
            Button nextButton = findViewById(R.id.btnBottomRight);
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (paused) {
                prefs.edit().putBoolean("EighthActivityPaused", true).apply();
                pausedStatusText.setText("Paused");
                pausedStatusText.setVisibility(TextView.VISIBLE);
                nextButton.setText("Resume");
            } else {
                prefs.edit().remove("EighthActivityPaused").apply();
                pausedStatusText.setVisibility(TextView.GONE);
                nextButton.setText("Next");
            }
        }
    }
}