package com.example.supportapp.Pick_Consolidated;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.supportapp.LogoutManager;
import com.example.supportapp.Pick_Consolidated.model.ConsolidatedId;
import com.example.supportapp.Pick_Consolidated.network.ConsolidatedPickingApi;
import com.example.supportapp.Pick_Consolidated.network.RetrofitClientDev;
import com.example.supportapp.Pick_Consolidated.session.SessionHelper;
import com.example.supportapp.R;
import com.example.supportapp.voice.VoiceCommandCenter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsolidatedTransactionActivity extends AppCompatActivity {

    private static final String TAG = "ConsolidatedTxn";

    private TextView tvId1, tvId2;

    private ImageView arrowUp, arrowDown;
    private AppCompatButton backButton, btnNext;

    private List<ConsolidatedId> data;
    private int currentIndex = 0;

    private ConsolidatedPickingApi api;
    private SessionHelper session;

    // Selected transaction details
    private String selectedTransactionId;
    private String selectedCompanyCode;
    private String selectedPrinCode;
    private String selectedPickUser;

    // Centralized voice actions for this screen
    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onBack() { finish(); }
        @Override public void onNext() {
            if (btnNext != null) btnNext.performClick();
            else goNext();
        }
        @Override public void onScrollUp() {
            if (arrowUp != null && arrowUp.isEnabled()) arrowUp.performClick();
            else moveUp();
        }
        @Override public void onScrollDown() {
            if (arrowDown != null && arrowDown.isEnabled()) arrowDown.performClick();
            else moveDown();
        }
        @Override public void onLogout() { LogoutManager.performLogout(ConsolidatedTransactionActivity.this); }

        // Unused in this screen
        @Override public void onSelect() {}
        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_consolidated_transaction);

        // Register shared voice phrases (idempotent)
        VoiceCommandCenter.init(this);

        session = new SessionHelper(this);
        api = RetrofitClientDev.getInstance().create(ConsolidatedPickingApi.class);

        tvId1 = findViewById(R.id.tvId1);
        tvId2 = findViewById(R.id.tvId2);
        arrowUp = findViewById(R.id.arrowUp);
        arrowDown = findViewById(R.id.arrowDown);
        backButton = findViewById(R.id.backButton);
        btnNext = findViewById(R.id.btnNext);

        backButton.setOnClickListener(v -> finish());
        btnNext.setOnClickListener(v -> goNext());
        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> LogoutManager.performLogout(ConsolidatedTransactionActivity.this));

        fetchConsolidatedIds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register after lifecycle changes so voice keeps working
        VoiceCommandCenter.init(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Centralized key routing for voice
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void fetchConsolidatedIds() {
        String user = session.getUserId();
        if (user == null || user.trim().isEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        api.getConsolidatedIds(user).enqueue(new Callback<List<ConsolidatedId>>() {
            @Override
            public void onResponse(Call<List<ConsolidatedId>> call, Response<List<ConsolidatedId>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    Toast.makeText(ConsolidatedTransactionActivity.this, "Failed to fetch IDs", Toast.LENGTH_SHORT).show();
                    return;
                }

                data = resp.body();
                if (data.isEmpty()) {
                    tvId1.setText("");
                    tvId2.setText("");
                    btnNext.setEnabled(false);
                    Toast.makeText(ConsolidatedTransactionActivity.this, "No transactions found", Toast.LENGTH_SHORT).show();
                } else {
                    currentIndex = (data.size() >= 2) ? 1 : 0;
                    btnNext.setEnabled(true);
                    updateUi();
                }
            }

            @Override
            public void onFailure(Call<List<ConsolidatedId>> call, Throwable t) {
                Toast.makeText(ConsolidatedTransactionActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUi() {
        if (data == null || data.isEmpty()) return;

        String above = "";
        if (currentIndex - 1 >= 0 && currentIndex - 1 < data.size()) {
            above = safeIdText(data.get(currentIndex - 1));
        }
        String current = safeIdText(data.get(currentIndex));

        tvId1.setText(above);
        tvId2.setText(current);

        arrowUp.setAlpha(currentIndex > 0 ? 1f : 0.4f);
        arrowUp.setEnabled(currentIndex > 0);

        boolean hasDown = currentIndex < data.size() - 1;
        arrowDown.setAlpha(hasDown ? 1f : 0.4f);
        arrowDown.setEnabled(hasDown);
    }

    private String safeIdText(ConsolidatedId row) {
        return row != null ? (row.transBatchId != null ? row.transBatchId : "") : "";
    }

    private void moveUp() {
        if (data == null || data.isEmpty()) return;
        if (currentIndex > 0) {
            currentIndex--;
            updateUi();
        }
    }

    private void moveDown() {
        if (data == null || data.isEmpty()) return;
        if (currentIndex < data.size() - 1) {
            currentIndex++;
            updateUi();
        }
    }

    private void goNext() {
        if (data == null || data.isEmpty()) return;

        ConsolidatedId sel = data.get(currentIndex);
        Log.d(TAG, "Selected: " + sel.transBatchId + " (PRIN_CODE=" + sel.prinCode + ")");

        String transBatchId = sel.transBatchId;
        String companyCode  = sel.companyCode;
        String prinCode     = sel.prinCode;
        String pickUser     = session.getUserId();

        // 🚀 Jump straight to LocationDetailsActivity (or whatever the *actual* next UI is)
        Intent i = new Intent(this, LocationDetailsActivity.class);
        i.putExtra("TRANS_BATCH_ID", transBatchId);
        i.putExtra("COMPANY_CODE",   companyCode);
        i.putExtra("PRIN_CODE",      prinCode);
        i.putExtra("PICK_USER",      pickUser);
        startActivity(i);
    }


}
