package com.example.Pickbyvision.Consolidated_Pick.model;

import com.google.gson.annotations.SerializedName;

public class ScanBarcodeRequest {

    @SerializedName("companyCode")
    private String companyCode;

    @SerializedName("prinCode")
    private String prinCode;

    @SerializedName("transBatchId")
    private String transBatchId;

    @SerializedName("jobNo")
    private String jobNo;

    @SerializedName("siteCode")
    private String siteCode;

    @SerializedName("locationCode")
    private String locationCode;

    @SerializedName("orderNo")
    private String orderNo;

    @SerializedName("productCode")
    private String productCode;

    @SerializedName("palletId")
    private String palletId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("quantity1")
    private int quantity1;

    @SerializedName("quantity2")
    private int quantity2;


    public ScanBarcodeRequest(
            String companyCode,
            String prinCode,
            String transBatchId,
            String jobNo,
            String siteCode,
            String locationCode,
            String orderNo,
            String productCode,
            String palletId,
            String userId,
            int quantity1,
            int quantity2
    ) {
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
        this.quantity2 = quantity2;
    }

    public String getCompanyCode() { return companyCode; }
    public String getPrinCode() { return prinCode; }
    public String getTransBatchId() { return transBatchId; }
    public String getJobNo() { return jobNo; }
    public String getSiteCode() { return siteCode; }
    public String getLocationCode() { return locationCode; }
    public String getOrderNo() { return orderNo; }
    public String getProductCode() { return productCode; }
    public String getPalletId() { return palletId; }
    public String getUserId() { return userId; }
    public int getQuantity1() { return quantity1; }
    public int getQuantity2() { return quantity2; }
}