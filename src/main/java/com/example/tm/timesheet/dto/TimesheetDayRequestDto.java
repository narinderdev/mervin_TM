package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * Transfers timesheet day request dto data between layers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimesheetDayRequestDto {

    @NotNull
    @JsonProperty("date")
    private LocalDate date;

    @NotBlank
    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("daily_total")
    private BigDecimal dailyTotal;

    @NotEmpty
    @Valid
    @JsonProperty("rows")
    private List<TimesheetRowRequestDto> rows;

    /** Returns date. */
    public LocalDate getDate() {
        return date;
    }

    /** Sets date. */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /** Returns day of week. */
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    /** Sets day of week. */
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /** Returns daily total. */
    public BigDecimal getDailyTotal() {
        return dailyTotal;
    }

    /** Sets daily total. */
    public void setDailyTotal(BigDecimal dailyTotal) {
        this.dailyTotal = dailyTotal;
    }

    /** Returns rows. */
    public List<TimesheetRowRequestDto> getRows() {
        return rows;
    }

    /** Sets rows. */
    public void setRows(List<TimesheetRowRequestDto> rows) {
        this.rows = rows;
    }
}
