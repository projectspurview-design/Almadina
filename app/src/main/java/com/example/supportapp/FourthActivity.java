package com.example.supportapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FourthActivity extends AppCompatActivity {
    private static final String EXPECTED_FIREBASE_URL_BASE = "https://webrtcjava-c7aa0-default-rtdb.firebaseio.com";
    private static final String TAG = "FourthActivity";

    private PreviewView previewView;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ProcessCameraProvider cameraProvider;
    private boolean isToastShown = false;
    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_fourth);
        previewView = findViewById(R.id.previewView);
        Button scanQrButton = findViewById(R.id.scanQrButton);

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startCamera();
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Camera permission denied permanently. Please enable it in Settings.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show();
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        scanQrButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                isToastShown = false;
                isScanning = true;
                startCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        try {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!isScanning) return;

                        if (barcodes.isEmpty()) {
                            if (!isToastShown) {
                                Toast.makeText(this, "No QR code detected", Toast.LENGTH_SHORT).show();
                                isToastShown = true;
                            }
                        } else {
                            for (Barcode barcode : barcodes) {
                                String scannedUrl = barcode.getRawValue();
                                Log.d(TAG, "Scanned QR code content: " + scannedUrl);

                                if (scannedUrl != null && scannedUrl.startsWith(EXPECTED_FIREBASE_URL_BASE)) {
                                    isScanning = false;
                                    cameraProvider.unbindAll();

                                    Uri uri = Uri.parse(scannedUrl);
                                    String userId = uri.getQueryParameter("userid");

                                    if (userId != null && !userId.isEmpty()) {
                                        userId = userId.trim();  // <-- Trim whitespace to clean up userId
                                        validateFirebaseDataForUser(userId);
                                    } else {
                                        validateFirebaseDataFirstUser();
                                    }

                                    break;
                                } else {
                                    if (!isToastShown) {
                                        Toast.makeText(this, "Invalid QR code: " + scannedUrl, Toast.LENGTH_SHORT).show();
                                        isToastShown = true;
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isToastShown) {
                            Toast.makeText(this, "Error scanning QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            isToastShown = true;
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            if (!isToastShown) {
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isToastShown = true;
            }
            imageProxy.close();
        }
    }

    private void validateFirebaseDataForUser(String userId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = snapshot.child("name").getValue(String.class);
                    if (userName == null) userName = "User";

                    goToFifthActivity(userName);
                } else {
                    Toast.makeText(FourthActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    isScanning = true;
                    startCamera();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FourthActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                isScanning = true;
                startCamera();
            }
        });
    }

    private void validateFirebaseDataFirstUser() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("users");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = "User";
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        if (userSnapshot.hasChild("name")) {
                            userName = userSnapshot.child("name").getValue(String.class);
                            break;
                        }
                    }
                    goToFifthActivity(userName);
                } else {
                    Toast.makeText(FourthActivity.this, "Invalid data structure", Toast.LENGTH_SHORT).show();
                    isScanning = true;
                    startCamera();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FourthActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                isScanning = true;
                startCamera();
            }
        });
    }

    private void goToFifthActivity(String userName) {
        Intent intent = new Intent(FourthActivity.this, FifthActivity.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeScanner.close();
        cameraExecutor.shutdown();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
