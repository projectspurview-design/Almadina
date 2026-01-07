package com.example.Pickbyvision.Induvidual_Pick;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.Pickbyvision.Induvidual_Pick.data.UserSessionManager;
import com.example.Pickbyvision.Induvidual_Pick.network.BarcodeLoginApi;
import com.example.Pickbyvision.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Barcodescanner extends AppCompatActivity {

    private static final String TAG = "Barcodescanner";

    private PreviewView previewView;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ActivityResultLauncher<String> permissionLauncher;

    private boolean isScanning = true;
    private boolean isToastShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcodescanner);

        previewView = findViewById(R.id.previewView);
        Button scanQrButton = findViewById(R.id.scanQrButton);

        barcodeScanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
        );

        cameraExecutor = Executors.newSingleThreadExecutor();

        permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) startCamera();
                    else openPermissionSettings();
                });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }

        scanQrButton.setOnClickListener(v -> {
            isScanning = true;
            isToastShown = false;
            startCamera();
        });
    }

    private void openPermissionSettings() {
        Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
        );
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String qr = barcode.getRawValue();
                        if (qr != null && !qr.trim().isEmpty()) {
                            isScanning = false;
                            cameraProvider.unbindAll();
                            validateWithBackend(qr.trim());
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> showOnce("QR scan failed"))
                .addOnCompleteListener(t -> imageProxy.close());
    }

    private void showOnce(String msg) {
        if (!isToastShown) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            isToastShown = true;
        }
    }

    private void validateWithBackend(String qrData) {

        String[] creds = parseQRData(qrData);
        String loginId = creds[0];
        String password = creds[1];

        new BarcodeLoginApi().login(loginId, password,
                new BarcodeLoginApi.LoginCallback() {

                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {

                            if ("Login Success".equalsIgnoreCase(response)) {

                                UserSessionManager.saveUser(
                                        Barcodescanner.this,
                                        loginId,
                                        loginId
                                );
                                goToWelcome(loginId);

                            } else {
                                Toast.makeText(
                                        Barcodescanner.this,
                                        response.isEmpty()
                                                ? "Invalid login"
                                                : response,
                                        Toast.LENGTH_LONG
                                ).show();

                                isScanning = true;
                                startCamera();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    Barcodescanner.this,
                                    "Login failed: " + error,
                                    Toast.LENGTH_LONG
                            ).show();
                            isScanning = true;
                            startCamera();
                        });
                    }
                });
    }


    private String[] parseQRData(String qr) {
        if (qr.contains(":")) return qr.split(":", 2);
        if (qr.contains(",")) return qr.split(",", 2);
        if (qr.contains("|")) return qr.split("\\|", 2);
        if (qr.contains(";")) return qr.split(";", 2);

        try {
            JSONObject obj = new JSONObject(qr);
            return new String[]{
                    obj.optString("login_id", qr),
                    obj.optString("password", "DEFAULT")
            };
        } catch (JSONException e) {
            return new String[]{qr, "DEFAULT"};
        }
    }

    private void goToWelcome(String userName) {
        Intent intent = new Intent(this, Welcomeactivity.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) barcodeScanner.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
