package com.example.tm.eam.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Transfers work order number option dto data between layers.
 */
@Data
@Builder
public class WorkOrderNumberOptionDto {

    private Long id;
    private String workOrderNumber;
}
