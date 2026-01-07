package com.example.Pickbyvision.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Pickbyvision.Induvidual_Pick.adapter.OptionAdapter;
import com.example.Pickbyvision.Induvidual_Pick.data.UserSessionManager;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;
import com.example.Pickbyvision.voice.VoiceCommandCenter.Actions;

import java.util.Arrays;
import java.util.List;

public class ProcessActivity extends AppCompatActivity {

    private static final String TAG = "ProcessActivity";

    private RecyclerView optionsRecyclerView;
    private final List<String> options =
            Arrays.asList("Inbound", "Outbound", "Inventory");

    private int selectedPosition = 1;
    private OptionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.processactivity);


        if (!UserSessionManager.isLoggedIn(this)) {
            Log.e(TAG, "Session missing → logout");
            LogoutManager.performLogout(this);
            return;
        }

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button nextButton = findViewById(R.id.btnNext);
        Button backButton = findViewById(R.id.btnBack);
        Button logoutButton = findViewById(R.id.logoutButton);

        optionsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        adapter = new OptionAdapter(options);
        optionsRecyclerView.setAdapter(adapter);

        adapter.setSelectedPosition(selectedPosition);

        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());
        backButton.setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> goNext());

        logoutButton.setOnClickListener(v ->
                LogoutManager.performLogout(this));

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
        if (selectedPosition == 1) {
            startActivity(new Intent(this, OutBoundActivity.class));
        }
    }

    private final Actions voiceActions = new Actions() {
        @Override public void onNext() { goNext(); }
        @Override public void onBack() { finish(); }
        @Override public void onScrollUp() { moveUp(); }
        @Override public void onScrollDown() { moveDown(); }
        @Override public void onSelect() { goNext(); }

        @Override public void onInbound() { selectedPosition = 0; }
        @Override public void onOutbound() { selectedPosition = 1; }
        @Override public void onInventory() { selectedPosition = 2; }

        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}

        @Override public void onLogout() {
            LogoutManager.performLogout(ProcessActivity.this);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
