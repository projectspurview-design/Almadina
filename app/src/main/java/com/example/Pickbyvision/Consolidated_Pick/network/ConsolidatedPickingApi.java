package com.example.Pickbyvision.Consolidated_Pick.network;

import com.example.Pickbyvision.Consolidated_Pick.model.ApiMessage;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedId;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedPickDetail;
import com.example.Pickbyvision.Consolidated_Pick.location.Location;
import com.example.Pickbyvision.Consolidated_Pick.model.ScanBarcodeRequest;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ConsolidatedPickingApi {

    @GET("ConsolidatedPicking/CHECK_LOGIN")
    Call<JsonObject> checkLogin(
            @Query("as_login_id") String loginId,
            @Query("as_log_pass") String password
    );

    @GET("api/ConsolidatedPicking/CONSOLIDATED_IDS")
    Call<List<ConsolidatedId>> getConsolidatedIds(
            @Query("as_putaway_user") String putawayUser
    );



    @GET("api/ConsolidatedPicking/CONSOLIDATED_PICKLIST")
    Call<List<Location>> getConsolidatedPicklist(
            @Query("as_company_code") String companyCode,
            @Query("as_prin_code") String prinCode,
            @Query("as_TRANS_BATCH_ID") String transactionId,
            @Query("as_pick_user") String pickUser
    );

    @GET("/PICK_BY_VISION_REST_API/PICK_/GET_OUTBOUND_ALLOTED_TIME")
    Call<JsonObject> getOutboundAllocatedTime(
            @Query("as_prin_code") String prinCode,
            @Query("ts") long timestamp
    );

    @GET("/PICK_BY_VISION_REST_API/PICK_/GET_JOB_TIMING")
    Call<JsonObject> getJobTiming(
            @Query("as_login_id") String loginId,
            @Query("order_no") String orderNo,
            @Query("job_no") String jobNo,
            @Query("ts") long timestamp
    );

    @GET("PICK_/GET_OUTBOUND_ALLOTED_TIME")
    Call<JsonObject> getOutboundAllocatedTime(@Query("as_prin_code") String prinCode);



    @GET("api/ConsolidatedPicking/CONSOLIDATED_PICK_DETAIL")
    Call<List<ConsolidatedPickDetail>> getConsolidatedPickDetail(
            @Query("as_company_code")   String companyCode,
            @Query("as_prin_code")      String prinCode,
            @Query("as_TRANS_BATCH_ID") String transBatchId,
            @Query("as_job_no")         String jobNo,
            @Query("as_site_code")      String siteCode,
            @Query("as_location_code")  String locationCode,
            @Query("as_prod_code")      String prodCode
    );

    @POST("api/ConsolidatedPicking/SCAN_BARCODE")
    Call<ApiMessage> scanBarcode2(@Body ScanBarcodeRequest body);

    @POST("api/ConsolidatedPicking/START_CONSOLIDATED_PICKING")
    Call<ResponseBody> startConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    @POST("api/ConsolidatedPicking/PAUSE_CONSOLIDATED_PICKING")
    Call<ResponseBody> pauseConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    @POST("api/ConsolidatedPicking/RESUME_CONSOLIDATED_PICKING")
    Call<ResponseBody> resumeConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );

    @POST("api/ConsolidatedPicking/COMPLETE_CONSOLIDATED_PICKING")
    Call<ResponseBody> completeConsolidatedPicking(
            @Query("as_login_id") String loginId,
            @Query("as_trans_batch_id") String transBatchId
    );


    @GET("PICK_/GET_OUTBOUND_ALLOTED_TIME")
    Call<JsonObject> getOutboundAllotedTime(
            @Query("as_prin_code") String prinCode
    );


}