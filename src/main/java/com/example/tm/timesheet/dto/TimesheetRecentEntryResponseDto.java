package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Transfers timesheet recent entry response dto data between layers.
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TimesheetRecentEntryResponseDto {

    private Long technicianId;
    private String project;
    private String payCode;
    private String department;
    private String account;
    private BigDecimal totalHours;
}
