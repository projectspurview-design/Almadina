package com.example.Pickbyvision.Induvidual_Pick.network;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.*;

import okhttp3.OkHttpClient;

public final class UnsafeOkHttpClient {

    private UnsafeOkHttpClient() {}

    public static OkHttpClient get() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(
                                java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {}

                        @Override public void checkServerTrusted(
                                java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {}

                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(
                            sslContext.getSocketFactory(),
                            (X509TrustManager) trustAllCerts[0]
                    )
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            return new OkHttpClient.Builder().build();
        }
    }
}
