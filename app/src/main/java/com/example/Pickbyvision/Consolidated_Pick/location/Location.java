package com.example.Pickbyvision.Consolidated_Pick.location;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Location implements Serializable {

    @SerializedName("TRANS_BATCH_ID") private String transBatchId;
    @SerializedName("SITE_CODE")      private String siteCode;
    @SerializedName("LOCATION_CODE")  private String locationCode;
    @SerializedName("PROD_CODE")      private String prodCode;
    @SerializedName("ORDER_NO")       private String orderNo;
    @SerializedName("JOB_NO")         private String jobNo;

    @SerializedName("QUANTITY")       private int quantity;

    @SerializedName(value = "PDA_QUANTITY", alternate = {"PICK_QTY","PICKQTY","PICKED_QTY"})
    private int pickQty;

    @SerializedName("COMPANY_CODE")   private String companyCode;
    @SerializedName("PRIN_CODE")      private String prinCode;

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
