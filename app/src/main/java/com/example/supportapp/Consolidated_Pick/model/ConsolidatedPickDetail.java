package com.example.supportapp.Consolidated_Pick.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

public class ConsolidatedPickDetail implements Parcelable {

    @SerializedName("PROD_CODE")     private String prodCode;
    @SerializedName("QUANTITY")      private Integer quantity;
    @SerializedName("QTY_PUOM")      private Integer qtyPuom;
    @SerializedName("P_UOM")         private String pUom;
    @SerializedName("QTY_LUOM")      private Integer qtyLuom;
    @SerializedName("L_UOM")         private String lUom;
    @SerializedName("PDA_QUANTITY")  private Integer pdaQuantity;
    @SerializedName("PROD_NAME")     private String prodName;
    @SerializedName("SITE_CODE")     private String siteCode;
    @SerializedName("LOCATION_CODE") private String locationCode;
    @SerializedName("UPPP")          private Integer uppp;

    // These fields are sometimes {} → must be JsonElement
    @SerializedName("LOT_NO")        private JsonElement lotNo;
    @SerializedName("BATCH_ID")      private JsonElement batchId;
    @SerializedName("ORIGIN_COUNTRY") private JsonElement originCountry;

    // ❗ The crash came from here. EXP_DATE is not always string → must be JsonElement
    @SerializedName("EXP_DATE")      private JsonElement expDate;

    @SerializedName("TRANS_BATCH_ID") private String transBatchId;
    @SerializedName("MFG_DATE") private JsonElement mfgDate;

    @SerializedName("JOB_NO")         private String jobNo;

    // NEW (server returns it)
    @SerializedName("ORDER_NO")       private String orderNo;

    public ConsolidatedPickDetail() { }



    // ==== SAFE GETTERS ====
    public String getProdCode()     { return prodCode; }
    public Integer getQuantity()    { return quantity; }
    public String getProdName()     { return prodName; }
    public String getSiteCode()     { return siteCode; }
    public String getLocationCode() { return locationCode; }
    public String getTransBatchId() { return transBatchId; }
    public String getJobNo()        { return jobNo; }
    public String getOrderNo()      { return orderNo == null ? "" : orderNo; }


    /** Safely handle EXP_DATE when server returns "{}" or string */
    public String getExpDate() {
        if (expDate == null) return "";
        if (expDate.isJsonPrimitive()) {
            try { return expDate.getAsString(); }
            catch (Exception ignored) { return ""; }
        }
        return "";  // {} → treated as empty
    }
    /** Safely handle MFG_DATE when server returns "{}" or string */
    public String getMfgDate() {
        if (mfgDate == null) return "";
        if (mfgDate.isJsonPrimitive()) {
            try { return mfgDate.getAsString(); }
            catch (Exception ignored) { return ""; }
        }
        return "";  // {} → treated as empty
    }

    /** Safely handle MFG_DATE when server returns "{}" or string */


    private static String asString(JsonElement el) {
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : "";
    }
    public Integer getPdaQuantity() {
        return pdaQuantity;
    }


    public String getLotNoAsString()        { return asString(lotNo); }
    public String getBatchIdAsString()      { return asString(batchId); }
    public String getOriginCountryAsString(){ return asString(originCountry); }

    // ==== PARCELABLE ====
    protected ConsolidatedPickDetail(Parcel in) {
        prodCode = in.readString();
        quantity = in.readByte() == 0 ? null : in.readInt();
        qtyPuom  = in.readByte() == 0 ? null : in.readInt();
        pUom = in.readString();
        qtyLuom  = in.readByte() == 0 ? null : in.readInt();
        lUom = in.readString();
        pdaQuantity = in.readByte() == 0 ? null : in.readInt();
        prodName = in.readString();
        siteCode = in.readString();
        locationCode = in.readString();
        uppp = in.readByte() == 0 ? null : in.readInt();

        String exp = in.readString();
        expDate = exp.isEmpty() ? null : JsonParser.parseString(exp);
        String exp1 = in.readString();

        transBatchId = in.readString();
        mfgDate      = exp1.isEmpty() ? null : JsonParser.parseString(exp1);
        jobNo        = in.readString();
        orderNo      = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(prodCode);
        if (quantity == null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(quantity); }
        if (qtyPuom  == null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(qtyPuom); }
        dest.writeString(pUom);
        if (qtyLuom  == null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(qtyLuom); }
        dest.writeString(lUom);
        if (pdaQuantity == null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(pdaQuantity); }
        dest.writeString(prodName);
        dest.writeString(siteCode);
        dest.writeString(locationCode);
        if (uppp == null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(uppp); }

        dest.writeString(expDate == null ? "" : expDate.toString());
        dest.writeString(transBatchId);
        dest.writeString(mfgDate == null ? "" : mfgDate.toString());
        dest.writeString(jobNo);
        dest.writeString(orderNo);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<ConsolidatedPickDetail> CREATOR = new Creator<ConsolidatedPickDetail>() {
        @Override
        public ConsolidatedPickDetail createFromParcel(Parcel in) {
            return new ConsolidatedPickDetail(in);
        }

        @Override
        public ConsolidatedPickDetail[] newArray(int size) {
            return new ConsolidatedPickDetail[size];
        }
    };
}
