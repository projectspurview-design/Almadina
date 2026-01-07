package com.example.Pickbyvision.Consolidated_Pick.repo;

import com.example.Pickbyvision.Consolidated_Pick.model.ApiMessage;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedId;
import com.example.Pickbyvision.Consolidated_Pick.model.ConsolidatedPickDetail;
import com.example.Pickbyvision.Consolidated_Pick.location.Location;
import com.example.Pickbyvision.Consolidated_Pick.model.ScanBarcodeRequest;
import com.example.Pickbyvision.Consolidated_Pick.network.ConsolidatedPickingApi;
import com.example.Pickbyvision.Consolidated_Pick.network.RetrofitClientDev;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ConsolidatedRepository {

    private final ConsolidatedPickingApi api =
            RetrofitClientDev.getInstance().create(ConsolidatedPickingApi.class);





    public List<Location> getConsolidatedPicklistBlocking(
            String companyCode, String prinCode, String transactionId, String pickUser
    ) throws IOException {
        Response<List<Location>> response =
                api.getConsolidatedPicklist(companyCode, prinCode, transactionId, pickUser).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch locations: " + response.code() + " " + response.message());
        }
        return response.body();
    }

    public ConsolidatedPickDetail getConsolidatedPickDetailBlocking(
            String companyCode,
            String prinCode,
            String transBatchId,
            String jobNo,
            String siteCode,
            String locationCode,
            String prodCode
    ) throws IOException {
        Response<List<ConsolidatedPickDetail>> response =
                api.getConsolidatedPickDetail(companyCode, prinCode, transBatchId, jobNo, siteCode, locationCode, prodCode)
                        .execute();

        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
            throw new IOException("Failed to fetch ConsolidatedPickDetail: " + response.code() + " - " + response.message());
        }
        return response.body().get(0);
    }

    public ApiMessage scanBarcodeBlocking(ScanBarcodeRequest req) throws IOException {
        Response<ApiMessage> r = api.scanBarcode2(req).execute();
        if (!r.isSuccessful() || r.body() == null) {
            String msg = r.errorBody() != null ? r.errorBody().string() : ("HTTP " + r.code());
            throw new IOException("SCAN_BARCODE failed: " + msg);
        }
        return r.body();
    }

    /** Try to complete the batch. Returns true only when server responds 200 (completed). */
    public boolean tryCompleteConsolidatedPicking(String loginId, String transBatchId) throws IOException {
        Response<ResponseBody> r = api.completeConsolidatedPicking(loginId, transBatchId).execute();
        if (r.isSuccessful()) return true;

        String msg = r.errorBody() != null ? r.errorBody().string() : ("HTTP " + r.code());
        if (r.code() == 400 && msg != null && msg.toLowerCase().contains("still need to be picked")) {
            return false;
        }
        throw new IOException("COMPLETE_CONSOLIDATED_PICKING failed: " + msg);
    }




}
