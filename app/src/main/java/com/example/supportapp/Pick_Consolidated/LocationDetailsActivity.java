package com.example.supportapp.Pick_Consolidated;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent; // ⬅️ added
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapp.LogoutManager;
import com.example.supportapp.Pick_Consolidated.model.Location;
import com.example.supportapp.Pick_Consolidated.repo.ConsolidatedRepository;
import com.example.supportapp.R;
import androidx.appcompat.widget.AppCompatButton;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient; // ⬅️ added

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LocationDetailsActivity extends AppCompatActivity {

    private static final String TAG = "LocationDetails";

    private String consolidatedTransactionId;
    private String companyCode;
    private String prinCode;
    private String pickUser;

    private TableLayout tableLayout;
    private AppCompatButton backButton, nextButton ;
    private TextView tvPage, tvNoLocations;
    private View arrowUp, arrowDown;

    private int currentPage = 1;
    private int rowsPerPage = 10;
    private List<Location> allLocations = Collections.emptyList();

    private int selectedRowIndex = -1;
    private int currentPageStartIndex = 0;

    private final ConsolidatedRepository repository = new ConsolidatedRepository();

    // ===== Voice Commands (same mapping style as ConsolidatedTransactionActivity) =====
    private VuzixSpeechClient speechClient;
    private static final int KEYCODE_BACK_VOICE   = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_NEXT_VOICE   = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_UP_VOICE     = KeyEvent.KEYCODE_F3;  // "Up"
    private static final int KEYCODE_DOWN_VOICE   = KeyEvent.KEYCODE_F4;  // "Down"
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F5;
    private static final int KEYCODE_SCROLL_UP    = KeyEvent.KEYCODE_F9;  // "Scroll Up"
    private static final int KEYCODE_SCROLL_DOWN  = KeyEvent.KEYCODE_F10; // "Scroll Down"

    // ===== NEW: select-only mode support =====
    private boolean selectOnly = false;
    private String preselectLocationCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_details);

        tableLayout   = findViewById(R.id.tblLocations);
        tvPage        = findViewById(R.id.tvPage);
        tvNoLocations = findViewById(R.id.tvNoLocations);
        arrowUp       = findViewById(R.id.arrowUp);
        arrowDown     = findViewById(R.id.arrowDown);

        backButton = findViewById(R.id.btnBack);
        nextButton = findViewById(R.id.btnNext);

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> LogoutManager.performLogout(LocationDetailsActivity.this));

        backButton.setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> {
            if (selectedRowIndex >= 0 && selectedRowIndex < allLocations.size()) {
                Location selectedLocation = allLocations.get(selectedRowIndex);

                if (selectOnly) {
                    // Return result to caller (ConsolidatedJobActivity)
                    Intent result = new Intent();
                    result.putExtra("SITE_CODE",      n(selectedLocation.getSiteCode()));
                    result.putExtra("LOCATION_CODE",  n(selectedLocation.getLocationCode()));
                    result.putExtra("QUANTITY",       selectedLocation.getQuantity());
                    result.putExtra("TRANS_BATCH_ID", n(selectedLocation.getTransBatchId()));
                    result.putExtra("COMPANY_CODE",   n(selectedLocation.getCompanyCode()));
                    result.putExtra("PRIN_CODE",      n(selectedLocation.getPrinCode()));
                    result.putExtra("JOB_NO",         n(selectedLocation.getJobNo()));
                    result.putExtra("PROD_CODE",      n(selectedLocation.getProdCode()));
                    result.putExtra("PICK_QTY",       selectedLocation.getPickQty());
                    result.putExtra("ORDER_NO",       n(selectedLocation.getOrderNo())); // Add this line
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    // Original flow: move forward into ConsolidatedJobActivity
                    Intent intent = new Intent(LocationDetailsActivity.this, ConsolidatedJobActivity.class);
                    intent.putExtra("SITE_CODE", selectedLocation.getSiteCode());
                    intent.putExtra("LOCATION_CODE", selectedLocation.getLocationCode());
                    intent.putExtra("QUANTITY", selectedLocation.getQuantity());
                    intent.putExtra("TRANS_BATCH_ID", selectedLocation.getTransBatchId());

                    intent.putExtra("COMPANY_CODE", selectedLocation.getCompanyCode());
                    intent.putExtra("PRIN_CODE", selectedLocation.getPrinCode());
                    intent.putExtra("JOB_NO", selectedLocation.getJobNo());
                    intent.putExtra("PROD_CODE", selectedLocation.getProdCode());
                    intent.putExtra("PICK_QTY", selectedLocation.getPickQty());
                    intent.putExtra("ORDER_NO", selectedLocation.getOrderNo());

                    intent.putExtra("PICK_USER", pickUser);

                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
            }
        });

        Intent intent = getIntent();
        consolidatedTransactionId = intent.getStringExtra("TRANS_BATCH_ID");
        companyCode = intent.getStringExtra("COMPANY_CODE");
        prinCode = intent.getStringExtra("PRIN_CODE");
        pickUser = intent.getStringExtra("PICK_USER");

        // NEW: read select-only + preselect location (when opened from Jump To)
        selectOnly = intent.getBooleanExtra("SELECT_ONLY", false);
        preselectLocationCode = n(intent.getStringExtra("CURRENT_LOCATION_CODE"));

        arrowUp.setOnClickListener(v -> navigateUp());
        arrowDown.setOnClickListener(v -> navigateDown());

        // 🔊 Init voice commands
        setupVoiceCommands();

        if (consolidatedTransactionId != null && companyCode != null && prinCode != null && pickUser != null) {
            fetchLocationDetails(companyCode, prinCode, consolidatedTransactionId, pickUser);
        } else {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Voice setup & handling =====
    private void setupVoiceCommands() {
        try {
            speechClient = new VuzixSpeechClient(this);
            // base nav
            speechClient.insertKeycodePhrase("Back",   KEYCODE_BACK_VOICE);
            speechClient.insertKeycodePhrase("Next",   KEYCODE_NEXT_VOICE);
            speechClient.insertKeycodePhrase("Up",     KEYCODE_UP_VOICE);
            speechClient.insertKeycodePhrase("Down",   KEYCODE_DOWN_VOICE);
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);
            // scroll synonyms
            speechClient.insertKeycodePhrase("Scroll Up",   KEYCODE_SCROLL_UP);
            speechClient.insertKeycodePhrase("Scroll Down", KEYCODE_SCROLL_DOWN);
            Log.d(TAG, "Voice commands registered (Back/Next/Up/Down/Logout/Scroll Up/Scroll Down)");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_BACK_VOICE:
                finish();
                return true;

            case KEYCODE_NEXT_VOICE:
                if (nextButton != null && nextButton.isEnabled()) nextButton.performClick();
                return true;

            case KEYCODE_UP_VOICE:
            case KEYCODE_SCROLL_UP:
                if (arrowUp != null && arrowUp.isEnabled()) arrowUp.performClick();
                else navigateUp(); // fallback
                return true;

            case KEYCODE_DOWN_VOICE:
            case KEYCODE_SCROLL_DOWN:
                if (arrowDown != null && arrowDown.isEnabled()) arrowDown.performClick();
                else navigateDown(); // fallback
                return true;

            case KEYCODE_LOGOUT_VOICE:
                LogoutManager.performLogout(LocationDetailsActivity.this);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    // ===== End voice handling =====

    private void fetchLocationDetails(String companyCode, String prinCode, String transactionId, String pickUser) {
        new Thread(() -> {
            try {
                allLocations = repository.getConsolidatedPicklistBlocking(
                        companyCode, prinCode, transactionId, pickUser
                );

                allLocations.removeIf(loc -> !transactionId.equals(loc.getTransBatchId()));

                runOnUiThread(() -> {
                    if (allLocations.isEmpty()) {
                        tableLayout.setVisibility(View.GONE);
                        tvNoLocations.setVisibility(View.VISIBLE);
                        tvPage.setText("0/0");
                        arrowUp.setVisibility(View.GONE);
                        arrowDown.setVisibility(View.GONE);
                    } else {
                        tableLayout.setVisibility(View.VISIBLE);
                        tvNoLocations.setVisibility(View.GONE);

                        // NEW: preselect current location when provided
                        if (!preselectLocationCode.isEmpty()) {
                            int idx = -1;
                            for (int i = 0; i < allLocations.size(); i++) {
                                if (preselectLocationCode.equalsIgnoreCase(n(allLocations.get(i).getLocationCode()))) {
                                    idx = i; break;
                                }
                            }
                            if (idx >= 0) {
                                selectedRowIndex = idx;
                                currentPage = (idx / rowsPerPage) + 1;
                            } else {
                                currentPage = 1;
                                selectedRowIndex = 0;
                            }
                        } else {
                            currentPage = 1;
                            selectedRowIndex = 0;
                        }

                        displayCurrentPage();

                        if (allLocations.size() > 1) {
                            arrowUp.setVisibility(View.VISIBLE);
                            arrowDown.setVisibility(View.VISIBLE);
                            updateArrowStates();
                        } else {
                            arrowUp.setVisibility(View.GONE);
                            arrowDown.setVisibility(View.GONE);
                        }
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error fetching locations: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void displayCurrentPage() {
        tableLayout.removeAllViews();

        // Header Row
        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        addHeaderCell(headerRow, "Site");
        addHeaderCell(headerRow, "Location");
        addHeaderCell(headerRow, "Qty");
        tableLayout.addView(headerRow);

        currentPageStartIndex = (currentPage - 1) * rowsPerPage;
        int end = Math.min(currentPageStartIndex + rowsPerPage, allLocations.size());

        // Add only data rows (no empty rows)
        for (int i = currentPageStartIndex; i < end; i++) {
            Location loc = allLocations.get(i);
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            addDataCell(row, loc.getSiteCode(), i == selectedRowIndex);
            addDataCell(row, loc.getLocationCode(), i == selectedRowIndex);
            addDataCell(row, String.valueOf(loc.getQuantity()), i == selectedRowIndex);

            final int rowIndex = i;
            row.setOnClickListener(v -> {
                selectedRowIndex = rowIndex;
                displayCurrentPage();
                updateArrowStates();
            });

            tableLayout.addView(row);
        }

        updatePageInfo();
        updateArrowStates();
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.bg_table_cell_header);
        tv.setPadding(8, 12, 8, 12);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(tv);
    }

    private void addDataCell(TableRow row, String text, boolean isSelected) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        tv.setPadding(8, 12, 8, 12);

        if (isSelected) {
            tv.setBackgroundResource(R.drawable.bg_table_cell_selected);
        } else {
            tv.setBackgroundResource(R.drawable.bg_table_cell);
        }

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(tv);
    }

    private void navigateUp() {
        if (selectedRowIndex > 0) {
            selectedRowIndex--;
            if (selectedRowIndex < currentPageStartIndex) {
                currentPage--;
                displayCurrentPage();
            } else {
                displayCurrentPage();
            }
            updateArrowStates();
        }
    }

    private void navigateDown() {
        if (selectedRowIndex < allLocations.size() - 1) {
            selectedRowIndex++;
            int currentPageEndIndex = currentPageStartIndex + rowsPerPage - 1;
            if (selectedRowIndex > currentPageEndIndex) {
                currentPage++;
                displayCurrentPage();
            } else {
                displayCurrentPage();
            }
            updateArrowStates();
        }
    }

    private void updateArrowStates() {
        arrowUp.setAlpha(selectedRowIndex > 0 ? 1f : 0.4f);
        arrowUp.setEnabled(selectedRowIndex > 0);

        boolean hasDown = selectedRowIndex < allLocations.size() - 1;
        arrowDown.setAlpha(hasDown ? 1f : 0.4f);
        arrowDown.setEnabled(hasDown);
    }

    private void updatePageInfo() {
        int totalPages = (int) Math.ceil(allLocations.size() / (double) rowsPerPage);
        tvPage.setText(currentPage + "/" + totalPages);
    }

    private static String n(String s) { return s == null ? "" : s; }
}
