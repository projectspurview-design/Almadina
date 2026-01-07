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

public class SeventhActivity extends AppCompatActivity {

    private static final String TAG = "SeventhActivity";

    private RecyclerView optionsRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<String> options = Arrays.asList("Pick - Individual", "Pick - Consolidated");
    private int selectedPosition = 0;
    private OptionAdapter adapter;

    private VuzixSpeechClient speechClient;

    private static final int KEYCODE_BACK = KeyEvent.KEYCODE_B;
    private static final int KEYCODE_NEXT = KeyEvent.KEYCODE_C;
    private static final int KEYCODE_UP = KeyEvent.KEYCODE_D;
    private static final int KEYCODE_DOWN = KeyEvent.KEYCODE_E;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seventh);

        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        ImageView arrowUp = findViewById(R.id.arrowUp);
        ImageView arrowDown = findViewById(R.id.arrowDown);
        Button backButton = findViewById(R.id.btnBack); // Updated to match new XML ID
        Button nextButton = findViewById(R.id.btnNext); // Updated to match new XML ID

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        optionsRecyclerView.setLayoutManager(layoutManager);

        adapter = new OptionAdapter(options);
        optionsRecyclerView.setAdapter(adapter);

        adapter.setSelectedPosition(selectedPosition);
        optionsRecyclerView.scrollToPosition(selectedPosition);

        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());

        backButton.setOnClickListener(v -> finish()); // Back button functionality
        nextButton.setOnClickListener(v -> goToNextIfIndividual());

        setupVoiceCommands();
    }

    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);

            speechClient.insertKeycodePhrase("Back", KEYCODE_BACK);
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT);
            speechClient.insertKeycodePhrase("Up", KEYCODE_UP);
            speechClient.insertKeycodePhrase("Down", KEYCODE_DOWN);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VuzixSpeechClient", e);
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

    private void goToNextIfIndividual() {
        if (selectedPosition == 0) {
            Intent intent = new Intent(SeventhActivity.this, EighthActivity.class);
            intent.putExtra("SELECTED_OPTION", options.get(selectedPosition));
            startActivity(intent);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_BACK:
                finish();
                return true;
            case KEYCODE_NEXT:
                goToNextIfIndividual();
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
        // No shutdown() needed for VuzixSpeechClient
    }
}