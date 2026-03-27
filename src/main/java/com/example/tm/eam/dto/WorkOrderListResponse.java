package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers work order list response data between layers.
 */
@Data
@Builder
public class WorkOrderListResponse {

    private List<WorkOrderDetailsResponse> workOrders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
