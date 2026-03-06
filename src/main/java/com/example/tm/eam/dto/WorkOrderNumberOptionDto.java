package com.example.tm.eam.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderNumberOptionDto {

    private Long id;
    private String workOrderNumber;
}
