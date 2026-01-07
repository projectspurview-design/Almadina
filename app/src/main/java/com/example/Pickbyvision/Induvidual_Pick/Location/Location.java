package com.example.Pickbyvision.Induvidual_Pick.Location;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

public class Location implements Parcelable {
    private String id;
    private String name;
    private String site;
    private String locationCode;
    private int quantity;


    private String product;
    private String description;
    private String expiration;
    private String manufacturing;


    private String qtyPUOM;
    private String qtyLUOM;


    private String pdAVerified;


    private String awsPath;


    public Location(String id, String name, String site, String locationCode, int quantity,
                    String product, String description, String expiration, String manufacturing,
                    String qtyPUOM, String qtyLUOM, String pdAVerified, String awsPath) {
        this.id = id;
        this.name = name;
        this.site = site;
        this.locationCode = locationCode;
        this.quantity = quantity;
        this.product = product;
        this.description = description;
        this.expiration = expiration;
        this.manufacturing = manufacturing;
        this.qtyPUOM = qtyPUOM;
        this.qtyLUOM = qtyLUOM;
        this.pdAVerified = pdAVerified;
        this.awsPath = awsPath;
    }


    public Location(String id, String locationCode, String product, String description,
                    String expiration, String manufacturing, int quantity, String pdAVerified, String awsPath) {
        this(id, null, null, locationCode, quantity, product, description,
                expiration, manufacturing, null, null, pdAVerified, awsPath);
    }



    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSite() {
        return site;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getProduct() {
        return product;
    }

    public String getDescription() {
        return description;
    }

    public String getExpiration() {
        return expiration;
    }

    public String getManufacturing() {
        return manufacturing;
    }

    public String getQtyPUOM() {
        return qtyPUOM;
    }

    public String getQtyLUOM() {
        return qtyLUOM;
    }

    public String getPdAVerified() {
        return pdAVerified;
    }

    public void setPdAVerified(String pdAVerified) {
        this.pdAVerified = pdAVerified;
    }


    public String getAwsPath() {
        return awsPath;
    }


    public void setAwsPath(String awsPath) {
        this.awsPath = awsPath;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", site='" + site + '\'' +
                ", locationCode='" + locationCode + '\'' +
                ", quantity=" + quantity +
                ", product='" + product + '\'' +
                ", description='" + description + '\'' +
                ", expiration='" + expiration + '\'' +
                ", manufacturing='" + manufacturing + '\'' +
                ", qtyPUOM='" + qtyPUOM + '\'' +
                ", qtyLUOM='" + qtyLUOM + '\'' +
                ", pdAVerified='" + pdAVerified + '\'' +
                ", awsPath='" + awsPath + '\'' +
                '}';
    }


    protected Location(Parcel in) {
        id = in.readString();
        name = in.readString();
        site = in.readString();
        locationCode = in.readString();
        quantity = in.readInt();
        product = in.readString();
        description = in.readString();
        expiration = in.readString();
        manufacturing = in.readString();
        qtyPUOM = in.readString();
        qtyLUOM = in.readString();
        pdAVerified = in.readString();
        awsPath = in.readString();
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(site);
        dest.writeString(locationCode);
        dest.writeInt(quantity);
        dest.writeString(product);
        dest.writeString(description);
        dest.writeString(expiration);
        dest.writeString(manufacturing);
        dest.writeString(qtyPUOM);
        dest.writeString(qtyLUOM);
        dest.writeString(pdAVerified);
        dest.writeString(awsPath);
    }




    public static Location fromJsonString(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        return new Location(
                json.optString("id"),
                json.optString("name"),
                json.optString("site"),
                json.optString("locationCode"),
                json.optInt("quantity"),
                json.optString("product"),
                json.optString("description"),
                json.optString("expiration"),
                json.optString("manufacturing"),
                json.optString("qtyPUOM"),
                json.optString("qtyLUOM"),
                json.optString("pdAVerified"),
                json.optString("awsPath")
        );
    }


    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("site", site);
            json.put("locationCode", locationCode);
            json.put("quantity", quantity);
            json.put("product", product);
            json.put("description", description);
            json.put("expiration", expiration);
            json.put("manufacturing", manufacturing);
            json.put("qtyPUOM", qtyPUOM);
            json.put("qtyLUOM", qtyLUOM);
            json.put("pdAVerified", pdAVerified);
            json.put("awsPath", awsPath);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json.toString();
    }


    public static Location fromJson(JSONObject json) throws JSONException {
        return new Location(
                json.optString("key_number", ""),
                json.optString("prod_desc", ""),
                json.optString("site_code", ""),
                json.optString("location_code", ""),
                json.optInt("quantity", 0),
                json.optString("prod_code", ""),
                json.optString("prod_desc", ""),
                json.optString("exp_date", ""),
                json.optString("mfg_date", ""),
                json.optString("qtY_PUOM", ""),
                json.optString("qtY_LUOM", ""),
                json.optString("pdA_VERIFIED", "N"),
                json.optString("aws_path", "")
        );
    }
}