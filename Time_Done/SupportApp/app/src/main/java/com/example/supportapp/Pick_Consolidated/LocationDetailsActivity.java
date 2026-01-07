package com.example.supportapp.Pick_Consolidated;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.supportapp.R;

public class LocationDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_details);

        Button back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        Button next = findViewById(R.id.btnNext);
        if (next != null) {
            next.setOnClickListener(v ->
                    startActivity(new Intent(this, ConsolidatedJobActivity.class))
            );
        }
    }
}
