package com.example.Pickbyvision.Induvidual_Pick.network;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.*;

public class BarcodeLoginApi {

    private final OkHttpClient client;

    public interface LoginCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public BarcodeLoginApi() {
        client = UnsafeOkHttpClient.get();
    }

    public void login(String loginId, String password, LoginCallback callback) {

        String url = ApiConfig.CHECK_LOGIN
                + "?as_login_id=" + Uri.encode(loginId)
                + "&as_log_pass=" + Uri.encode(password);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "*/*")
                .addHeader(ApiConfig.HEADER_API_KEY, ApiConfig.API_KEY)
                .addHeader("User-Agent", "SupportApp/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                try {
                    if (!response.isSuccessful()) {
                        callback.onError("Server error : " + response.code());
                        return;
                    }

                    String body = response.body() != null
                            ? response.body().string().trim()
                            : "";

                    callback.onSuccess(body);

                } finally {
                    response.close();
                }
            }
        });
    }
}
