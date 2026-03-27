package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Transfers timesheet request dto data between layers.
 */
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

    @JsonProperty("technician_id")
    private Long technicianId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("total_worked")
    @JsonAlias("totalWorked")
    private BigDecimal totalWorked;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("total_non_worked")
    @JsonAlias("totalNonWorked")
    private BigDecimal totalNonWorked;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("total_premium")
    @JsonAlias("totalPremium")
    private BigDecimal totalPremium;

    @NotEmpty
    @Valid
    @JsonProperty("timesheet_days")
    private List<TimesheetDayRequestDto> timesheetDays;

    @JsonProperty("save_as_template")
    @JsonAlias("saveAsTemplate")
    private Boolean saveAsTemplate;

    // Returns period start date.
    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    // Sets period start date.
    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    // Returns period end date.
    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    // Sets period end date.
    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEndDate = periodEndDate;
    }

    // Returns view type.
    public String getViewType() {
        return viewType;
    }

    // Sets view type.
    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    // Returns technician id.
    public Long getTechnicianId() {
        return technicianId;
    }

    // Sets technician id.
    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    // Returns total worked.
    public BigDecimal getTotalWorked() {
        return totalWorked;
    }

    // Sets total worked.
    public void setTotalWorked(BigDecimal totalWorked) {
        this.totalWorked = totalWorked;
    }

    // Returns total non worked.
    public BigDecimal getTotalNonWorked() {
        return totalNonWorked;
    }

    // Sets total non worked.
    public void setTotalNonWorked(BigDecimal totalNonWorked) {
        this.totalNonWorked = totalNonWorked;
    }

    // Returns total premium.
    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    // Sets total premium.
    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    // Returns timesheet days.
    public List<TimesheetDayRequestDto> getTimesheetDays() {
        return timesheetDays;
    }

    // Sets timesheet days.
    public void setTimesheetDays(List<TimesheetDayRequestDto> timesheetDays) {
        this.timesheetDays = timesheetDays;
    }

    // Returns save as template.
    public Boolean getSaveAsTemplate() {
        return saveAsTemplate;
    }

    // Sets save as template.
    public void setSaveAsTemplate(Boolean saveAsTemplate) {
        this.saveAsTemplate = saveAsTemplate;
    }
}
