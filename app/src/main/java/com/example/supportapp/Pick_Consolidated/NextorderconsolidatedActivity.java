package com.example.supportapp.Pick_Consolidated;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.supportapp.Pick_Consolidated.ConsolidatedTransactionActivity;
import com.example.supportapp.R;

public class NextorderconsolidatedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nextorderconsolidated);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Go to ConsolidatedTransactionActivity on "Next"
        View nextBtn = findViewById(R.id.btnNext);
        nextBtn.setOnClickListener(v -> {
            // debounce to avoid double navigation
            v.setEnabled(false);
            Intent nextIntent = new Intent(
                    NextorderconsolidatedActivity.this,
                    ConsolidatedTransactionActivity.class
            );
            startActivity(nextIntent);
            // remove this screen from back stack so Back doesn't return here
            finish();
        });
    }
}
