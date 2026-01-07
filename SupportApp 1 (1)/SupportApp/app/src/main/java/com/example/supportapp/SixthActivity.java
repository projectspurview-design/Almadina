package com.example.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.Arrays;
import java.util.List;

public class SixthActivity extends AppCompatActivity {

    private static final String TAG = "SixthActivity";

    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<String> options = Arrays.asList("Inbound", "Outbound", "Inventory");
    private int selectedPosition = 1; // Initially "Outbound"
    private OptionAdapter adapter;

    private VuzixSpeechClient speechClient;

    private static final int KEYCODE_NEXT = KeyEvent.KEYCODE_B;
    private static final int KEYCODE_UP = KeyEvent.KEYCODE_C;
    private static final int KEYCODE_DOWN = KeyEvent.KEYCODE_D;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sixth);

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button nextButton = findViewById(R.id.btnNext); // Updated to match new XML ID

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        optionsRecyclerView.setLayoutManager(layoutManager);

        adapter = new OptionAdapter(options);
        optionsRecyclerView.setAdapter(adapter);

        // Set initial selection
        adapter.setSelectedPosition(selectedPosition);
        optionsRecyclerView.scrollToPosition(selectedPosition);

        // Button Listeners
        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());

        nextButton.setOnClickListener(v -> goToNextIfOutbound());

        setupVoiceCommands();
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT);
            speechClient.insertKeycodePhrase("Up", KEYCODE_UP);
            speechClient.insertKeycodePhrase("Down", KEYCODE_DOWN);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient: " + e.getMessage());
        }
    }

    private void moveUp() {
        if (selectedPosition > 0) {
            selectedPosition--;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
        }
    }

    private void moveDown() {
        if (selectedPosition < options.size() - 1) {
            selectedPosition++;
            adapter.setSelectedPosition(selectedPosition);
            optionsRecyclerView.smoothScrollToPosition(selectedPosition);
        }
    }

    private void goToNextIfOutbound() {
        if (selectedPosition == 1) {
            Intent intent = new Intent(SixthActivity.this, SeventhActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_NEXT:
                goToNextIfOutbound();
                return true;
            case KEYCODE_UP:
                moveUp();
                return true;
            case KEYCODE_DOWN:
                moveDown();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // speechClient.shutdown(); // Not needed in latest SDK
    }
}