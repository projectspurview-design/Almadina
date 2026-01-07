package com.example.supportapp.Pick_Consolidated.model;

import com.google.gson.annotations.SerializedName;

public class ScanBarcodeRequest {
    @SerializedName("companyCode")  public String companyCode;
    @SerializedName("prinCode")     public String prinCode;
    @SerializedName("transBatchId") public String transBatchId;
    @SerializedName("jobNo")        public String jobNo;
    @SerializedName("siteCode")     public String siteCode;
    @SerializedName("locationCode") public String locationCode;
    @SerializedName("orderNo")      public String orderNo;
    @SerializedName("productCode")  public String productCode;
    @SerializedName("palletId")     public String palletId;
    @SerializedName("userId")       public String userId;

    // Changed from pdaQty to quantity1 and quantity2
    @SerializedName("quantity1")    public int quantity1;
    @SerializedName("quantity2")    public int quantity2;

    public ScanBarcodeRequest() {}

    public ScanBarcodeRequest(String companyCode, String prinCode, String transBatchId,
                              String jobNo, String siteCode, String locationCode,
                              String orderNo, String productCode, String palletId,
                              String userId, int quantity1) {
        this.companyCode = companyCode;
        this.prinCode = prinCode;
        this.transBatchId = transBatchId;
        this.jobNo = jobNo;
        this.siteCode = siteCode;
        this.locationCode = locationCode;
        this.orderNo = orderNo;
        this.productCode = productCode;
        this.palletId = palletId;
        this.userId = userId;
        this.quantity1 = quantity1;
        this.quantity2 = 0;  // Always 0 as per Python code
    }
}