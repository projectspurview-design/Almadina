package com.example.supportapp.Pick_Consolidated.model;

import com.google.gson.annotations.SerializedName;

public class ConsolidatedId {
    @SerializedName("PRIN_CODE")
    public String prinCode;

    @SerializedName("TRANS_BATCH_ID")
    public String transBatchId;

    @SerializedName("USER_DT")
    public String userDt;

    @SerializedName("COMPANY_CODE")
    public String companyCode;
}
