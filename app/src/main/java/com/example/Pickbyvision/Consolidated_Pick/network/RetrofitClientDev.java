package com.example.Pickbyvision.Consolidated_Pick.network;

import com.example.Pickbyvision.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class RetrofitClientDev {

    private static final String BASE_URL =
            BuildConfig.API_BASE_URL;

    public static final String API_KEY = BuildConfig.VENDOR_API_KEY;

    private static final String API_KEY_HEADER = "XApiKey";

    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit != null) return retrofit;

        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            HostnameVerifier allowAllHosts = (hostname, session) -> true;

            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();
                Request req = original.newBuilder()
                        .header(API_KEY_HEADER, API_KEY)
                        .header("Accept", "*/*")
                        .header("User-Agent", "SupportApp/1.0")
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(req);
            };

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient ok = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(allowAllHosts)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder().setLenient().create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            return retrofit;

        } catch (Exception e) {
            throw new RuntimeException("Failed to build trust-all Retrofit client", e);
        }
    }
    public interface JobActionApi {

        @POST("PICK_BY_VISION_REST_API/PICK/_START_JOB_PICKING")
        Call<JsonObject> startJobPicking(
                @Query("as_login_id") String loginId,
                @Query("order_no") String orderNo,
                @Query("job_no") String jobNo
        );

        @POST("PICK_BY_VISION_REST_API/PICK/_PAUSE_JOB_PICKING")
        Call<JsonObject> pauseJobPicking(
                @Query("as_login_id") String loginId,
                @Query("order_no") String orderNo,
                @Query("job_no") String jobNo
        );

        @POST("PICK_BY_VISION_REST_API/PICK/_RESUME_JOB_PICKING")
        Call<JsonObject> resumeJobPicking(
                @Query("as_login_id") String loginId,
                @Query("order_no") String orderNo,
                @Query("job_no") String jobNo
        );
    }
}

