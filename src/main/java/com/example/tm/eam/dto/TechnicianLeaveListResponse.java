package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Transfers technician leave list response data between layers.
 */
@Value
@Builder
public class TechnicianLeaveListResponse {

    List<TechnicianLeaveResponse> leaves;
    Integer page;
    Integer size;
    Long totalElements;
    Integer totalPages;
    Boolean last;
}
