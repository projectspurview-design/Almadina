// com.example.supportapp.Pick_Consolidated.model.ConsolItem.java
package com.example.supportapp.Pick_Consolidated.model;

import java.io.Serializable;

public class ConsolItem implements Serializable {
    public String transBatchId;
    public String companyCode;
    public String prinCode;
    public String pickUser;

    public ConsolItem(String transBatchId, String companyCode, String prinCode, String pickUser) {
        this.transBatchId = transBatchId;
        this.companyCode  = companyCode;
        this.prinCode     = prinCode;
        this.pickUser     = pickUser;
    }
}
