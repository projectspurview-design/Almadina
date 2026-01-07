package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageView imageView1, imageView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Initialize views
        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);

        // Set a delay to transition to the fourth page after 5 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Transition directly to FourthActivity (skipping ThirdActivity)
                Intent intent = new Intent(SecondActivity.this, FourthActivity.class);
                startActivity(intent);
                finish();  // Close this activity to prevent going back
            }
        }, 5000); // Delay of 5000 milliseconds (5 seconds)
    }
}