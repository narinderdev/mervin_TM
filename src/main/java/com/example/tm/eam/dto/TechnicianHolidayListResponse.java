package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Transfers technician holiday list response data between layers.
 */
@Value
@Builder
public class TechnicianHolidayListResponse {

    List<TechnicianHolidayResponse> holidays;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean last;
}
