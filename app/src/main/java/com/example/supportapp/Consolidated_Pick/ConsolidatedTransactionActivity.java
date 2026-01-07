package com.example.supportapp.Consolidated_Pick;

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

import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.Consolidated_Pick.model.ConsolidatedId;
import com.example.supportapp.Consolidated_Pick.network.ConsolidatedPickingApi;
import com.example.supportapp.Consolidated_Pick.network.RetrofitClientDev;
import com.example.supportapp.Consolidated_Pick.session.SessionHelper;
import com.example.supportapp.R;
import com.example.supportapp.Induvidual_Pick.OutBoundActivity;
import com.example.supportapp.voice.VoiceCommandCenter;
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
    private SessionHelper session;

    // Centralized voice actions for this screen
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

        rvIds = findViewById(R.id.rvIds);
        arrowUp = findViewById(R.id.arrowUp);
        arrowDown = findViewById(R.id.arrowDown);
        backButton = findViewById(R.id.backButton);
        btnNext = findViewById(R.id.btnNext);

        // Setup RecyclerView
        adapter = new IdsAdapter();
        rvIds.setLayoutManager(new LinearLayoutManager(this));
        rvIds.setAdapter(adapter);

        backButton.setOnClickListener(v -> goToOutbound());

        btnNext.setOnClickListener(v -> goNext());
        arrowUp.setOnClickListener(v -> moveUp());
        arrowDown.setOnClickListener(v -> moveDown());

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> LogoutManager.performLogout(ConsolidatedTransactionActivity.this));

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
            Log.e(TAG, "VuzixSpeechClient init failed: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register after lifecycle changes so voice keeps working
        VoiceCommandCenter.init(this);
    }
    private void goToOutbound() {
        Intent intent = new Intent(this, OutBoundActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 1. Check for specific D-Pad events *before* the VoiceCommandCenter

        // --- Logic for when focus is on UP Arrow (arrowUp) ---
        if (arrowUp != null && arrowUp.isFocused()) {
            // ... (existing logic for arrowUp)
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // Left (Swipe) or Up D-Pad goes to the table (RecyclerView)
                if (rvIds != null) {
                    rvIds.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // Down D-Pad goes to the down arrow
                if (arrowDown != null) {
                    arrowDown.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Right D-Pad/Swipe does nothing
                return true;
            }
        }

        // --- Logic for when focus is on DOWN Arrow (arrowDown) ---
        if (arrowDown != null && arrowDown.isFocused()) {
            // ... (existing logic for arrowDown)
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Right D-Pad/Swipe goes to the Back button
                if (backButton != null) {
                    backButton.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // Up D-Pad/Swipe goes to the up arrow
                if (arrowUp != null) {
                    arrowUp.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // Left D-Pad/Swipe does nothing
                if (rvIds != null) {
                    rvIds.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // Down D-Pad/Swipe goes to the Next button
                if (btnNext != null) {
                    btnNext.requestFocus();
                    return true;
                }
            }
        }

        // --- Logic for when focus is on NEXT Button (btnNext) ---
        if (btnNext != null && btnNext.isFocused()) {
            // ... (existing logic for btnNext)
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // Left D-Pad/Swipe goes to the Back button
                if (backButton != null) {
                    backButton.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // Up D-Pad/Swipe goes to the table (RecyclerView)
                if (rvIds != null) {
                    rvIds.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // Right D-Pad/Swipe or Down D-Pad/Swipe does nothing
                return true;
            }
        }

        // --- Logic for when focus is on BACK Button (backButton) ---
        if (backButton != null && backButton.isFocused()) {
            // ... (existing logic for backButton)
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // Left D-Pad/Swipe goes to the down arrow
                if (arrowDown != null) {
                    arrowDown.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Right D-Pad/Swipe goes to the Next button
                if (btnNext != null) {
                    btnNext.requestFocus();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // Up D-Pad/Swipe goes to the table (RecyclerView)
                if (rvIds != null) {
                    rvIds.requestFocus();
                    return true;
                }
            }
            // Note: DPAD_DOWN falls through to default Android handling.
        }

        // --- NEW Logic for when focus is on the RecyclerView (Table) ---
        if (rvIds != null && rvIds.hasFocus()) {

            // **IMPORTANT: The adapter.onBindViewHolder handles the selection update when D-PAD UP/DOWN is pressed,
            // which changes the currentIndex. We check the boundary conditions here.**

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // If on the first item and D-Pad Left, go to Back button
                if (currentIndex == 0) {
                    if (backButton != null) {
                        backButton.requestFocus();
                        return true;
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // If on the first item and D-Pad Right, go to Up arrow
                if (currentIndex == 0) {
                    if (arrowUp != null) {
                        arrowUp.requestFocus();
                        return true;
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // If on the LAST item and D-Pad Down, go to Next button
                if (data != null && !data.isEmpty() && currentIndex == data.size() - 1) {
                    if (btnNext != null) {
                        btnNext.requestFocus();
                        return true;
                    }
                }
            }
        }

        // 2. Centralized key routing for voice
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;

        // 3. Default Android key handling
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
                    btnNext.setEnabled(false);
                    Toast.makeText(ConsolidatedTransactionActivity.this, "No transactions found", Toast.LENGTH_SHORT).show();
                } else {
                    currentIndex = 0;
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

        adapter.notifyDataSetChanged();

        // Scroll to current selection
        rvIds.scrollToPosition(currentIndex);

        arrowUp.setAlpha(currentIndex > 0 ? 1f : 0.4f);
        arrowUp.setEnabled(currentIndex > 0);

        boolean hasDown = currentIndex < data.size() - 1;
        arrowDown.setAlpha(hasDown ? 1f : 0.4f);
        arrowDown.setEnabled(hasDown);
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

        // 🚀 Jump straight to LocationDetailsActivity
        Intent i = new Intent(this, LocationDetailsActivity.class);
        i.putExtra("TRANS_BATCH_ID", transBatchId);
        i.putExtra("COMPANY_CODE",   companyCode);
        i.putExtra("PRIN_CODE",      prinCode);
        i.putExtra("PICK_USER",      pickUser);
        startActivity(i);
    }

    // RecyclerView Adapter
    private class IdsAdapter extends RecyclerView.Adapter<IdsAdapter.ViewHolder> {

        @NonNull

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(0xFFFFFFFF); // white text for normal state
            tv.setTextSize(18);
            tv.setPadding(0, 0, 0, 0); // padding handled in drawable

            // Make it like a full-width pill with margins
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) (8 * parent.getResources().getDisplayMetrics().density);
            lp.setMargins(margin, margin, margin, margin);
            tv.setLayoutParams(lp);

            tv.setGravity(android.view.Gravity.CENTER);

            // default background (non-selected)
            tv.setBackgroundResource(R.drawable.bg_table_cell_cpy);

            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ConsolidatedId item = data.get(position);
            holder.textView.setText(item.transBatchId != null ? item.transBatchId : "");

            if (position == currentIndex) {
                holder.textView.setBackgroundResource(R.drawable.bg_table_cell_selected);
                holder.textView.setTextColor(0xFF003366); // dark blue text when selected (optional)
            } else {
                holder.textView.setBackgroundResource(R.drawable.bg_table_cell_cpy);
                holder.textView.setTextColor(0xFFFFFFFF); // white text for normal state
            }

            holder.itemView.setOnClickListener(v -> {
                currentIndex = position;
                notifyDataSetChanged();
                updateUi();
                if (btnNext != null) btnNext.requestFocus();
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