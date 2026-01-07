package com.example.supportapp;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

public class Location implements Parcelable {
    private String id; // Mapped from "key_number"
    private String name; // Mapped from "prod_desc" (for display in list, typically in PickLocationActivity)
    private String site; // Mapped from "site_code"
    private String locationCode; // Mapped from "location_code"
    private int quantity; // Mapped from "quantity"

    // Fields for TenthActivity display
    private String product;        // Mapped from "prod_code" (likely the SKU to scan)
    private String description;    // Mapped from "prod_desc" (as a dedicated description field in TenthActivity)
    private String expiration;     // Mapped from "exp_date"
    private String manufacturing;  // Mapped from "mfg_date"

    // Fields for qty_PUOM and qty_LUOM as per API and curl example
    private String qtyPUOM;      // Mapped from "qtY_PUOM"
    private String qtyLUOM;      // Mapped from "qtY_LUOM"

    // Field for pdA_VERIFIED status
    private String pdAVerified;  // Mapped from "pdA_VERIFIED"

    // NEW FIELD for AWS image path
    private String awsPath;      // Mapped from "aws_path"

    // Primary Constructor to initialize all fields (13 arguments now)
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

    // Constructor for loading from SharedPreferences in TenthActivity (9 arguments now)
    public Location(String id, String locationCode, String product, String description,
                    String expiration, String manufacturing, int quantity, String pdAVerified, String awsPath) {
        this(id, null, null, locationCode, quantity, product, description,
                expiration, manufacturing, null, null, pdAVerified, awsPath);
    }

    public Location(String lastLocationId, String lastLocationCode, String lastProduct, String lastDescription, String lastExpiration, String lastManufacturing, int lastQuantity, String lastPdaVerified) {
    }

    // --- Getter methods ---
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

    // NEW GETTER for AWS path
    public String getAwsPath() {
        return awsPath;
    }

    // NEW SETTER for AWS path
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

    // --- Parcelable Implementation ---

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
        awsPath = in.readString(); // NEW: Read AWS path from parcel
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
        dest.writeString(awsPath); // NEW: Write AWS path to parcel
    }

    // --- JSON Conversion Methods ---

    /**
     * Creates a Location object from a JSON string for SharedPreferences.
     */
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
                json.optString("awsPath") // NEW: Include AWS path
        );
    }

    /**
     * Converts the Location object to a JSON string for SharedPreferences.
     */
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
            json.put("awsPath", awsPath); // NEW: Include AWS path
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json.toString();
    }

    /**
     * Creates a Location object from a JSONObject (typically from API response).
     * This maps API response keys to Location object fields.
     */
    public static Location fromJson(JSONObject json) throws JSONException {
        return new Location(
                json.optString("key_number", ""),
                json.optString("prod_desc", ""), // Used for 'name'
                json.optString("site_code", ""),
                json.optString("location_code", ""),
                json.optInt("quantity", 0),
                json.optString("prod_code", ""), // Used for 'product'
                json.optString("prod_desc", ""), // Used for 'description'
                json.optString("exp_date", ""),
                json.optString("mfg_date", ""),
                json.optString("qtY_PUOM", ""),
                json.optString("qtY_LUOM", ""),
                json.optString("pdA_VERIFIED", "N"), // Map API's "pdA_VERIFIED", default to "N"
                json.optString("aws_path", "") // NEW: Map AWS path from API response
        );
    }
}