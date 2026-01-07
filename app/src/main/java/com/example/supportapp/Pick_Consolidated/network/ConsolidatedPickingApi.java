package com.example.supportapp.Pick_Consolidated.network;

import com.example.supportapp.Pick_Consolidated.model.ApiMessage;
import com.example.supportapp.Pick_Consolidated.model.ConsolidatedId;
import com.example.supportapp.Pick_Consolidated.model.ConsolidatedPickDetail;
import com.example.supportapp.Pick_Consolidated.model.Location;
import com.example.supportapp.Pick_Consolidated.model.ScanBarcodeRequest;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ConsolidatedPickingApi {

    // ----- Auth / lists -----
    @GET("ConsolidatedPicking/CHECK_LOGIN")
    Call<JsonObject> checkLogin(
            @Query("as_login_id") String loginId,
            @Query("as_log_pass") String password
    );

    @GET("ConsolidatedPicking/CONSOLIDATED_IDS")
    Call<List<ConsolidatedId>> getConsolidatedIds(
            @Query("as_putaway_user") String asPutawayUser
    );

    @GET("ConsolidatedPicking/CONSOLIDATED_PICKLIST")
    Call<List<Location>> getConsolidatedPicklist(
            @Query("as_company_code") String companyCode,
            @Query("as_prin_code") String prinCode,
            @Query("as_TRANS_BATCH_ID") String transactionId,
            @Query("as_pick_user") String pickUser
    );

    @GET("ConsolidatedPicking/CONSOLIDATED_PICK_DETAIL")
    Call<List<ConsolidatedPickDetail>> getConsolidatedPickDetail(
            @Query("as_company_code")   String companyCode,
            @Query("as_prin_code")      String prinCode,
            @Query("as_TRANS_BATCH_ID") String transBatchId,
            @Query("as_job_no")         String jobNo,
            @Query("as_site_code")      String siteCode,
            @Query("as_location_code")  String locationCode,
            @Query("as_prod_code")      String prodCode
    );

    @GET("api/ConsolidatedPicking/GET_CONSOLIDATED_STATUS")
    Call<JsonObject> getConsolidatedStatus(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String batchId
    );

    @GET("PICK_/GET_OUTBOUND_ALLOTED_TIME")
    Call<JsonObject> getOutboundAllotedTime(
            @Query("as_prin_code") String prinCode
    );

    // ----- Scan -----
    @POST("ConsolidatedPicking/SCAN_BARCODE")
    Call<ApiMessage> scanBarcode2(@Body ScanBarcodeRequest body);

    // ----- Consolidated batch control (from your Swagger) -----
    @POST("ConsolidatedPicking/START_CONSOLIDATED_PICKING")
    Call<ResponseBody> startConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    @POST("ConsolidatedPicking/PAUSE_CONSOLIDATED_PICKING")
    Call<ResponseBody> pauseConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    @POST("ConsolidatedPicking/RESUME_CONSOLIDATED_PICKING")
    Call<ResponseBody> resumeConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    @POST("ConsolidatedPicking/COMPLETE_CONSOLIDATED_PICKING")
    Call<ResponseBody> completeConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    // ====== 🔙 Added back for CompletedordersummaryconsolidatedActivity ======
    // NOTE: Your earlier code called this as: api.getOutboundAllocatedTime(orderNumber, System.currentTimeMillis())
    // The endpoint in your earlier interface used "as_prin_code". If your backend actually expects an order number,
    // that’s fine—this compiles; just verify the server-side param name.
    @GET("PICK_/GET_OUTBOUND_ALLOTED_TIME")
    Call<JsonObject> getOutboundAllocatedTime(
            @Query("as_prin_code") String value,   // you pass orderNumber here in your Activity
            @Query("ts") long timestamp
    );

    @GET("PICK_/GET_JOB_TIMING")
    Call<JsonObject> getJobTiming(
            @Query("as_login_id") String loginId,
            @Query("order_no") String orderNo,
            @Query("job_no") String jobNo,
            @Query("ts") long timestamp
    );
}
