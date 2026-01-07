package com.example.Pickbyvision.Consolidated_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;

public class NextorderconsolidatedActivity extends AppCompatActivity {

    private static final String TAG = "Nextorderconsolidated";

    private View nextBtn;
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

        nextBtn = findViewById(R.id.btnNext);
        if (nextBtn == null) {
            Log.e(TAG, "Next button not found in layout!");
        } else {
            nextBtn.setOnClickListener(v -> {
                v.setEnabled(false);
                Intent nextIntent = new Intent(
                        NextorderconsolidatedActivity.this,
                        ConsolidatedTransactionActivity.class
                );
                startActivity(nextIntent);
                finish();
            });
        }

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        VoiceCommandCenter.Actions actions = new VoiceCommandCenter.Actions() {
            @Override public void onNext() {
                if (nextBtn != null) nextBtn.performClick();
            }

            @Override public void onLogout() {
                LogoutManager.performLogout(NextorderconsolidatedActivity.this);
            }

            @Override public void onBack() {}
            @Override public void onScrollUp() {}
            @Override public void onScrollDown() {}
            @Override public void onSelect() {}
            @Override public void onInbound() {}
            @Override public void onOutbound() {}
            @Override public void onInventory() {}
            @Override public void onIndividual() {}
            @Override public void onConsolidated() {}
        };

        if (VoiceCommandCenter.handleKeyDownNextOrderConsolidated(keyCode, actions)) return true;

        if (VoiceCommandCenter.handleKeyDown(keyCode, actions)) return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceCommandCenter.init(this);
        VoiceCommandCenter.initConsolidatedTransaction(this);
        VoiceCommandCenter.initNextOrderConsolidated(this);
    }

    @Override
    public void onBackPressed() {





        Log.d(TAG, "Back pressed – blocked (no navigation)");
        Toast.makeText(this, "Back disabled on this screen", Toast.LENGTH_SHORT).show();

    }
}
