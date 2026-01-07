package com.example.supportapp.Consolidated_Pick;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.supportapp.Induvidual_Pick.manager.LogoutManager;
import com.example.supportapp.Consolidated_Pick.model.Location;
import com.example.supportapp.Consolidated_Pick.repo.ConsolidatedRepository;
import com.example.supportapp.R;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LocationDetailsActivity extends AppCompatActivity {

    private static final String TAG = "LocationDetails";
    private String loginId;

    private String consolidatedTransactionId;
    private String companyCode;
    private String prinCode;
    private String pickUser;
    private ScrollView tableScrollView;

    private TableLayout tableLayout;
    private AppCompatButton backButton, nextButton;
    private TextView tvPage, tvNoLocations;
    private View arrowUp, arrowDown;

    private int currentPage = 1;
    private int rowsPerPage = 10;
    private List<Location> allLocations = Collections.emptyList();

    private int selectedRowIndex = -1;
    private int currentPageStartIndex = 0;

    // Highlight-based continuous scrolling flags
    private boolean isScrollingDown = false;
    private boolean isScrollingUp = false;
    private static final long HIGHLIGHT_SCROLL_DELAY_MS = 500L;

    private Handler scrollHandler = new Handler();
    private Runnable scrollDownRunnable;
    private Runnable scrollUpRunnable;

    private final ConsolidatedRepository repository = new ConsolidatedRepository();

    private VuzixSpeechClient speechClient;
    private static final int KEYCODE_GO_DOWN = 1001;
    private static final int KEYCODE_GO_UP = 1000;

    private static final int KEYCODE_BACK_VOICE = KeyEvent.KEYCODE_F1;
    private static final int KEYCODE_NEXT_VOICE = KeyEvent.KEYCODE_F2;
    private static final int KEYCODE_UP_VOICE = KeyEvent.KEYCODE_F3;
    private static final int KEYCODE_DOWN_VOICE = KeyEvent.KEYCODE_F4;
    private static final int KEYCODE_LOGOUT_VOICE = KeyEvent.KEYCODE_F5;

    // Scroll Up / Down (voice phrases "Scroll Up"/"Scroll Down")
    private static final int KEYCODE_SCROLL_UP = KeyEvent.KEYCODE_F9;
    private static final int KEYCODE_SCROLL_DOWN = KeyEvent.KEYCODE_F10;

    // NEW: "Stop" to stop continuous scroll
    private static final int KEYCODE_STOP_SCROLL = KeyEvent.KEYCODE_F8;

    private boolean selectOnly = false;
    private String preselectLocationCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_details);

        tableScrollView = findViewById(R.id.scrollViewTable);
        tableLayout = findViewById(R.id.tblLocations);
        tvPage = findViewById(R.id.tvPage);
        tvNoLocations = findViewById(R.id.tvNoLocations);
        arrowUp = findViewById(R.id.arrowUp);
        arrowDown = findViewById(R.id.arrowDown);

        pickUser = getIntent().getStringExtra("PICK_USER");
        consolidatedTransactionId = getIntent().getStringExtra("TRANS_BATCH_ID");
        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        prinCode = getIntent().getStringExtra("PRIN_CODE");

        loginId = getIntent().getStringExtra("LOGIN_ID");
        if (loginId == null || loginId.trim().isEmpty()) {
            loginId = n(pickUser);
        }

        backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> goBackToTransactionList());

        nextButton = findViewById(R.id.btnNext);

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> LogoutManager.performLogout(LocationDetailsActivity.this));

        nextButton.setOnClickListener(v -> {
            if (selectedRowIndex >= 0 && selectedRowIndex < allLocations.size()) {
                Location selectedLocation = allLocations.get(selectedRowIndex);
                int totalQuantitySum = calculateTotalQuantitySum();

                if (selectOnly) {
                    Intent result = new Intent();
                    result.putExtra("SITE_CODE", n(selectedLocation.getSiteCode()));
                    result.putExtra("LOCATION_CODE", n(selectedLocation.getLocationCode()));
                    result.putExtra("QUANTITY", selectedLocation.getQuantity());
                    result.putExtra("TRANS_BATCH_ID", n(selectedLocation.getTransBatchId()));
                    result.putExtra("COMPANY_CODE", n(selectedLocation.getCompanyCode()));
                    result.putExtra("PRIN_CODE", n(selectedLocation.getPrinCode()));
                    result.putExtra("JOB_NO", n(selectedLocation.getJobNo()));
                    result.putExtra("PROD_CODE", n(selectedLocation.getProdCode()));
                    result.putExtra("PICK_QTY", selectedLocation.getPickQty());
                    result.putExtra("ORDER_NO", n(selectedLocation.getOrderNo()));
                    result.putExtra("TOTAL_QUANTITY_SUM", totalQuantitySum);
                    result.putExtra("SELECTED_INDEX", selectedRowIndex);

                    // ✅ ADD THIS: Pass the current location index
                    result.putExtra("CURRENT_LOCATION_INDEX", selectedRowIndex);
                    result.putExtra("TOTAL_ITEMS", allLocations.size());

                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    Intent intent = new Intent(LocationDetailsActivity.this, ConsolidatedJobActivity.class);
                    intent.putExtra("SITE_CODE", selectedLocation.getSiteCode());
                    intent.putExtra("LOCATION_CODE", selectedLocation.getLocationCode());
                    intent.putExtra("QUANTITY", selectedLocation.getQuantity());
                    intent.putExtra("TRANS_BATCH_ID", selectedLocation.getTransBatchId());
                    intent.putExtra("PICK_USER", pickUser);
                    intent.putExtra("COMPANY_CODE", selectedLocation.getCompanyCode());
                    intent.putExtra("PRIN_CODE", selectedLocation.getPrinCode());
                    intent.putExtra("JOB_NO", selectedLocation.getJobNo());
                    intent.putExtra("PROD_CODE", selectedLocation.getProdCode());
                    intent.putExtra("PICK_QTY", selectedLocation.getPickQty());
                    intent.putExtra("ORDER_NO", selectedLocation.getOrderNo());
                    intent.putExtra("TOTAL_ITEMS", allLocations.size());  // 2 or 3 or whatever
                    intent.putExtra("FRESH_START", true);  // 🔑 KEY FLAG - triggers re-locking!
                    intent.putExtra("TOTAL_QUANTITY_SUM", totalQuantitySum);

                    // ✅ ADD THIS: Pass the current location index
                    intent.putExtra("CURRENT_LOCATION_INDEX", selectedRowIndex);

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

        selectOnly = intent.getBooleanExtra("SELECT_ONLY", false);
        preselectLocationCode = n(intent.getStringExtra("CURRENT_LOCATION_CODE"));

        arrowUp.setOnClickListener(v -> navigateUp());
        arrowDown.setOnClickListener(v -> navigateDown());

        setupVoiceCommands();
        initializeScrollRunnables();

        if (consolidatedTransactionId != null && companyCode != null && prinCode != null && pickUser != null) {
            fetchLocationDetails(companyCode, prinCode, consolidatedTransactionId, pickUser);
        } else {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show();
        }
    }

    /** Continuous HIGHLIGHT scroll runnables (not pixel scroll) */
    private void initializeScrollRunnables() {
        scrollDownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScrollingDown) return;

                if (allLocations == null || allLocations.isEmpty()
                        || selectedRowIndex >= allLocations.size() - 1) {
                    // Reached bottom or no data → stop
                    isScrollingDown = false;
                    return;
                }

                navigateDown();
                scrollHandler.postDelayed(this, HIGHLIGHT_SCROLL_DELAY_MS);
            }
        };

        scrollUpRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScrollingUp) return;

                if (allLocations == null || allLocations.isEmpty()
                        || selectedRowIndex <= 0) {
                    // Reached top or no data → stop
                    isScrollingUp = false;
                    return;
                }

                navigateUp();
                scrollHandler.postDelayed(this, HIGHLIGHT_SCROLL_DELAY_MS);
            }
        };
    }

    private int calculateTotalQuantitySum() {
        int totalSum = 0;
        if (allLocations != null && !allLocations.isEmpty()) {
            for (Location location : allLocations) {
                totalSum += location.getQuantity();
            }
        }
        Log.d(TAG, "Total quantity sum calculated: " + totalSum);
        return totalSum;
    }

    private void setupVoiceCommands() {
        try {

            speechClient = new VuzixSpeechClient(this);

            speechClient.deletePhrase("OK");
            speechClient.deletePhrase("Ok");
            speechClient.deletePhrase("Okay");
            speechClient.deletePhrase("CLOSE");
            speechClient.deletePhrase("Close");


            speechClient.insertKeycodePhrase("Back", KEYCODE_BACK_VOICE);
            speechClient.insertKeycodePhrase("Next", KEYCODE_NEXT_VOICE);
            speechClient.insertKeycodePhrase("Up", KEYCODE_UP_VOICE);
            speechClient.insertKeycodePhrase("Down", KEYCODE_DOWN_VOICE);
            speechClient.insertKeycodePhrase("Logout", KEYCODE_LOGOUT_VOICE);

            // Continuous highlight scroll
            speechClient.insertKeycodePhrase("Scroll Up", KEYCODE_SCROLL_UP);
            speechClient.insertKeycodePhrase("Scroll Down", KEYCODE_SCROLL_DOWN);
            speechClient.insertKeycodePhrase("Stop", KEYCODE_STOP_SCROLL);

            // Single-step movement
            speechClient.insertKeycodePhrase("Go Down", KEYCODE_GO_DOWN);
            speechClient.insertKeycodePhrase("Go Up", KEYCODE_GO_UP);

            Log.d(TAG, "Voice commands registered");
        } catch (Exception e) {
            Log.e(TAG, "VuzixSpeechClient init failed: " + e.getMessage());
        }
    }

    private void goBackToTransactionList() {
        Intent intent = new Intent(this, ConsolidatedTransactionActivity.class);
        intent.putExtra("COMPANY_CODE", companyCode);
        intent.putExtra("LOGIN_ID", loginId);
        intent.putExtra("PICK_USER", pickUser);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_BACK_VOICE:
                goBackToTransactionList();
                return true;

            case KEYCODE_NEXT_VOICE:
                if (nextButton != null && nextButton.isEnabled()) {
                    nextButton.performClick();
                }
                return true;

            // Single-step Up (from voice "Up" / "Go Up")
            case KeyEvent.KEYCODE_DPAD_UP:
            case KEYCODE_GO_UP:
            case KEYCODE_UP_VOICE:
                // If Next is focused and user presses UP, move focus back off the button
                if (nextButton != null && nextButton.isFocused()) {
                    nextButton.clearFocus();
                    return true;
                }
                navigateUp();
                return true;

            // Single-step Down (from voice "Down" / "Go Down")
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KEYCODE_GO_DOWN:
            case KEYCODE_DOWN_VOICE:
                navigateDown();
                return true;

            // SINGLE SELECT (tap / enter / "select this")
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (nextButton != null && nextButton.isEnabled()) {
                    if (!nextButton.isFocused()) {
                        // First tap: move highlight to Next button
                        nextButton.requestFocus();
                    } else {
                        // Second tap: actually press Next
                        nextButton.performClick();
                    }
                    return true;
                }
                break;

            case KEYCODE_LOGOUT_VOICE:
                LogoutManager.performLogout(LocationDetailsActivity.this);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void fetchLocationDetails(String companyCode, String prinCode, String transactionId, String pickUser) {
        new Thread(() -> {
            try {
                allLocations = repository.getConsolidatedPicklistBlocking(
                        companyCode, prinCode, transactionId, pickUser
                );

                if (allLocations != null) {
                    allLocations.removeIf(loc -> loc == null || !transactionId.equals(loc.getTransBatchId()));
                } else {
                    allLocations = Collections.emptyList();
                }

                final int explicitIndex = getIntent().getIntExtra("SELECTED_INDEX", -1);

                runOnUiThread(() -> {
                    if (allLocations == null || allLocations.isEmpty()) {
                        tableLayout.setVisibility(View.GONE);
                        tvNoLocations.setVisibility(View.VISIBLE);
                        tvPage.setText("0/0");
                        arrowUp.setVisibility(View.GONE);
                        arrowDown.setVisibility(View.GONE);
                        selectedRowIndex = -1;
                        currentPage = 1;
                    } else {
                        tableLayout.setVisibility(View.VISIBLE);
                        tvNoLocations.setVisibility(View.GONE);

                        if (selectedRowIndex >= allLocations.size()) {
                            selectedRowIndex = Math.max(0, allLocations.size() - 1);
                        }

                        if (explicitIndex >= 0 && explicitIndex < allLocations.size()) {
                            selectedRowIndex = explicitIndex;
                            currentPage = (selectedRowIndex / rowsPerPage) + 1;
                            Log.d(TAG, "Preselecting by explicit index: " + selectedRowIndex);
                        } else if (!preselectLocationCode.isEmpty()) {
                            int idx = -1;
                            for (int i = 0; i < allLocations.size(); i++) {
                                Location loc = allLocations.get(i);
                                if (loc != null && preselectLocationCode.equalsIgnoreCase(n(loc.getLocationCode()))) {
                                    idx = i;
                                    break;
                                }
                            }
                            if (idx >= 0) {
                                selectedRowIndex = idx;
                                currentPage = (selectedRowIndex / rowsPerPage) + 1;
                                Log.d(TAG, "Preselecting by location code at index: " + selectedRowIndex);
                            } else {
                                currentPage = 1;
                                selectedRowIndex = 0;
                                Log.d(TAG, "Preselect location code not found; defaulting to index 0");
                            }
                        } else {
                            if (selectedRowIndex < 0 || selectedRowIndex >= allLocations.size()) {
                                selectedRowIndex = 0;
                            }
                            currentPage = (selectedRowIndex / rowsPerPage) + 1;
                            Log.d(TAG, "Default preselect index: " + selectedRowIndex);
                        }

                        displayCurrentPage();

                        if (allLocations.size() > 1) {
                            arrowUp.setVisibility(View.VISIBLE);
                            arrowDown.setVisibility(View.VISIBLE);
                        } else {
                            arrowUp.setVisibility(View.GONE);
                            arrowDown.setVisibility(View.GONE);
                        }
                        updateArrowStates();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error fetching locations: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
                Log.e(TAG, "fetchLocationDetails failed: " + e.getMessage(), e);
            }
        }).start();
    }

    private void displayCurrentPage() {
        tableLayout.removeAllViews();

        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        addHeaderCell(headerRow, "Site");
        addHeaderCell(headerRow, "Location");
        addHeaderCell(headerRow, "Qty");
        tableLayout.addView(headerRow);

        currentPageStartIndex = (currentPage - 1) * rowsPerPage;
        int end = Math.min(currentPageStartIndex + rowsPerPage, allLocations.size());

        for (int i = currentPageStartIndex; i < end; i++) {
            Location loc = allLocations.get(i);
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            final boolean isSelected = (i == selectedRowIndex);

            addDataCell(row, loc.getSiteCode(), isSelected);
            addDataCell(row, loc.getLocationCode(), isSelected);
            addDataCell(row, String.valueOf(loc.getQuantity()), isSelected);

            final int rowIndex = i;
            row.setOnClickListener(v -> {
                selectedRowIndex = rowIndex;
                refreshRowHighlights();
                updateArrowStates();

                if (nextButton != null && nextButton.isEnabled()) {
                    nextButton.requestFocus();
                }
            });

            tableLayout.addView(row);
        }

        updatePageInfo();
        updateArrowStates();

        tableLayout.post(this::scrollToSelectedRow);
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            View focusView = getCurrentFocus();

            // Physical BACK → always go back
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goBackToTransactionList();
                return true;
            }

            // Handle STOP (voice "Stop") → stop continuous highlight scrolling
            if (keyCode == KEYCODE_STOP_SCROLL) {
                stopHighlightScroll();
                return true;
            }

            // ---------- WHEN NEXT BUTTON IS FOCUSED ----------
            if (nextButton != null && focusView == nextButton) {
                switch (keyCode) {
                    // LEFT of Next => Back button
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (backButton != null) {
                            backButton.requestFocus();
                        } else {
                            nextButton.clearFocus();
                        }
                        return true;

                    // UP of Next => Table (exit Next focus)
                    case KeyEvent.KEYCODE_DPAD_UP:
                        nextButton.clearFocus();
                        if (tableLayout != null) {
                            tableLayout.requestFocus();
                        }
                        return true;

                    // Single tap / enter on Next => perform Next
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        handleSelect();
                        return true;

                    // Block other directions & scroll keys while on Next
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KEYCODE_SCROLL_UP:
                    case KEYCODE_SCROLL_DOWN:
                    case KEYCODE_GO_UP:
                    case KEYCODE_GO_DOWN:
                        return true;
                }
            }

            // ⭐ SPECIAL: LEFT from UP ARROW **or DOWN ARROW** → go back to TABLE (not Back button)
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                    (focusView == arrowUp || focusView == arrowDown)) {

                if (tableLayout != null) {
                    tableLayout.requestFocus();
                }
                return true;
            }

            // ANYWHERE (except when Next / special handled): LEFT → Back
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (backButton != null && focusView != backButton) {
                    backButton.requestFocus();
                    return true;
                }
            }

            // TABLE AREA: RIGHT => UP ARROW
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && isViewInTable(focusView)) {
                if (arrowUp != null) {
                    arrowUp.requestFocus();
                }
                return true;
            }

            // CLICK on Up/Down arrows (Center/Enter) => only navigateUp/Down
            if ((focusView == arrowUp || focusView == arrowDown) &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {

                if (focusView == arrowUp) {
                    navigateUp();
                } else {
                    navigateDown();
                }
                return true;
            }

            // UP ARROW: DOWN => DOWN ARROW
            if (arrowUp != null && focusView == arrowUp) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (arrowDown != null) {
                        arrowDown.requestFocus();
                    }
                    return true;
                }
            }

            // DOWN ARROW: UP => UP ARROW, DOWN => NEXT
            if (arrowDown != null && focusView == arrowDown) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (arrowUp != null) {
                        arrowUp.requestFocus();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (nextButton != null) {
                        nextButton.requestFocus();
                    }
                    return true;
                }
            }

            // CONTINUOUS HIGHLIGHT SCROLL (voice "Scroll Up/Down")
            if (keyCode == KEYCODE_SCROLL_DOWN) {
                startHighlightScrollDown();
                return true;
            }

            if (keyCode == KEYCODE_SCROLL_UP) {
                startHighlightScrollUp();
                return true;
            }

            // NORMAL TABLE / BACK / NEXT SELECT HANDLING
            switch (keyCode) {
                // NOTE: SCROLL_UP/SCROLL_DOWN removed from here so they don't step once
                case KeyEvent.KEYCODE_DPAD_UP:
                case KEYCODE_GO_UP:
                case KEYCODE_UP_VOICE:
                    navigateUp();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KEYCODE_GO_DOWN:
                case KEYCODE_DOWN_VOICE:
                    navigateDown();
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    handleSelect();
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void startHighlightScrollDown() {
        isScrollingDown = true;
        isScrollingUp = false;
        scrollHandler.removeCallbacks(scrollUpRunnable);
        scrollHandler.post(scrollDownRunnable);
    }

    private void startHighlightScrollUp() {
        isScrollingUp = true;
        isScrollingDown = false;
        scrollHandler.removeCallbacks(scrollDownRunnable);
        scrollHandler.post(scrollUpRunnable);
    }

    private void stopHighlightScroll() {
        isScrollingDown = false;
        isScrollingUp = false;
        scrollHandler.removeCallbacks(scrollDownRunnable);
        scrollHandler.removeCallbacks(scrollUpRunnable);
    }

    private void handleSelect() {
        View focusView = getCurrentFocus();

        if (focusView == backButton) {
            backButton.performClick();
            return;
        }

        if (nextButton == null || !nextButton.isEnabled()) return;

        if (focusView == nextButton) {
            nextButton.performClick();
        } else {
            nextButton.requestFocus();
        }
    }

    private boolean isViewInTable(View view) {
        if (view == null) return false;
        if (view == tableLayout) return true;

        View parent = (View) view.getParent();
        while (parent != null) {
            if (parent == tableLayout) {
                return true;
            }
            if (parent.getParent() instanceof View) {
                parent = (View) parent.getParent();
            } else {
                parent = null;
            }
        }
        return false;
    }

    private void addDataCell(TableRow row, String text, boolean isSelected) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(16f);
        tv.setPadding(8, 12, 8, 12);

        if (isSelected) {
            tv.setBackgroundResource(R.drawable.bg_table_cell_highlight);
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        } else {
            tv.setBackgroundResource(R.drawable.bg_table_cell);
            tv.setTextColor(Color.WHITE);
        }

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(tv);
    }

    private void refreshRowHighlights() {
        int childCount = tableLayout.getChildCount();

        for (int i = 1; i < childCount; i++) { // skip header
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                int dataIndex = currentPageStartIndex + (i - 1);
                boolean shouldHighlight = (dataIndex == selectedRowIndex);

                for (int j = 0; j < row.getChildCount(); j++) {
                    View cellView = row.getChildAt(j);
                    if (cellView instanceof TextView) {
                        TextView tv = (TextView) cellView;
                        if (shouldHighlight) {
                            tv.setBackgroundResource(R.drawable.bg_table_cell_highlight);
                            tv.setTextColor(Color.BLACK);
                            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
                        } else {
                            tv.setBackgroundResource(R.drawable.bg_table_cell);
                            tv.setTextColor(Color.WHITE);
                            tv.setTypeface(null, Typeface.NORMAL);
                        }
                    }
                }
            }
        }

        updatePageInfo();
    }

    private void navigateUp() {
        if (allLocations == null || allLocations.isEmpty()) return;

        if (selectedRowIndex <= 0) {
            Log.d(TAG, "Already at first item");
            return;
        }

        selectedRowIndex--;
        Log.d(TAG, "Navigate UP to index: " + selectedRowIndex);

        int newPage = (selectedRowIndex / rowsPerPage) + 1;

        if (newPage != currentPage) {
            currentPage = newPage;
            displayCurrentPage();
        } else {
            refreshRowHighlights();
            scrollToSelectedRow();
        }

        updateArrowStates();
    }

    private void navigateDown() {
        if (allLocations == null || allLocations.isEmpty()) return;

        if (selectedRowIndex >= allLocations.size() - 1) {
            Log.d(TAG, "Already at last item");
            return;
        }

        selectedRowIndex++;
        Log.d(TAG, "Navigate DOWN to index: " + selectedRowIndex);

        int newPage = (selectedRowIndex / rowsPerPage) + 1;

        if (newPage != currentPage) {
            currentPage = newPage;
            displayCurrentPage();
        } else {
            refreshRowHighlights();
            scrollToSelectedRow();
        }

        updateArrowStates();
    }

    /** Only scroll enough to keep selected row visible (not centering every time) */
    private void scrollToSelectedRow() {
        if (tableScrollView == null || tableLayout == null) return;

        int rowIndexInPage = selectedRowIndex - currentPageStartIndex;
        if (rowIndexInPage >= 0 && rowIndexInPage < (tableLayout.getChildCount() - 1)) {
            View selectedRow = tableLayout.getChildAt(rowIndexInPage + 1); // +1 for header
            if (selectedRow != null) {
                int rowTop = selectedRow.getTop();
                int rowBottom = selectedRow.getBottom();
                int scrollY = tableScrollView.getScrollY();
                int height = tableScrollView.getHeight();

                int newScrollY = scrollY;

                if (rowTop < scrollY) {
                    newScrollY = rowTop;
                } else if (rowBottom > scrollY + height) {
                    newScrollY = rowBottom - height;
                }

                if (newScrollY != scrollY) {
                    tableScrollView.smoothScrollTo(0, Math.max(0, newScrollY));
                }
            }
        }
    }

    private void updateArrowStates() {
        boolean hasData = allLocations != null && !allLocations.isEmpty();
        arrowUp.setEnabled(hasData && selectedRowIndex > 0);
        arrowDown.setEnabled(hasData && selectedRowIndex < allLocations.size() - 1);
        arrowUp.setAlpha((hasData && selectedRowIndex > 0) ? 1f : 0.4f);
        arrowDown.setAlpha((hasData && selectedRowIndex < allLocations.size() - 1) ? 1f : 0.4f);
    }

    private void updatePageInfo() {
        int total = (allLocations == null) ? 0 : allLocations.size();
        int current = (selectedRowIndex >= 0 && selectedRowIndex < total) ? (selectedRowIndex + 1) : 0;
        tvPage.setText(current + "/" + total);
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onBackPressed() {
        goBackToTransactionList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopHighlightScroll();
        speechClient = null;
        scrollHandler.removeCallbacksAndMessages(null);
    }
}
