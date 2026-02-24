package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class TimesheetRequestDto {

    @NotNull
    @JsonProperty("period_start_date")
    private LocalDate periodStartDate;

    @NotNull
    @JsonProperty("period_end_date")
    private LocalDate periodEndDate;

    @NotBlank
    @JsonProperty("view_type")
    private String viewType;

    @NotNull
    @JsonProperty("technician_id")
    private Long technicianId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("total_worked")
    private BigDecimal totalWorked;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("total_non_worked")
    private BigDecimal totalNonWorked;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("total_premium")
    private BigDecimal totalPremium;

    @NotEmpty
    @Valid
    @JsonProperty("timesheet_rows")
    private List<TimesheetRowRequestDto> timesheetRows;

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEndDate = periodEndDate;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public BigDecimal getTotalWorked() {
        return totalWorked;
    }

    public void setTotalWorked(BigDecimal totalWorked) {
        this.totalWorked = totalWorked;
    }

    public BigDecimal getTotalNonWorked() {
        return totalNonWorked;
    }

    public void setTotalNonWorked(BigDecimal totalNonWorked) {
        this.totalNonWorked = totalNonWorked;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public List<TimesheetRowRequestDto> getTimesheetRows() {
        return timesheetRows;
    }

    public void setTimesheetRows(List<TimesheetRowRequestDto> timesheetRows) {
        this.timesheetRows = timesheetRows;
    }
}
