package com.example.supportapp.Pick_Consolidated;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.supportapp.R;

public class NextorderconsolidatedActivity extends AppCompatActivity {

    // Params required by LocationDetailsActivity
    private String transBatchId;
    private String companyCode;
    private String prinCode;
    private String pickUser;

    private boolean navigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nextorderconsolidated);

        // Use safe content root for insets to avoid NPEs
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // 🔗 Read the params sent from ConsolidatedTransactionActivity
        transBatchId = getIntent().getStringExtra("TRANS_BATCH_ID");
        companyCode  = getIntent().getStringExtra("COMPANY_CODE");
        prinCode     = getIntent().getStringExtra("PRIN_CODE");
        pickUser     = getIntent().getStringExtra("PICK_USER");

        // Validate — LocationDetailsActivity needs all four
        View nextBtn = findViewById(R.id.btnNext);
        if (isMissing(transBatchId) || isMissing(companyCode) || isMissing(prinCode) || isMissing(pickUser)) {
            if (nextBtn != null) nextBtn.setEnabled(false);
            Toast.makeText(this, "Missing required data. Please reselect a transaction.", Toast.LENGTH_LONG).show();
            return;
        }

        if (nextBtn != null) {
            nextBtn.setOnClickListener(v -> {
                if (navigating) return;
                navigating = true;
                v.setEnabled(false);

                // ▶️ Go to LocationDetailsActivity with the same required extras
                Intent nextIntent = new Intent(
                        NextorderconsolidatedActivity.this,
                        LocationDetailsActivity.class
                );
                nextIntent.putExtra("TRANS_BATCH_ID", transBatchId);
                nextIntent.putExtra("COMPANY_CODE",   companyCode);
                nextIntent.putExtra("PRIN_CODE",      prinCode);
                nextIntent.putExtra("PICK_USER",      pickUser);

                // Optional flags you already support on LocationDetailsActivity:
                // - SELECT_ONLY: false → continue the normal flow
                // - CURRENT_LOCATION_CODE: pass if you have one to preselect; otherwise skip
                nextIntent.putExtra("SELECT_ONLY", false);

                startActivity(nextIntent);
                finish(); // remove this page from the back stack
            });
        }
    }

    private boolean isMissing(String s) {
        return s == null || s.trim().isEmpty();
    }
}

