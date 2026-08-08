package com.wxzj.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MigrateReport {

    private int totalRows;
    private int communityCreated;
    private int buildingCreated;
    private int houseCreated;
    private int ownerCreated;
    private int accountCreated;
    private int depositCreated;
    private int flowCreated;
    private int skipped;
    private int errorRows;
    private List<String> errors = new ArrayList<>();

    public void error(String msg) {
        errors.add(msg);
        errorRows++;
    }
}
