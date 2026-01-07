package com.example.supportapp.Pick_Consolidated.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class ConsolidatedPickDetail implements Parcelable {
    @SerializedName("PROD_CODE")      private String prodCode;
    @SerializedName("QUANTITY")       private Integer quantity;
    @SerializedName("QTY_PUOM")       private Integer qtyPuom;
    @SerializedName("P_UOM")          private String pUom;
    @SerializedName("QTY_LUOM")       private Integer qtyLuom;
    @SerializedName("L_UOM")          private String lUom;
    @SerializedName("PDA_QUANTITY")   private Integer pdaQuantity;
    @SerializedName("PROD_NAME")      private String prodName;
    @SerializedName("SITE_CODE")      private String siteCode;
    @SerializedName("LOCATION_CODE")  private String locationCode;
    @SerializedName("UPPP")           private Integer uppp;
    @SerializedName("ORDER_NO")       private String orderNo;

    // ✅ NEW FIELD: AWS Image Path
    @SerializedName("AWS_PATH")
    private String awsPath;

    // flexible types
    @SerializedName("LOT_NO")         private JsonElement lotNo;
    @SerializedName("BATCH_ID")       private JsonElement batchId;
    @SerializedName("ORIGIN_COUNTRY") private JsonElement originCountry;

    // EXP/MFG can vary in type
    @SerializedName("EXP_DATE")       private JsonElement expDateEl;
    @SerializedName("TRANS_BATCH_ID") private String transBatchId;
    @SerializedName("MFG_DATE")       private JsonElement mfgDateEl;
    @SerializedName("JOB_NO")         private String jobNo;

    // cached string forms for parceling
    private transient String expDateStr;
    private transient String mfgDateStr;

    public ConsolidatedPickDetail() {}

    // ===== Getters =====
    public String getProdCode()      { return prodCode; }
    public Integer getQuantity()     { return quantity; }
    public String getProdName()      { return prodName; }
    public String getSiteCode()      { return siteCode; }
    public String getLocationCode()  { return locationCode; }
    public String getTransBatchId()  { return transBatchId; }
    public String getJobNo()         { return jobNo; }
    public String getOrderNo()       { return orderNo; }

    // ✅ Getter & Setter for AWS Path
    public String getAwsPath() { return awsPath; }
    public void setAwsPath(String awsPath) { this.awsPath = awsPath; }

    public String getLotNoAsString()         { return asString(lotNo); }
    public String getBatchIdAsString()       { return asString(batchId); }
    public String getOriginCountryAsString() { return asString(originCountry); }

    public String getExpDate() {
        if (expDateStr != null) return expDateStr;
        return asDateString(expDateEl);
    }
    public String getMfgDate() {
        if (mfgDateStr != null) return mfgDateStr;
        return asDateString(mfgDateEl);
    }

    // ===== Utility Parsers =====
    private static String asString(JsonElement el) {
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : "";
    }

    private static String asDateString(JsonElement el) {
        if (el == null) return "";
        try {
            if (el.isJsonNull()) return "";

            if (el.isJsonPrimitive()) {
                if (el.getAsJsonPrimitive().isString()) {
                    String s = el.getAsString();
                    if (s == null) return "";
                    s = s.trim();
                    if (s.isEmpty() || s.equals("--") || s.equalsIgnoreCase("null") || s.equals("0000-00-00")) return "";
                    return s;
                }
                if (el.getAsJsonPrimitive().isNumber()) {
                    long v = el.getAsLong();
                    if (String.valueOf(Math.abs(v)).length() == 10) v *= 1000L; // sec → ms
                    return formatEpochYMD(v);
                }
            }

            if (el.isJsonObject()) {
                if (el.getAsJsonObject().has("$date")) {
                    JsonElement v = el.getAsJsonObject().get("$date");
                    if (v != null) {
                        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                            return v.getAsString();
                        } else if (v.isJsonObject() && v.getAsJsonObject().has("$numberLong")) {
                            long ms = Long.parseLong(v.getAsJsonObject().get("$numberLong").getAsString());
                            return formatEpochYMD(ms);
                        }
                    }
                }
                if (el.getAsJsonObject().has("$numberLong")) {
                    long ms = Long.parseLong(el.getAsJsonObject().get("$numberLong").getAsString());
                    return formatEpochYMD(ms);
                }
                if (el.getAsJsonObject().has("date")) {
                    return el.getAsJsonObject().get("date").getAsString();
                }
                if (el.getAsJsonObject().has("time")) {
                    return el.getAsJsonObject().get("time").getAsString();
                }
                return el.toString();
            }
        } catch (Exception ignore) {}
        return "";
    }

    private static String formatEpochYMD(long millis) {
        try {
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            return out.format(new java.util.Date(millis));
        } catch (Exception e) {
            return "";
        }
    }

    // ===== Parcelable Implementation =====
    protected ConsolidatedPickDetail(Parcel in) {
        prodCode = in.readString();
        quantity = in.readByte()==0? null: in.readInt();
        qtyPuom  = in.readByte()==0? null: in.readInt();
        pUom = in.readString();
        qtyLuom  = in.readByte()==0? null: in.readInt();
        lUom = in.readString();
        pdaQuantity = in.readByte()==0? null: in.readInt();
        prodName = in.readString();
        siteCode = in.readString();
        locationCode = in.readString();
        uppp = in.readByte()==0? null: in.readInt();
        expDateStr = in.readString();
        transBatchId = in.readString();
        mfgDateStr = in.readString();
        jobNo = in.readString();
        orderNo = in.readString();
        awsPath = in.readString(); // ✅ include in Parcel
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(prodCode);
        if (quantity==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(quantity); }
        if (qtyPuom==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(qtyPuom); }
        dest.writeString(pUom);
        if (qtyLuom==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(qtyLuom); }
        dest.writeString(lUom);
        if (pdaQuantity==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(pdaQuantity); }
        dest.writeString(prodName);
        dest.writeString(siteCode);
        dest.writeString(locationCode);
        if (uppp==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeInt(uppp); }
        dest.writeString(getExpDate());
        dest.writeString(transBatchId);
        dest.writeString(getMfgDate());
        dest.writeString(jobNo);
        dest.writeString(orderNo);
        dest.writeString(awsPath); // ✅ include in Parcel
    }
    public Integer getPickQty() {
        return pdaQuantity; // PDA_QUANTITY from JSON
    }

    @Override public int describeContents() { return 0; }

    public static final Creator<ConsolidatedPickDetail> CREATOR = new Creator<>() {
        @Override
        public ConsolidatedPickDetail createFromParcel(Parcel in) { return new ConsolidatedPickDetail(in); }
        @Override
        public ConsolidatedPickDetail[] newArray(int size) { return new ConsolidatedPickDetail[size]; }
    };
}
