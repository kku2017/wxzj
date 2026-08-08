package com.wxzj.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class MigratePreview {

    private long totalRows;
    private List<String> headers = new ArrayList<>();
    private List<Map<String, String>> sample = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
