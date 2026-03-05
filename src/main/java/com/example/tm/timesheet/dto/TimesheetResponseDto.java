package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TimesheetResponseDto {

    private Long id;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private LocalDate deadlineDate;
    private LocalDate lockDate;
    private String payPeriodStatus;
    private Boolean adminUnlocked;
    private String viewType;
    private Long technicianId;
    private String technicianFirstName;
    private String technicianLastName;
    private String technicianName;
    private BigDecimal totalWorked;
    private BigDecimal totalNonWorked;
    private BigDecimal totalPremium;
    private String status;
    private List<TimesheetDayResponseDto> timesheetDays;
}
