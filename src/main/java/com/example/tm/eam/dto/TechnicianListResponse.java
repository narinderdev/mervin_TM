package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers technician list response data between layers.
 */
@Data
@Builder
public class TechnicianListResponse {

    private List<TechnicianDetailsResponse> technicians;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
