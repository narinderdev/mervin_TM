package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Transfers timesheet day response dto data between layers.
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TimesheetDayResponseDto {
    private LocalDate date;
    private String dayOfWeek;
    private BigDecimal dailyTotal;
    private List<TimesheetRowResponseDto> rows;
}
