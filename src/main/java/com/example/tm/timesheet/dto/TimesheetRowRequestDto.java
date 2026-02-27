package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimesheetRowRequestDto {

    @NotBlank
    @JsonProperty("pay_code")
    private String payCode;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("hours")
    private BigDecimal hours;

    @NotBlank
    @JsonProperty("accounting_unit")
    private String accountingUnit;

    @NotBlank
    @JsonProperty("ferc")
    private String ferc;

    @JsonProperty("activity")
    private String activity;

    @JsonProperty("comment")
    private String comment;

    @NotNull
    @JsonProperty("is_deleted")
    private Boolean isDeleted;

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

    public String getAccountingUnit() {
        return accountingUnit;
    }

    public void setAccountingUnit(String accountingUnit) {
        this.accountingUnit = accountingUnit;
    }

    public String getFerc() {
        return ferc;
    }

    public void setFerc(String ferc) {
        this.ferc = ferc;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
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
