package com.example.supportapp.Induvidual_Pick;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.supportapp.R;
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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;

public class Barcodescanner extends AppCompatActivity {
    private static final String API_BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/PICK_/CHECK_LOGIN";
    private static final String API_KEY = "bkV7TzFDJx4m55fY~5Lql2BvsEwlMXr";
    private static final String TAG = "FourthActivity";
    private static final String PREFS_NAME = "AppPrefs"; // Add this constant

    private PreviewView previewView;
    private BarcodeScanner barcodeScanner;

    private ExecutorService cameraExecutor;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ProcessCameraProvider cameraProvider;
    private boolean isToastShown = false;
    private boolean isScanning = true;
    private OkHttpClient httpClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcodescanner);

        // Initialize HTTP client with timeout settings
        initializeHttpClient();

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

    private void initializeHttpClient() {
        try {
            // Create a trust manager that accepts all certificates
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                            // Accept all certificates
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                            // Accept all certificates
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true; // Accept all hostnames
                        }
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Log.d(TAG, "HTTP client initialized with SSL bypass");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up HTTP client with SSL bypass: " + e.getMessage());
            // Fallback to default client (this will still have SSL issues)
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
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
                                String scannedData = barcode.getRawValue();
                                Log.d(TAG, "Scanned QR code content: " + scannedData);

                                if (scannedData != null && !scannedData.trim().isEmpty()) {
                                    isScanning = false;
                                    cameraProvider.unbindAll();

                                    // Validate the scanned data with REST API
                                    validateWithRestAPI(scannedData.trim());
                                    break;
                                } else {
                                    if (!isToastShown) {
                                        Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
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

    private void validateWithRestAPI(String qrData) {
        try {
            Log.d(TAG, "Starting API validation for QR Data: '" + qrData + "'");
            Log.d(TAG, "QR Data Length: " + qrData.length());

            String[] credentials = parseQRData(qrData);
            String loginId = credentials[0];
            String password = credentials[1];

            Log.d(TAG, "Parsed credentials - Login ID: '" + loginId + "', Password: '" + password + "'");

            // Make the API call (only one method now)
            makeGETAPICall(loginId, password);

        } catch (Exception e) {
            Log.e(TAG, "Error in validateWithRestAPI: " + e.getMessage());
            Toast.makeText(this, "Error parsing QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isScanning = true;
            startCamera();
        }
    }

    private void makeGETAPICall(String loginId, String password) {
        try {
            // Build URL exactly as shown in the cURL command
            String urlWithParams = API_BASE_URL +
                    "?as_login_id=" + loginId +
                    "&as_log_pass=" + password;

            Log.d(TAG, "Making API call to URL: " + urlWithParams);
            Log.d(TAG, "Using API Key: " + API_KEY);

            // Build request with headers matching cURL command exactly
            Request request = new Request.Builder()
                    .url(urlWithParams)
                    .get()
                    .addHeader("Accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("User-Agent", "SupportApp/1.0")
                    .build();

            Log.d(TAG, "Request headers: " + request.headers().toString());

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed with error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(Barcodescanner.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        isScanning = true;
                        startCamera();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    Log.d(TAG, "Response received:");
                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Response Message: " + response.message());
                    Log.d(TAG, "Response Body: " + responseBody);
                    Log.d(TAG, "Response Headers: " + response.headers().toString());

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "API call successful, processing response...");
                            handleSuccessfulResponse(responseBody, loginId);
                        } else {
                            Log.e(TAG, "API returned error code: " + response.code());
                            String errorMsg = "Server error (Code: " + response.code() + ")";
                            if (!responseBody.isEmpty() && responseBody.length() < 200) {
                                errorMsg = responseBody;
                            }
                            Toast.makeText(Barcodescanner.this, errorMsg, Toast.LENGTH_LONG).show();
                            isScanning = true;
                            startCamera();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating API request: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isScanning = true;
            startCamera();
        }
    }

    // Alternative method if the main one doesn't work
    private void makeGETAPICallAlternative(String loginId, String password) {
        try {
            // Try with XApiKey as URL parameter instead of api_key
            String urlWithParams = API_BASE_URL +
                    "?as_login_id=" + Uri.encode(loginId) +
                    "&as_log_pass=" + Uri.encode(password) +
                    "&XApiKey=" + Uri.encode(API_KEY);

            Request request = new Request.Builder()
                    .url(urlWithParams)
                    .get()
                    .addHeader("Accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .addHeader("User-Agent", "SupportApp/1.0")
                    .build();

            Log.d(TAG, "Making alternative GET API call to: " + urlWithParams);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Alternative API call failed: " + e.getMessage());
                        // Try the third method
                        makeGETAPICallThirdTry(loginId, password);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Alternative Response Code: " + response.code());
                            Log.d(TAG, "Alternative Response Body: " + responseBody);

                            if (response.isSuccessful()) {
                                handleSuccessfulResponse(responseBody, loginId);
                            } else {
                                Log.e(TAG, "Alternative API Error: " + response.code() + " - " + responseBody);
                                // Try the third method
                                makeGETAPICallThirdTry(loginId, password);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing alternative response: " + e.getMessage());
                            makeGETAPICallThirdTry(loginId, password);
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating alternative GET request: " + e.getMessage());
            makeGETAPICallThirdTry(loginId, password);
        }
    }

    // Third try - exactly match the cURL command
    private void makeGETAPICallThirdTry(String loginId, String password) {
        try {
            // Try without api_key in URL, only in header
            String urlWithParams = API_BASE_URL +
                    "?as_login_id=" + Uri.encode(loginId) +
                    "&as_log_pass=" + Uri.encode(password);

            Request request = new Request.Builder()
                    .url(urlWithParams)
                    .get()
                    .addHeader("Accept", "*/*")
                    .addHeader("XApiKey", API_KEY)
                    .build();

            Log.d(TAG, "Making third try GET API call to: " + urlWithParams);
            Log.d(TAG, "Third try headers: " + request.headers().toString());

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Third try API call failed: " + e.getMessage());
                        Toast.makeText(Barcodescanner.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isScanning = true;
                        startCamera();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Third try Response Code: " + response.code());
                            Log.d(TAG, "Third try Response Body: " + responseBody);

                            if (response.isSuccessful()) {
                                handleSuccessfulResponse(responseBody, loginId);
                            } else {
                                Log.e(TAG, "All API methods failed: " + response.code() + " - " + responseBody);
                                String errorMsg = "Authentication failed (Code: " + response.code() + ")";
                                if (!responseBody.isEmpty() && responseBody.length() < 200) {
                                    errorMsg = responseBody;
                                }
                                Toast.makeText(Barcodescanner.this, errorMsg, Toast.LENGTH_LONG).show();
                                isScanning = true;
                                startCamera();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing third try response: " + e.getMessage());
                            Toast.makeText(Barcodescanner.this, "Error processing response", Toast.LENGTH_SHORT).show();
                            isScanning = true;
                            startCamera();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating third try GET request: " + e.getMessage());
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            isScanning = true;
            startCamera();
        }
    }

    private String[] parseQRData(String qrData) {
        // Enhanced parsing with better error handling
        if (qrData.contains(":")) {
            String[] parts = qrData.split(":", 2);
            if (parts.length == 2) {
                return new String[]{parts[0].trim(), parts[1].trim()};
            }
        } else if (qrData.contains(",")) {
            String[] parts = qrData.split(",", 2);
            if (parts.length == 2) {
                return new String[]{parts[0].trim(), parts[1].trim()};
            }
        } else if (qrData.contains("|")) {
            String[] parts = qrData.split("\\|", 2);
            if (parts.length == 2) {
                return new String[]{parts[0].trim(), parts[1].trim()};
            }
        } else if (qrData.contains(";")) {
            String[] parts = qrData.split(";", 2);
            if (parts.length == 2) {
                return new String[]{parts[0].trim(), parts[1].trim()};
            }
        }

        // Try JSON parsing
        try {
            JSONObject jsonData = new JSONObject(qrData);
            String loginId = jsonData.optString("login_id",
                    jsonData.optString("user_id",
                            jsonData.optString("username",
                                    jsonData.optString("id", "USER"))));
            String password = jsonData.optString("password",
                    jsonData.optString("pass",
                            jsonData.optString("pwd",
                                    jsonData.optString("key", "DEFAULT"))));
            return new String[]{loginId, password};
        } catch (JSONException e) {
            Log.w(TAG, "QR data is not JSON format: " + e.getMessage());
        }

        // If all parsing fails, use the entire QR data as login_id with default password
        Log.w(TAG, "Using entire QR data as login ID with default password");
        return new String[]{qrData.trim(), "DEFAULT_PASS"};
    }

    private void handleSuccessfulResponse(String responseBody, String loginId) {
        try {
            String trimmedResponse = responseBody.trim();
            Log.d(TAG, "Processing response: " + trimmedResponse);

            // Check for simple text responses
            if (trimmedResponse.equalsIgnoreCase("Login Success") ||
                    trimmedResponse.toLowerCase().contains("success") ||
                    trimmedResponse.toLowerCase().contains("valid") ||
                    trimmedResponse.toLowerCase().contains("authenticated")) {

                Log.d(TAG, "Login successful for user: " + loginId);
                Toast.makeText(this, "Login successful: " + loginId, Toast.LENGTH_SHORT).show();

                // ENHANCED: Save both userName and userId properly
                saveUserData(loginId, loginId, loginId); // ← fallback if no separate keyNumber
                goToFifthActivity(loginId, loginId);
                return;
            }

            // Try to parse as JSON
            JSONObject jsonResponse = new JSONObject(responseBody);

            boolean success = jsonResponse.optBoolean("success", false);
            String status = jsonResponse.optString("status", "");
            String message = jsonResponse.optString("message", "");

            if (success ||
                    "success".equalsIgnoreCase(status) ||
                    "valid".equalsIgnoreCase(status) ||
                    "ok".equalsIgnoreCase(status) ||
                    "authenticated".equalsIgnoreCase(status)) {

                String userName = extractUserName(jsonResponse, loginId);
                String userId = extractUserId(jsonResponse, loginId);

                // ENHANCED: Ensure we always have valid user data
                if (userName == null || userName.trim().isEmpty()) {
                    userName = loginId; // Fallback to loginId
                }
                if (userId == null || userId.trim().isEmpty()) {
                    userId = loginId; // Fallback to loginId
                }

                Log.d(TAG, "Login successful - UserName: " + userName + ", UserId: " + userId);
                Toast.makeText(this, "Login successful: " + userName, Toast.LENGTH_SHORT).show();

                // Save user data to SharedPreferences
                saveUserData(userName, userId, loginId);  // using loginId as keyNumber fallback
                goToFifthActivity(userName, userId);
            } else {
                String errorMessage = !message.isEmpty() ? message : "Invalid credentials";
                Log.w(TAG, "Login failed: " + errorMessage);
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                isScanning = true;
                startCamera();
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
            Log.d(TAG, "Raw response: " + responseBody);

            // If JSON parsing fails, check for success keywords
            String lowerResponse = responseBody.toLowerCase();
            if (lowerResponse.contains("success") ||
                    lowerResponse.contains("valid") ||
                    lowerResponse.contains("authenticated") ||
                    lowerResponse.contains("login success")) {

                // ENHANCED: Ensure we save valid user data even on JSON parse failure
                String fallbackUserName = (loginId != null && !loginId.trim().isEmpty()) ? loginId : "Unknown_User";
                String fallbackUserId = fallbackUserName;

                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                saveUserData(fallbackUserName, fallbackUserId, fallbackUserId); // using userId as keyNumber fallback
                goToFifthActivity(fallbackUserName, fallbackUserId);
            } else {
                Toast.makeText(this, "Unexpected response format", Toast.LENGTH_SHORT).show();
                isScanning = true;
                startCamera();
            }
        }
    }

    private void saveUserData(String userName, String userId, String keyNumber) {
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Unknown_User";
            Log.w(TAG, "Empty userName provided, using fallback");
        }
        if (userId == null || userId.trim().isEmpty()) {
            userId = userName;
            Log.w(TAG, "Empty userId provided, using userName as fallback");
        }
        if (keyNumber == null || keyNumber.trim().isEmpty()) {
            keyNumber = userId;
            Log.w(TAG, "Empty keyNumber provided, using userId as fallback");
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("LOGGED_IN_USER_NAME", userName.trim());
        editor.putString("LOGGED_IN_USER_ID", userId.trim());
        editor.putString("KEY_NUMBER", keyNumber.trim());  // New field
        editor.putString("CURRENT_USER_NAME", userName.trim());
        editor.putString("CURRENT_USER_ID", userId.trim());
        editor.putLong("LOGIN_TIMESTAMP", System.currentTimeMillis());

        boolean saved = editor.commit();
        Log.d(TAG, "User data save result: " + saved + " - Name: '" + userName + "', ID: '" + userId + "', Key: '" + keyNumber + "'");
    }


    private String extractUserName(JSONObject jsonResponse, String defaultValue) {
        // Try various field names for user name
        String userName = jsonResponse.optString("user_name", "");
        if (userName.isEmpty()) userName = jsonResponse.optString("username", "");
        if (userName.isEmpty()) userName = jsonResponse.optString("name", "");
        if (userName.isEmpty()) userName = jsonResponse.optString("display_name", "");
        if (userName.isEmpty()) userName = jsonResponse.optString("full_name", "");

        // Check if there's a data object
        if (userName.isEmpty() && jsonResponse.has("data")) {
            try {
                JSONObject dataObject = jsonResponse.getJSONObject("data");
                userName = dataObject.optString("user_name", "");
                if (userName.isEmpty()) userName = dataObject.optString("name", "");
                if (userName.isEmpty()) userName = dataObject.optString("display_name", "");
            } catch (JSONException e) {
                Log.w(TAG, "Error parsing data object: " + e.getMessage());
            }
        }

        return userName.isEmpty() ? defaultValue : userName;
    }

    private String extractUserId(JSONObject jsonResponse, String defaultValue) {
        // Try various field names for user ID
        String userId = jsonResponse.optString("user_id", "");
        if (userId.isEmpty()) userId = jsonResponse.optString("id", "");
        if (userId.isEmpty()) userId = jsonResponse.optString("login_id", "");

        // Check if there's a data object
        if (userId.isEmpty() && jsonResponse.has("data")) {
            try {
                JSONObject dataObject = jsonResponse.getJSONObject("data");
                userId = dataObject.optString("user_id", "");
                if (userId.isEmpty()) userId = dataObject.optString("id", "");
            } catch (JSONException e) {
                Log.w(TAG, "Error parsing data object for user ID: " + e.getMessage());
            }
        }

        return userId.isEmpty() ? defaultValue : userId;
    }

    private void goToFifthActivity(String userName, String userId) {
        Intent intent = new Intent(Barcodescanner.this, Welcomeactivity.class);
        intent.putExtra("USER_NAME", userName);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}