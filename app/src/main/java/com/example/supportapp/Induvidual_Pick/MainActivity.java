package com.example.supportapp.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.R;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageView imageView1, imageView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);

        // Set a delay to transition to the third page after 5 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Transition to ThirdActivity
                Intent intent = new Intent(MainActivity.this, Barcodescanner.class);
                startActivity(intent);
                finish();  // Close this activity to prevent going back
            }
        }, 5000); // Delay of 5000 milliseconds (5 seconds)
    }
}
