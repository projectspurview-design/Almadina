package com.example.supportapp.Pick_Consolidated;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.Pick_Consolidated.network.ConsolidatedPickingApi;
import com.example.supportapp.Pick_Consolidated.network.RetrofitClientDev;
import com.example.supportapp.R;
import com.example.supportapp.NextOrderReadyActivity;


import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompletedordersummaryconsolidatedActivity extends AppCompatActivity {

    private static final String TAG = "CompletedOrderSummary";

    private TextView tvGoal, tvPerformanceValue, tvTime;
    private Button nextButton;

    private String orderNumber, jobNumber, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completedordersummaryconsolidated);

        // Initialize UI components
        tvGoal = findViewById(R.id.tvGoal);
        tvPerformanceValue = findViewById(R.id.tvPerformanceValue);
        tvTime = findViewById(R.id.tvTime);
        nextButton = findViewById(R.id.btnNext);

        // Handle button click
        nextButton.setOnClickListener(v -> {
            Intent nextIntent = new Intent(CompletedordersummaryconsolidatedActivity.this, NextOrderReadyActivity.class);
            startActivity(nextIntent);
            finish();
        });

        // Example of data passed into the activity (you can fetch this data dynamically)
        orderNumber = "ORD123"; // Set dynamically
        jobNumber = "JOB123";   // Set dynamically
        userId = "USER123";     // Set dynamically

        // Fetch data for the job
        fetchJobData();
    }

    private void fetchJobData() {
        // Fetch the outbound allocated time and job timing for the current job
        fetchOutboundAllocatedTime();
        fetchJobTiming();
    }

    private void fetchOutboundAllocatedTime() {
        ConsolidatedPickingApi api = RetrofitClientDev.getInstance().create(ConsolidatedPickingApi.class);
        Call<JsonObject> call = api.getOutboundAllocatedTime(orderNumber, System.currentTimeMillis());

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject jsonResponse = response.body();
                    // Update UI with the response data
                    String allocatedTime = jsonResponse != null ? jsonResponse.get("outbounD_ALLOCATED_TIME").getAsString() : "No Data";
                    tvGoal.setText("Goal: " + allocatedTime);
                    calculatePerformance(allocatedTime);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching allocated time: " + t.getMessage());
            }
        });
    }

    private void fetchJobTiming() {
        ConsolidatedPickingApi api = RetrofitClientDev.getInstance().create(ConsolidatedPickingApi.class);
        Call<JsonObject> call = api.getJobTiming(userId, orderNumber, jobNumber, System.currentTimeMillis());

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject jsonResponse = response.body();
                    String totalActiveMinutes = jsonResponse != null ? jsonResponse.get("total_active_minutes").getAsString() : "0";
                    tvTime.setText("Time: " + totalActiveMinutes + " min");
                    calculatePerformance(totalActiveMinutes);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching job timing: " + t.getMessage());
            }
        });
    }

    private void calculatePerformance(String time) {
        try {
            int performance = Integer.parseInt(time);
            tvPerformanceValue.setText(performance + "%");
        } catch (NumberFormatException e) {
            tvPerformanceValue.setText("N/A");
        }
    }
}
