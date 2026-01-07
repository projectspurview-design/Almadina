package com.example.Pickbyvision.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Pickbyvision.Consolidated_Pick.ConsolidatedTransactionActivity;
import com.example.Pickbyvision.Induvidual_Pick.adapter.OptionAdapter;
import com.example.Pickbyvision.Induvidual_Pick.data.UserSessionManager;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.example.Pickbyvision.voice.VoiceCommandCenter.Actions;

import java.util.Arrays;
import java.util.List;

public class OutBoundActivity extends AppCompatActivity {

    private RecyclerView optionsRecyclerView;
    private final List<String> options =
            Arrays.asList("Pick - Individual", "Pick - Consolidated");

    private int selectedPosition = 0;
    private OptionAdapter adapter;

    private String userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.outboundactivity);


        userId = UserSessionManager.getUserId(this);
        userName = UserSessionManager.getUserName(this);

        if (userId == null || userId.isEmpty()) {
            LogoutManager.performLogout(this);
            return;
        }

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        optionsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        adapter = new OptionAdapter(options);
        optionsRecyclerView.setAdapter(adapter);
        adapter.setSelectedPosition(selectedPosition);

        findViewById(R.id.arrowUp).setOnClickListener(v -> moveUp());
        findViewById(R.id.arrowDown).setOnClickListener(v -> moveDown());
        findViewById(R.id.btnNext).setOnClickListener(v -> goNext());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.logoutButton)
                .setOnClickListener(v -> LogoutManager.performLogout(this));

        VoiceCommandCenter.init(this);
    }

    private void moveUp() {
        if (selectedPosition > 0) {
            selectedPosition--;
            adapter.setSelectedPosition(selectedPosition);
        }
    }

    private void moveDown() {
        if (selectedPosition < options.size() - 1) {
            selectedPosition++;
            adapter.setSelectedPosition(selectedPosition);
        }
    }

    private void goNext() {

        if (selectedPosition == 0) {
            startActivity(new Intent(this, OrdersActivity.class));
        } else {
            startActivity(new Intent(this, ConsolidatedTransactionActivity.class));
        }
    }

    private final Actions voiceActions = new Actions() {
        @Override public void onNext() { goNext(); }
        @Override public void onBack() { finish(); }
        @Override public void onScrollUp() { moveUp(); }
        @Override public void onScrollDown() { moveDown(); }
        @Override public void onSelect() { goNext(); }

        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}

        @Override public void onIndividual() {
            selectedPosition = 0;
            adapter.setSelectedPosition(0);
        }

        @Override public void onConsolidated() {
            selectedPosition = 1;
            adapter.setSelectedPosition(1);
        }

        @Override public void onLogout() {
            LogoutManager.performLogout(OutBoundActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)
                || super.onKeyDown(keyCode, event);
    }
}
