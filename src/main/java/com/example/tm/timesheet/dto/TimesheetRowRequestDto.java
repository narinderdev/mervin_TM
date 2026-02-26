package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimesheetRowRequestDto {

    @NotNull
    @JsonProperty("date")
    private LocalDate date;

    @NotBlank
    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @NotBlank
    @JsonProperty("pay_code")
    private String payCode;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("hours")
    private BigDecimal hours;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("daily_total")
    private BigDecimal dailyTotal;

    @NotBlank
    @JsonProperty("department")
    private String department;

    @NotBlank
    @JsonProperty("account")
    private String account;

    @JsonProperty("project")
    private String project;

    @JsonProperty("comment")
    private String comment;

    @NotNull
    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getPayCode() {
        return payCode;
    }

    public void setPayCode(String payCode) {
        this.payCode = payCode;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public BigDecimal getDailyTotal() {
        return dailyTotal;
    }

    public void setDailyTotal(BigDecimal dailyTotal) {
        this.dailyTotal = dailyTotal;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
}
