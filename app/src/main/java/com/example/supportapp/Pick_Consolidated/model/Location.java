package com.example.supportapp.Pick_Consolidated.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Location implements Serializable {

    @SerializedName("TRANS_BATCH_ID") private String transBatchId;
    @SerializedName("SITE_CODE")      private String siteCode;
    @SerializedName("LOCATION_CODE")  private String locationCode;
    @SerializedName("PROD_CODE")      private String prodCode;
    @SerializedName("ORDER_NO")       private String orderNo;     // present in your JSON
    @SerializedName("JOB_NO")         private String jobNo;

    @SerializedName("QUANTITY")       private int quantity;       // total to pick

    // IMPORTANT: API returns PDA_QUANTITY (not PICK_QTY). Support both just in case.
    @SerializedName(value = "PDA_QUANTITY", alternate = {"PICK_QTY","PICKQTY","PICKED_QTY"})
    private int pickQty;                                          // picked so far

    // (Optional) only if other endpoints actually return these:
    @SerializedName("COMPANY_CODE")   private String companyCode;
    @SerializedName("PRIN_CODE")      private String prinCode;

    // Getters
    public String getTransBatchId() { return nz(transBatchId); }
    public String getSiteCode()      { return nz(siteCode); }
    public String getLocationCode()  { return nz(locationCode); }
    public String getProdCode()      { return nz(prodCode); }
    public String getOrderNo()       { return nz(orderNo); }
    public String getJobNo()         { return nz(jobNo); }

    public int getQuantity()         { return quantity; }
    public int getPickQty()          { return pickQty; }

    public String getCompanyCode()   { return nz(companyCode); }
    public String getPrinCode()      { return nz(prinCode); }

    private static String nz(String s) { return s == null ? "" : s; }

    @Override public String toString() {
        return "Location{" +
                "transBatchId='" + transBatchId + '\'' +
                ", siteCode='" + siteCode + '\'' +
                ", locationCode='" + locationCode + '\'' +
                ", prodCode='" + prodCode + '\'' +
                ", orderNo='" + orderNo + '\'' +
                ", jobNo='" + jobNo + '\'' +
                ", quantity=" + quantity +
                ", pickQty=" + pickQty +
                '}';
    }
}
