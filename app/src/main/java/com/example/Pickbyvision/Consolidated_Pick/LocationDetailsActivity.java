package com.example.Pickbyvision.Consolidated_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.Pickbyvision.Consolidated_Pick.adapter.LocationsAdapter;
import com.example.Pickbyvision.Consolidated_Pick.location.Location;
import com.example.Pickbyvision.Consolidated_Pick.repo.ConsolidatedRepository;
import com.example.Pickbyvision.Induvidual_Pick.manager.LogoutManager;
import com.example.Pickbyvision.R;
import com.example.Pickbyvision.voice.VoiceCommandCenter;

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

    private final int rowsPerPage = 10;

    private boolean isScrollingDown = false;
    private boolean isScrollingUp = false;
    private static final long HIGHLIGHT_SCROLL_DELAY_MS = 500L;

    private final Handler scrollHandler = new Handler();
    private Runnable scrollDownRunnable;
    private Runnable scrollUpRunnable;

    private final ConsolidatedRepository repository = new ConsolidatedRepository();

    private boolean selectOnly = false;
    private String preselectLocationCode = "";

    private LocationsAdapter locationsAdapter;

    private final VoiceCommandCenter.Actions voiceActions = new VoiceCommandCenter.Actions() {
        @Override public void onNext() {
            if (nextButton != null && nextButton.isEnabled()) nextButton.performClick();
        }

        @Override public void onBack() {
            goBackToTransactionList();
        }

        @Override public void onScrollUp() {
            if (nextButton != null && nextButton.isFocused()) return;
            startHighlightScrollUp();
        }

        @Override public void onScrollDown() {
            if (nextButton != null && nextButton.isFocused()) return;
            startHighlightScrollDown();
        }

        @Override public void onSelect() {
            handleSelect();
        }

        @Override public void onUp() {
            if (nextButton != null && nextButton.isFocused()) {
                nextButton.clearFocus();
                if (tableLayout != null) tableLayout.requestFocus();
                return;
            }
            if (locationsAdapter != null) locationsAdapter.navigateUp();
        }

        @Override public void onDown() {
            if (nextButton != null && nextButton.isFocused()) return;
            if (locationsAdapter != null) locationsAdapter.navigateDown();
        }

        @Override public void onGoUp() {
            if (nextButton != null && nextButton.isFocused()) return;
            if (locationsAdapter != null) locationsAdapter.navigateUp();
        }

        @Override public void onGoDown() {
            if (nextButton != null && nextButton.isFocused()) return;
            if (locationsAdapter != null) locationsAdapter.navigateDown();
        }

        @Override public void onStop() {
            stopHighlightScroll();
        }

        @Override public void onLogout() {
            LogoutManager.performLogout(LocationDetailsActivity.this);
        }

        @Override public void onInbound() {}
        @Override public void onOutbound() {}
        @Override public void onInventory() {}
        @Override public void onIndividual() {}
        @Override public void onConsolidated() {}
    };

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

        Intent intent = getIntent();
        pickUser = intent.getStringExtra("PICK_USER");
        consolidatedTransactionId = intent.getStringExtra("TRANS_BATCH_ID");
        companyCode = intent.getStringExtra("COMPANY_CODE");
        prinCode = intent.getStringExtra("PRIN_CODE");

        loginId = intent.getStringExtra("LOGIN_ID");
        if (loginId == null || loginId.trim().isEmpty()) {
            loginId = n(pickUser);
        }

        selectOnly = intent.getBooleanExtra("SELECT_ONLY", false);
        preselectLocationCode = n(intent.getStringExtra("CURRENT_LOCATION_CODE"));

        backButton = findViewById(R.id.btnBack);
        nextButton = findViewById(R.id.btnNext);

        backButton.setOnClickListener(v -> goBackToTransactionList());

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> LogoutManager.performLogout(LocationDetailsActivity.this));

        locationsAdapter = new LocationsAdapter(
                this,
                tableScrollView,
                tableLayout,
                tvPage,
                tvNoLocations,
                arrowUp,
                arrowDown,
                rowsPerPage,
                absoluteIndex -> {
                    if (nextButton != null && nextButton.isEnabled()) {
                        nextButton.requestFocus();
                    }
                }
        );

        arrowUp.setOnClickListener(v -> locationsAdapter.navigateUp());
        arrowDown.setOnClickListener(v -> locationsAdapter.navigateDown());

        nextButton.setOnClickListener(v -> onNextPressed());

        initializeScrollRunnables();

        if (consolidatedTransactionId != null && companyCode != null && prinCode != null && pickUser != null) {
            fetchLocationDetails(companyCode, prinCode, consolidatedTransactionId, pickUser);
        } else {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show();
        }
    }

    private void onNextPressed() {
        Location selectedLocation = locationsAdapter.getSelectedLocation();
        int selectedIndex = locationsAdapter.getSelectedIndex();

        if (selectedLocation == null || selectedIndex < 0) {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalQuantitySum = locationsAdapter.calculateTotalQuantitySum();

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
            result.putExtra("SELECTED_INDEX", selectedIndex);

            result.putExtra("CURRENT_LOCATION_INDEX", selectedIndex);
            result.putExtra("TOTAL_ITEMS", locationsAdapter.getItemCount());

            setResult(RESULT_OK, result);
            finish();
        } else {
            Intent next = new Intent(LocationDetailsActivity.this, ConsolidatedJobActivity.class);
            next.putExtra("SITE_CODE", selectedLocation.getSiteCode());
            next.putExtra("LOCATION_CODE", selectedLocation.getLocationCode());
            next.putExtra("QUANTITY", selectedLocation.getQuantity());
            next.putExtra("TRANS_BATCH_ID", selectedLocation.getTransBatchId());
            next.putExtra("PICK_USER", pickUser);
            next.putExtra("COMPANY_CODE", selectedLocation.getCompanyCode());
            next.putExtra("PRIN_CODE", selectedLocation.getPrinCode());
            next.putExtra("JOB_NO", selectedLocation.getJobNo());
            next.putExtra("PROD_CODE", selectedLocation.getProdCode());
            next.putExtra("PICK_QTY", selectedLocation.getPickQty());
            next.putExtra("ORDER_NO", selectedLocation.getOrderNo());

            next.putExtra("TOTAL_ITEMS", locationsAdapter.getItemCount());
            next.putExtra("FRESH_START", true);
            next.putExtra("TOTAL_QUANTITY_SUM", totalQuantitySum);
            next.putExtra("CURRENT_LOCATION_INDEX", selectedIndex);

            startActivity(next);
        }
    }

    private void initializeScrollRunnables() {
        scrollDownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScrollingDown) return;

                if (locationsAdapter == null || !locationsAdapter.canNavigateDown()) {
                    isScrollingDown = false;
                    return;
                }

                locationsAdapter.navigateDown();
                scrollHandler.postDelayed(this, HIGHLIGHT_SCROLL_DELAY_MS);
            }
        };

        scrollUpRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScrollingUp) return;

                if (locationsAdapter == null || !locationsAdapter.canNavigateUp()) {
                    isScrollingUp = false;
                    return;
                }

                locationsAdapter.navigateUp();
                scrollHandler.postDelayed(this, HIGHLIGHT_SCROLL_DELAY_MS);
            }
        };
    }

    private void fetchLocationDetails(String companyCode, String prinCode, String transactionId, String pickUser) {
        new Thread(() -> {
            try {
                List<Location> allLocations = repository.getConsolidatedPicklistBlocking(
                        companyCode, prinCode, transactionId, pickUser
                );

                if (allLocations != null) {
                    allLocations.removeIf(loc -> loc == null || !transactionId.equals(loc.getTransBatchId()));
                } else {
                    allLocations = Collections.emptyList();
                }

                final int explicitIndex = getIntent().getIntExtra("SELECTED_INDEX", -1);
                final List<Location> finalAllLocations = allLocations;

                runOnUiThread(() -> {
                    if (locationsAdapter != null) {
                        locationsAdapter.submitLocations(finalAllLocations, explicitIndex, preselectLocationCode);
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
        if (VoiceCommandCenter.handleKeyDown(keyCode, voiceActions)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            View focusView = getCurrentFocus();

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goBackToTransactionList();
                return true;
            }

            if (nextButton != null && focusView == nextButton) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (backButton != null) backButton.requestFocus();
                        else nextButton.clearFocus();
                        return true;

                    case KeyEvent.KEYCODE_DPAD_UP:
                        nextButton.clearFocus();
                        if (tableLayout != null) tableLayout.requestFocus();
                        return true;

                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        handleSelect();
                        return true;

                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        return true;
                }
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && (focusView == arrowUp || focusView == arrowDown)) {
                if (tableLayout != null) tableLayout.requestFocus();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (backButton != null && focusView != backButton) {
                    backButton.requestFocus();
                    return true;
                }
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                    locationsAdapter != null &&
                    locationsAdapter.isViewInTable(focusView)) {
                if (arrowUp != null) arrowUp.requestFocus();
                return true;
            }

            if ((focusView == arrowUp || focusView == arrowDown) &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (locationsAdapter != null) {
                    if (focusView == arrowUp) locationsAdapter.navigateUp();
                    else locationsAdapter.navigateDown();
                }
                return true;
            }

            if (arrowUp != null && focusView == arrowUp) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (arrowDown != null) arrowDown.requestFocus();
                    return true;
                }
            }

            if (arrowDown != null && focusView == arrowDown) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (arrowUp != null) arrowUp.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (nextButton != null) nextButton.requestFocus();
                    return true;
                }
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (locationsAdapter != null) locationsAdapter.navigateUp();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (locationsAdapter != null) locationsAdapter.navigateDown();
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

    private static String n(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onBackPressed() {
        goBackToTransactionList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceCommandCenter.init(this);

        VoiceCommandCenter.initConsolidatedTransaction(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopHighlightScroll();
        scrollHandler.removeCallbacksAndMessages(null);
    }
}