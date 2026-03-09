package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TimesheetRowResponseDto {

    private Long id;
    private String payCode;
    private BigDecimal hours;
    private String accountingUnit;
    private String ferc;
    private String activity;
    private String comment;
    private String entryType;
    private String expenseCode;
    private BigDecimal expenseAmount;
    private Boolean isDeleted;
}
