package com.example.Pickbyvision.Consolidated_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.Pickbyvision.R;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

public class NextorderconsolidatedActivity extends AppCompatActivity {

    private static final String TAG = "Nextorderconsolidated";
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2; // 🔹 Same as reference

    private VuzixSpeechClient speechClient;
    private View nextBtn; // keep a reference so we can trigger performClick()

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

        // ---- UI: Next button ----
        nextBtn = findViewById(R.id.btnNext);
        if (nextBtn == null) {
            Log.e(TAG, "Next button not found in layout!");
        } else {
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

        // ---- Voice commands (Vuzix) ----
        setupVoiceCommands();
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);


            speechClient.deletePhrase("BACK");
            speechClient.deletePhrase("GO BACK");

            speechClient.deletePhrase("Go Back");


            // When user says "Next", device will send KEYCODE_F2
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);
            Log.d(TAG, "Voice command 'Next' registered with keycode F2");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient: " + e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key pressed: " + keyCode);

        if (keyCode == KEYCODE_NEXT_VOICE) {
            Log.d(TAG, "Voice 'Next' triggered -> performing Next button click");
            if (nextBtn != null) {
                nextBtn.performClick();  // 🔹 Re-use the same logic as manual tap
            }
            return true; // we handled this event
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechClient != null) {
            try {
                speechClient = null; // letting GC clean it up
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up speech client: " + e.getMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {

        // If camera/scanner is open -> just close scanner UI


        // If Short UI is open -> close it (optional, but recommended)


        // 🚫 Block navigation to previous screen (hardware back included)
        Log.d(TAG, "Back pressed – blocked (no navigation)");
        Toast.makeText(this, "Back disabled on this screen", Toast.LENGTH_SHORT).show();

        // DO NOT call super.onBackPressed();
    }
}
