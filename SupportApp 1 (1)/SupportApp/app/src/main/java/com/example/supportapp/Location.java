package com.example.supportapp; // Ensure this matches your package name

public class Location {
    private String id;
    private String name;        // e.g., "006 Mahebi Enterprises"
    private String site;        // e.g., "F5"
    private String locationCode; // e.g., "042901"
    private int quantity;       // e.g., 1

    // Constructor
    public Location(String id, String name, String site, String locationCode, int quantity) {
        this.id = id;
        this.name = name;
        this.site = site;
        this.locationCode = locationCode;
        this.quantity = quantity;
    }


    // Getter methods
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

    // Optional: Setters if you need to modify location objects after creation
    // public void setId(String id) { this.id = id; }
    // public void setName(String name) { this.name = name; }
    // public void setSite(String site) { this.site = site; }
    // public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    // public void setQuantity(int quantity) { this.quantity = quantity; }
}