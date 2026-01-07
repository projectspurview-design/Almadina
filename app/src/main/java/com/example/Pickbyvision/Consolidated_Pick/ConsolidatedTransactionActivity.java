package com.example.Pickbyvision.Consolidated_Pick;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Pickbyvision.Induvidual_Pick.OutBoundActivity;
import com.example.Pickbyvision.data.UserSessionManager;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedId;
import com.example.Pickbyvision.Consolidated_Pick.network.ConsolidatedPickingApi;
import com.example.Pickbyvision.Consolidated_Pick.network.RetrofitClientDev;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsolidatedTransactionActivity extends AppCompatActivity {

    private static final String TAG = "ConsolidatedTxn";

    private RecyclerView rvIds;
    private VuzixSpeechClient speechClient;
    private IdsAdapter adapter;

    private ImageView arrowUp, arrowDown;
    private AppCompatButton backButton, btnNext;

    private List<ConsolidatedId> data = new ArrayList<>();
    private int currentIndex = 0;

    private ConsolidatedPickingApi api;

    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onBack() { goToOutbound(); }

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

        @Override public void onLogout() {
            LogoutManager.performLogout(ConsolidatedTransactionActivity.this);
        }

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

        VoiceCommandCenter.init(this);

        api = RetrofitClientDev.getInstance()
                .create(ConsolidatedPickingApi.class);

        rvIds = findViewById(R.id.rvIds);
        arrowUp = findViewById(R.id.arrowUp);
        arrowDown = findViewById(R.id.arrowDown);
        backButton = findViewById(R.id.backButton);
        btnNext = findViewById(R.id.btnNext);

        adapter = new IdsAdapter();
        rvIds.setLayoutManager(new LinearLayoutManager(this));
        rvIds.setAdapter(adapter);

        backButton.setOnClickListener(v -> goToOutbound());
        btnNext.setOnClickListener(v -> goNext());
        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v ->
                LogoutManager.performLogout(this));

        setupVoiceCommands();
        fetchConsolidatedIds();
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");
            Log.d(TAG, "Voice commands registered");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceCommandCenter.init(this);

        VoiceCommandCenter.initConsolidatedTransaction(this);

    }

    private void goToOutbound() {
        Intent intent = new Intent(this, OutBoundActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void fetchConsolidatedIds() {

        String user = UserSessionManager.getUserId(this);

        if (user == null || user.trim().isEmpty()) {
            Toast.makeText(this, "User not found. Please login again.",
                    Toast.LENGTH_LONG).show();
            LogoutManager.performLogout(this);
            return;
        }

        api.getConsolidatedIds(user).enqueue(new Callback<List<ConsolidatedId>>() {
            @Override
            public void onResponse(Call<List<ConsolidatedId>> call,
                                   Response<List<ConsolidatedId>> resp) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    Toast.makeText(ConsolidatedTransactionActivity.this,
                            "Failed to fetch IDs", Toast.LENGTH_SHORT).show();
                    return;
                }

                data = resp.body();

                if (data.isEmpty()) {
                    btnNext.setEnabled(false);
                    Toast.makeText(ConsolidatedTransactionActivity.this,
                            "No transactions found", Toast.LENGTH_SHORT).show();
                } else {
                    currentIndex = 0;
                    btnNext.setEnabled(true);
                    updateUi();
                }
            }

            @Override
            public void onFailure(Call<List<ConsolidatedId>> call, Throwable t) {
                Toast.makeText(ConsolidatedTransactionActivity.this,
                        t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUi() {
        adapter.notifyDataSetChanged();
        rvIds.scrollToPosition(currentIndex);

        arrowUp.setEnabled(currentIndex > 0);
        arrowDown.setEnabled(currentIndex < data.size() - 1);
    }

    private void moveUp() {
        if (currentIndex > 0) {
            currentIndex--;
            updateUi();
        }
    }

    private void moveDown() {
        if (currentIndex < data.size() - 1) {
            currentIndex++;
            updateUi();
        }
    }

    private void goNext() {
        if (data.isEmpty()) return;

        ConsolidatedId sel = data.get(currentIndex);

        Intent i = new Intent(this, LocationDetailsActivity.class);
        i.putExtra("TRANS_BATCH_ID", sel.transBatchId);
        i.putExtra("COMPANY_CODE", sel.companyCode);
        i.putExtra("PRIN_CODE", sel.prinCode);
        i.putExtra("PICK_USER", UserSessionManager.getUserId(this));
        startActivity(i);
    }

    private class IdsAdapter extends RecyclerView.Adapter<IdsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(18);
            tv.setTextColor(0xFFFFFFFF);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setBackgroundResource(R.drawable.bg_table_cell_cpy);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder,
                                     @SuppressLint("RecyclerView") int position) {

            holder.textView.setText(data.get(position).transBatchId);

            holder.textView.setBackgroundResource(
                    position == currentIndex
                            ? R.drawable.bg_table_cell_selected
                            : R.drawable.bg_table_cell_cpy
            );

            holder.itemView.setOnClickListener(v -> {
                currentIndex = position;
                notifyDataSetChanged();
                updateUi();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(TextView tv) {
                super(tv);
                textView = tv;
            }
        }
    }

    @Override
    public void onBackPressed() {
        goToOutbound();
    }
}
