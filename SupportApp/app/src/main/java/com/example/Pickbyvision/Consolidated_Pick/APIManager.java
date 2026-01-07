package com.example.Pickbyvision.Consolidated_Pick;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

public class APIManager {
    private static APIManager instance;
    private RequestQueue requestQueue;
    private static Context context;

    // Base URLs
    private static final String BASE_URL = "https://apps.almadinalogistics.com:4432/PICK_BY_VISION_REST_API/api/";

    // API Endpoints
    private static final String CONSOLIDATED_TRANSACTION_ENDPOINT = "ConsolidatedPicking/CONSOLIDATED_IDS";
    private static final String LOCATION_DETAILS_ENDPOINT = "location-details";
    private static final String USER_DATA_ENDPOINT = "user-data";

    private APIManager(Context context) {
        APIManager.context = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized APIManager getInstance(Context context) {
        if (instance == null) {
            instance = new APIManager(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public interface APICallback<T> {
        void onSuccess(T response);
        void onError(String error);
    }

    public void getConsolidatedTransactions(String putawayUser, APICallback<JSONObject> callback) {
        String url = BASE_URL + CONSOLIDATED_TRANSACTION_ENDPOINT + "?as_putaway_user=" + putawayUser;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Network error";
                        if (error.networkResponse != null) {
                            errorMessage = "Error code: " + error.networkResponse.statusCode;
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Accept", "*/");
                headers.put("ApiKey", "bkV7TzFDJX4m55fY-5LqI2BvsFoIMK");
                return headers;
            }
        };

        addToRequestQueue(jsonRequest);
    }
}