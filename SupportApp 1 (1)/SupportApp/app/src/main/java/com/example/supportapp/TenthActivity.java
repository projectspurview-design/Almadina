package com.example.supportapp; // Ensure this matches your package name

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color; // Import for color
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.NumberPicker; // Import NumberPicker
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TenthActivity extends AppCompatActivity {

    private static final String TAG = "TenthActivity";
    private static final int MENU_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_LOCATION_REQUEST_CODE = 1001;
    // You can adjust this duration if needed, or make it shorter for the SKU message
    private static final long SKU_MESSAGE_DISPLAY_DURATION_MS = 2000; // Duration to show "RIGHT/WRONG SKU"

    private TextView pausedStatusText;
    private TextView skuStatusText;
    private Button btnScanBarcodeInternal;
    private ImageView ivScanIcon;
    private Button btnJumpTo;
    private Button btnShort;

    // For CameraX and ML Kit
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider mCameraProvider;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private Handler uiHandler;

    // View references for managing visibility
    private View orderInfoContainer;
    private View scanBarcodeContainer;
    private View menuLayout;
    private TextView locationText;

    // View references for Short Quantity Input
    private LinearLayout shortQuantityInputContainer;
    private NumberPicker npThousandsInline, npHundredsInline, npTensInline, npUnitsInline;
    private Button btnConfirmShortInput, btnCancelShortInput;

    // Define the expected SKU
    // In a real app, this would likely come from order data or a ViewModel
    private String expectedSku = "12345ABC"; // EXAMPLE: Replace with your actual expected SKU logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenth);

        uiHandler = new Handler(Looper.getMainLooper());

        pausedStatusText = findViewById(R.id.pausedStatusText);
        skuStatusText = findViewById(R.id.skuStatusText);
        if (skuStatusText == null) {
            Log.e(TAG, "FATAL ERROR: skuStatusText is not found in the layout (R.id.skuStatusText).");
            Toast.makeText(this, "Layout error: SKU status view missing.", Toast.LENGTH_LONG).show();
            // Potentially finish the activity if this is critical
            // finish();
            // return;
        }

        locationText = findViewById(R.id.locationText);
        btnScanBarcodeInternal = findViewById(R.id.btnScanBarcode);
        ivScanIcon = findViewById(R.id.ivScanIcon);
        btnJumpTo = findViewById(R.id.btnJumpTo);
        btnShort = findViewById(R.id.btnShort);
        previewView = findViewById(R.id.camera_preview);

        orderInfoContainer = findViewById(R.id.orderInfoContainer);
        scanBarcodeContainer = findViewById(R.id.scanBarcodeContainer);
        menuLayout = findViewById(R.id.menuLayout);

        shortQuantityInputContainer = findViewById(R.id.shortQuantityInputContainer);
        npThousandsInline = findViewById(R.id.npThousandsInline);
        npHundredsInline = findViewById(R.id.npHundredsInline);
        npTensInline = findViewById(R.id.npTensInline);
        npUnitsInline = findViewById(R.id.npUnitsInline);
        btnConfirmShortInput = findViewById(R.id.btnConfirmShortInput);
        btnCancelShortInput = findViewById(R.id.btnCancelShortInput);

        setupNumberPickers();

        if (scanBarcodeContainer != null) {
            scanBarcodeContainer.setOnClickListener(v -> handleScanButtonClick());
        } else {
            Log.w(TAG, "scanBarcodeContainer not found, setting click listener on btnScanBarcodeInternal directly.");
            if (btnScanBarcodeInternal != null) {
                btnScanBarcodeInternal.setOnClickListener(v -> handleScanButtonClick());
            }
        }

        if (btnJumpTo != null) {
            btnJumpTo.setOnClickListener(v -> {
                Log.d(TAG, "Jump To button clicked, launching PickLocationActivity.");
                Intent intent = new Intent(TenthActivity.this, PickLocationActivity.class);
                startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
            });
        }

        if (btnShort != null) {
            btnShort.setOnClickListener(v -> {
                Log.d(TAG, "Short button clicked.");
                showShortQuantityInput();
            });
        }

        if (btnConfirmShortInput != null) {
            btnConfirmShortInput.setOnClickListener(v -> handleConfirmShortQuantity());
        }

        if (btnCancelShortInput != null) {
            btnCancelShortInput.setOnClickListener(v -> handleCancelShortQuantity());
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS) // Consider restricting formats if known
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        showInitialScreenState();
    }

    private void setupNumberPickers() {
        NumberPicker[] pickers = {npThousandsInline, npHundredsInline, npTensInline, npUnitsInline};
        for (NumberPicker picker : pickers) {
            if (picker != null) {
                picker.setMinValue(0);
                picker.setMaxValue(9);
                picker.setWrapSelectorWheel(false);
            }
        }
    }

    private void showShortQuantityInput() {
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (scanBarcodeContainer != null) scanBarcodeContainer.setVisibility(View.GONE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (previewView != null) previewView.setVisibility(View.GONE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);


        if (shortQuantityInputContainer != null) {
            shortQuantityInputContainer.setVisibility(View.VISIBLE);
        }

        if (npThousandsInline != null) npThousandsInline.setValue(0);
        if (npHundredsInline != null) npHundredsInline.setValue(0);
        if (npTensInline != null) npTensInline.setValue(0);
        if (npUnitsInline != null) npUnitsInline.setValue(0);

        if (npUnitsInline != null && npUnitsInline.getVisibility() == View.VISIBLE) {
            npUnitsInline.requestFocus();
        } else if (npThousandsInline != null && npThousandsInline.getVisibility() == View.VISIBLE) {
            npThousandsInline.requestFocus();
        } else if (btnConfirmShortInput != null && btnConfirmShortInput.getVisibility() == View.VISIBLE) {
            btnConfirmShortInput.requestFocus();
        }
        Log.d(TAG, "UI set to: Short Quantity Input Screen State");
    }

    private void hideShortQuantityInputAndShowMenu() {
        if (shortQuantityInputContainer != null) {
            shortQuantityInputContainer.setVisibility(View.GONE);
        }
        showInitialScreenState();
        Log.d(TAG, "UI returned to: Initial Screen State from Short Quantity Input");
    }

    private void handleConfirmShortQuantity() {
        int thousands = (npThousandsInline != null) ? npThousandsInline.getValue() : 0;
        int hundreds = (npHundredsInline != null) ? npHundredsInline.getValue() : 0;
        int tens = (npTensInline != null) ? npTensInline.getValue() : 0;
        int units = (npUnitsInline != null) ? npUnitsInline.getValue() : 0;

        int enteredQuantity = (thousands * 1000) + (hundreds * 100) + (tens * 10) + units;

        if (enteredQuantity <= 0) {
            Toast.makeText(this, "Please enter a quantity greater than 0.", Toast.LENGTH_SHORT).show();
            if (npUnitsInline != null && npUnitsInline.getVisibility() == View.VISIBLE) {
                npUnitsInline.requestFocus();
            } else if (btnConfirmShortInput != null && btnConfirmShortInput.getVisibility() == View.VISIBLE) {
                btnConfirmShortInput.requestFocus();
            }
            return;
        }

        String message = "Confirmed Short Quantity: " + enteredQuantity;
        Log.d(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        hideShortQuantityInputAndShowMenu();
    }

    private void handleCancelShortQuantity() {
        Log.d(TAG, "Short quantity input cancelled.");
        hideShortQuantityInputAndShowMenu();
    }

    private void handleScanButtonClick() {
        if (btnScanBarcodeInternal == null) {
            Log.e(TAG, "btnScanBarcodeInternal is null in handleScanButtonClick");
            return;
        }
        String currentButtonText = btnScanBarcodeInternal.getText().toString();

        if (currentButtonText.equalsIgnoreCase(getString(R.string.resume_text))) {
            showInitialScreenState(); // Resume scanning or go to initial state
            Toast.makeText(this, "Resumed.", Toast.LENGTH_SHORT).show();
        } else if (currentButtonText.equalsIgnoreCase(getString(R.string.scan_barcode_text))) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (!isScanning.get()) {
                    startCameraAndScanner();
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void startCameraAndScanner() {
        if (!isScanning.compareAndSet(false, true)) {
            Log.d(TAG, "Already attempting to start or is scanning.");
            return;
        }
        Log.d(TAG, "Starting camera and scanner.");
        runOnUiThread(this::showScanningScreenState);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();
                if (mCameraProvider == null) {
                    Log.e(TAG, "Camera provider became null after future completed.");
                    stopCameraAndScanner(true); // true for error
                    return;
                }
                bindPreviewAndAnalysis(mCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera provider", e);
                Thread.currentThread().interrupt();
                stopCameraAndScanner(true); // true for error
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        if (previewView == null) {
            Log.e(TAG, "PreviewView is null in bindPreviewAndAnalysis. Cannot proceed.");
            stopCameraAndScanner(true); // true for error
            return;
        }

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (!isScanning.get()) { // Check if scanning is still active
                image.close();
                return;
            }
            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = image.getImage();

            if (mediaImage != null) {
                InputImage inputImage =
                        InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                barcodeScanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            // Check scanning flag again inside listener, as it might have changed
                            if (!isScanning.get() || barcodes.isEmpty()) {
                                if (!isScanning.get()){
                                    Log.d(TAG, "Scanning stopped before barcode processing completed or no barcode found.");
                                }
                                return; // Important: return if no barcodes or scanning stopped
                            }

                            // Stop scanning once a barcode is successfully processed
                            if (isScanning.compareAndSet(true, false)) {
                                if (mCameraProvider != null) {
                                    mCameraProvider.unbindAll(); // Unbind to stop camera feed
                                }

                                String rawValue = barcodes.get(0).getRawValue();
                                Log.d(TAG, "Barcode detected: " + rawValue);

                                // Compare with expectedSku
                                boolean isRightSKU = expectedSku.equals(rawValue);
                                runOnUiThread(() -> showScanResultScreenState(isRightSKU)); // Pass only isRightSKU
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isScanning.get()) { // Only log if still intended to scan
                                Log.e(TAG, "Barcode scanning failed", e);
                                // Optionally, handle UI feedback for failure here if needed
                                // stopCameraAndScanner(true); // Consider if you want to stop on every failure
                            }
                        })
                        .addOnCompleteListener(task -> image.close()); // Always close the image
            } else {
                image.close(); // Ensure image is closed if mediaImage is null
            }
        });

        try {
            cameraProvider.unbindAll(); // Unbind existing use cases before binding new ones
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "Camera use cases bound.");
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            stopCameraAndScanner(true); // true for error
        }
    }

    private void stopCameraAndScanner(boolean dueToError) {
        Log.d(TAG, "Stopping camera and scanner. Due to error: " + dueToError);
        isScanning.set(false); // Ensure scanning state is updated

        if (mCameraProvider != null) {
            mCameraProvider.unbindAll();
            Log.d(TAG, "Camera provider unbound.");
        }

        // Handle UI state after stopping
        runOnUiThread(() -> {
            boolean isShowingSkuStatus = (skuStatusText != null && skuStatusText.getVisibility() == View.VISIBLE);
            boolean isPreviewVisible = (previewView != null && previewView.getVisibility() == View.VISIBLE);
            boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);

            // If preview was visible and we're not already showing SKU status or short quantity input, go to initial
            if (isPreviewVisible && !isShowingSkuStatus && !isShortQuantityInputVisible) {
                showInitialScreenState();
            } else if (isPreviewVisible && isShowingSkuStatus) {
                // If SKU status is visible, the handler in showScanResultScreenState will manage timeout
                Log.d(TAG, "Scanner stopped, but SKU status is visible. Its handler will take over.");
            } else if (isShortQuantityInputVisible) {
                Log.d(TAG, "Scanner stopped while short quantity input was visible.");
                if (dueToError) {
                    Toast.makeText(this, "Scanner error. Please check.", Toast.LENGTH_SHORT).show();
                    // Keep focus on short quantity input
                }
            }

            if (dueToError && !isShortQuantityInputVisible && !isShowingSkuStatus) { // Don't show toast if already showing result or input
                Toast.makeText(this, "Scanner error. Please try again.", Toast.LENGTH_SHORT).show();
                if (!isPreviewVisible) showInitialScreenState(); // Only go to initial if preview isn't already there
            }
        });
    }

    private void showInitialScreenState() {
        if (previewView != null) previewView.setVisibility(View.GONE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.VISIBLE);
        if (scanBarcodeContainer != null) scanBarcodeContainer.setVisibility(View.VISIBLE);
        if (btnScanBarcodeInternal != null) btnScanBarcodeInternal.setText(getString(R.string.scan_barcode_text));
        if (ivScanIcon != null) ivScanIcon.setVisibility(View.VISIBLE);
        if (menuLayout != null) menuLayout.setVisibility(View.VISIBLE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        Log.d(TAG, "UI set to: Initial Screen State");

        View viewToFocus = null;
        if (btnScanBarcodeInternal != null && btnScanBarcodeInternal.getVisibility() == View.VISIBLE) {
            viewToFocus = btnScanBarcodeInternal;
        } else if (scanBarcodeContainer != null && scanBarcodeContainer.getVisibility() == View.VISIBLE) {
            viewToFocus = scanBarcodeContainer;
        }
        if (viewToFocus != null) {
            viewToFocus.requestFocus();
        }
    }

    private void showScanningScreenState() {
        if (previewView != null) previewView.setVisibility(View.VISIBLE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (scanBarcodeContainer != null) scanBarcodeContainer.setVisibility(View.GONE);
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
        Log.d(TAG, "UI set to: Scanning Screen State");
        if (previewView != null) previewView.requestFocus(); // Focus camera preview when scanning
    }


    // MODIFIED: showScanResultScreenState
    private void showScanResultScreenState(boolean isRightSKU) {
        // Hide camera preview and other interactive elements immediately
        if (previewView != null) previewView.setVisibility(View.GONE);
        if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
        if (scanBarcodeContainer != null) scanBarcodeContainer.setVisibility(View.GONE);
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);
        if (pausedStatusText != null) pausedStatusText.setVisibility(View.GONE);
        if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);

        if (skuStatusText != null) {
            skuStatusText.setVisibility(View.VISIBLE);
            if (isRightSKU) {
                skuStatusText.setText("RIGHT SKU");
                skuStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                skuStatusText.setText("WRONG SKU");
                skuStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
            Log.d(TAG, "UI set to: Scan Result Screen State (" + (isRightSKU ? "Right" : "Wrong") + " SKU)");
        } else {
            // Fallback if skuStatusText is somehow null (shouldn't happen with the check in onCreate)
            Log.e(TAG, "skuStatusText is null, cannot display scan result message via TextView.");
            Toast.makeText(this, (isRightSKU ? "RIGHT SKU" : "WRONG SKU"), Toast.LENGTH_LONG).show();
        }

        // Clear any previous callbacks from the handler
        uiHandler.removeCallbacksAndMessages(null);

        // Handle next action based on SKU result
        if (isRightSKU) {
            uiHandler.postDelayed(() -> {
                Intent intent = new Intent(TenthActivity.this, ConfirmationPageActivity.class);
                startActivity(intent);
                finish(); // Finish TenthActivity so user can't go back
            }, SKU_MESSAGE_DISPLAY_DURATION_MS);
        } else {
            // For WRONG SKU, revert to initial screen after a delay to show the message
            uiHandler.postDelayed(this::showInitialScreenState, SKU_MESSAGE_DISPLAY_DURATION_MS);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!isScanning.get()) { // Check if not already trying to scan
                    Log.d(TAG, "Permission granted, starting camera.");
                    startCameraAndScanner();
                }
            } else {
                Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_SHORT).show();
                showInitialScreenState(); // Revert to initial state if permission denied
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MENU_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                boolean paused = data.getBooleanExtra("PAUSED", false);
                updateUIForPauseState(paused);
            }
        } else if (requestCode == PICK_LOCATION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // Use the correct constant names from PickLocationActivity
                String selectedLocation = data.getStringExtra(PickLocationActivity.EXTRA_SELECTED_LOCATION);
                String selectedSite = data.getStringExtra(PickLocationActivity.EXTRA_SELECTED_SITE);
                String selectedCode = data.getStringExtra(PickLocationActivity.EXTRA_SELECTED_CODE);
                int selectedQuantity = data.getIntExtra(PickLocationActivity.EXTRA_SELECTED_QUANTITY, 0);

                if (selectedLocation != null && selectedSite != null && selectedCode != null) {
                    String message = "Selected Location: " + selectedLocation +
                            "\nSite: " + selectedSite +
                            "\nCode: " + selectedCode +
                            "\nQuantity: " + selectedQuantity;
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, message);

                    // Update the location text with the selected location name
                    if (locationText != null) {
                        locationText.setText(selectedLocation + " (" + selectedSite + ")");
                    }
                } else {
                    Log.w(TAG, "Location data missing from PickLocationActivity result.");
                    Toast.makeText(this, "Failed to get location details.", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Location selection cancelled.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Location selection cancelled or no location selected.");
            }
        }
    }

    private void updateUIForPauseState(boolean isPaused) {
        if (isPaused) {
            if (isScanning.compareAndSet(true, false)) { // If scanning, stop it
                Log.d(TAG, "External pause requested during scan. Stopping camera.");
                if (mCameraProvider != null) mCameraProvider.unbindAll();
            }
            // Set UI to paused state
            if (previewView != null) previewView.setVisibility(View.GONE);
            if (orderInfoContainer != null) orderInfoContainer.setVisibility(View.GONE);
            if (scanBarcodeContainer != null) scanBarcodeContainer.setVisibility(View.VISIBLE);
            if (btnScanBarcodeInternal != null) btnScanBarcodeInternal.setText(getString(R.string.resume_text));
            if (ivScanIcon != null) ivScanIcon.setVisibility(View.GONE); // Hide scan icon for resume button
            if (menuLayout != null) menuLayout.setVisibility(View.GONE);
            if (skuStatusText != null) skuStatusText.setVisibility(View.GONE);
            if (shortQuantityInputContainer != null) shortQuantityInputContainer.setVisibility(View.GONE);
            if (pausedStatusText != null) pausedStatusText.setVisibility(View.VISIBLE);
            Log.d(TAG, "UI set to: Paused State");
            if (btnScanBarcodeInternal != null && btnScanBarcodeInternal.getVisibility() == View.VISIBLE) {
                btnScanBarcodeInternal.requestFocus();
            }
        } else {
            // If resuming from pause, go to initial state (which allows scanning)
            showInitialScreenState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);

        // Only stop camera if actively scanning, not changing config, and not in short quantity input mode
        if (isScanning.get() && !isChangingConfigurations() && !isShortQuantityInputVisible) {
            Log.d(TAG, "onPause: Actively scanning (and not in short input), stopping camera.");
            stopCameraAndScanner(false); // false for not an error
        } else if (isScanning.get() && isShortQuantityInputVisible) {
            Log.d(TAG, "onPause: Actively scanning BUT short quantity input is visible. Camera NOT stopped by onPause.");
        }
        // Always remove handler callbacks on pause to prevent unexpected UI changes
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Determine current UI state to decide on resume behavior
        boolean isShortQuantityInputVisible = (shortQuantityInputContainer != null && shortQuantityInputContainer.getVisibility() == View.VISIBLE);
        boolean isShowingSkuStatus = (skuStatusText != null && skuStatusText.getVisibility() == View.VISIBLE);
        boolean isInPausedState = (pausedStatusText != null && pausedStatusText.getVisibility() == View.VISIBLE);

        Log.d(TAG, "onResume: ShortInputVisible=" + isShortQuantityInputVisible +
                ", Scanning=" + isScanning.get() + // isScanning reflects intent, not necessarily active camera
                ", SkuStatusVisible=" + isShowingSkuStatus +
                ", PausedState=" + isInPausedState);

        if (isShortQuantityInputVisible) {
            Log.d(TAG, "onResume: Resuming to Short Quantity Input state. Ensuring focus.");
            // Ensure focus is correctly set within the short quantity input
            View focusedChild = shortQuantityInputContainer.findFocus();
            if (focusedChild == null) { // If no child has focus, set it
                if (npUnitsInline != null && npUnitsInline.getVisibility() == View.VISIBLE) {
                    npUnitsInline.requestFocus();
                } else if (npThousandsInline != null && npThousandsInline.getVisibility() == View.VISIBLE) {
                    npThousandsInline.requestFocus();
                } else if (btnConfirmShortInput != null && btnConfirmShortInput.getVisibility() == View.VISIBLE) {
                    btnConfirmShortInput.requestFocus();
                }
            }
        } else if (isScanning.get() && !isShowingSkuStatus && !isInPausedState &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // If we were in the process of scanning (and not interrupted by SKU display or pause)
            // and have permission, restart the camera. This handles cases where onPause stopped it.
            Log.d(TAG, "onResume: Was scanning and not showing status/paused. Restarting camera.");
            startCameraAndScanner(); // This will also call showScanningScreenState
        } else if (!isShowingSkuStatus && !isInPausedState) {
            // Default to initial screen state if not in short input, not showing SKU, and not paused.
            // This covers the case after returning from ConfirmationPageActivity (if not finished)
            // or if the app was simply backgrounded from the initial state.
            Log.d(TAG, "onResume: Not in specific state. Ensuring initial screen state.");
            showInitialScreenState();
        } else if (isShowingSkuStatus) {
            // If SKU status is visible, its handler is responsible for the timeout.
            // Re-posting might be needed if the activity was fully stopped and handler lost.
            // However, with finish() on RIGHT_SKU, this path is less likely for that case.
            // For WRONG_SKU, the handler should still be active or needs re-triggering if necessary.
            // For simplicity, we assume the handler post in showScanResultScreenState is sufficient
            // if the activity is just paused and resumed.
            Log.d(TAG, "onResume: Resuming while SKU status is visible. Its handler should manage timeout.");
        } else { // isInPausedState must be true
            Log.d(TAG, "onResume: Resuming to Paused state. UI should already be set by updateUIForPauseState.");
            if (btnScanBarcodeInternal != null && btnScanBarcodeInternal.getVisibility() == View.VISIBLE &&
                    btnScanBarcodeInternal.getText().toString().equalsIgnoreCase(getString(R.string.resume_text))) {
                btnScanBarcodeInternal.requestFocus();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null); // Clean up handler
        }
        // mCameraProvider will be handled by lifecycle if bound
        Log.d(TAG, "onDestroy called.");
    }
}