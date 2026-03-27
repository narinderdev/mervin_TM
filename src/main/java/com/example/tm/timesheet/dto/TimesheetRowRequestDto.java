package com.example.tm.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * Transfers timesheet row request dto data between layers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimesheetRowRequestDto {

    @JsonProperty("pay_code")
    private String payCode;

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

    @JsonProperty("work_order_type")
    private String workOrderType;

    @JsonProperty("expense_code")
    private String expenseCode;

    @JsonProperty("company_number")
    private String companyNumber;

    @jakarta.validation.constraints.NotNull
    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    /** Returns pay code. */
    public String getPayCode() {
        return payCode;
    }

    /** Sets pay code. */
    public void setPayCode(String payCode) {
        this.payCode = payCode;
    }

    /** Returns hours. */
    public BigDecimal getHours() {
        return hours;
    }

    /** Sets hours. */
    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    /** Returns accounting unit. */
    public String getAccountingUnit() {
        return accountingUnit;
    }

    /** Sets accounting unit. */
    public void setAccountingUnit(String accountingUnit) {
        this.accountingUnit = accountingUnit;
    }

    /** Returns ferc. */
    public String getFerc() {
        return ferc;
    }

    /** Sets ferc. */
    public void setFerc(String ferc) {
        this.ferc = ferc;
    }

    /** Returns activity. */
    public String getActivity() {
        return activity;
    }

    /** Sets activity. */
    public void setActivity(String activity) {
        this.activity = activity;
    }

    /** Returns comment. */
    public String getComment() {
        return comment;
    }

    /** Sets comment. */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** Returns work order type. */
    public String getWorkOrderType() {
        return workOrderType;
    }

    /** Sets work order type. */
    public void setWorkOrderType(String workOrderType) {
        this.workOrderType = workOrderType;
    }

    /** Returns expense code. */
    public String getExpenseCode() {
        return expenseCode;
    }

    /** Sets expense code. */
    public void setExpenseCode(String expenseCode) {
        this.expenseCode = expenseCode;
    }

    /** Returns company number. */
    public String getCompanyNumber() {
        return companyNumber;
    }

    /** Sets company number. */
    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    /** Returns is deleted. */
    public Boolean getIsDeleted() {
        return isDeleted;
    }

    /** Sets is deleted. */
    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
}
