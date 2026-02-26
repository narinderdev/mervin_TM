package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TimesheetRowResponseDto {

    private Long id;
    private LocalDate date;
    private String dayOfWeek;
    private String payCode;
    private BigDecimal hours;
    private BigDecimal dailyTotal;
    private String department;
    private String account;
    private String project;
    private String comment;
    private Boolean isDeleted;
}
