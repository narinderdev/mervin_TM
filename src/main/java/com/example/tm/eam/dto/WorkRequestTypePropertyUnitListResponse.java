package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkRequestTypePropertyUnitListResponse {

    private List<String> propertyUnits;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
