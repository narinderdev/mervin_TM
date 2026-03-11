package com.example.tm.eam.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderTypeListResponse {

    private List<Map<String, Object>> workOrderTypes;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
