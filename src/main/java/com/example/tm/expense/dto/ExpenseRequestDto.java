package com.example.tm.expense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Transfers expense request dto data between layers.
 */
public class ExpenseRequestDto {

    @NotNull
    @JsonProperty("date")
    private LocalDate date;

    @NotBlank
    @JsonProperty("expense_code")
    private String expenseCode;

    @NotBlank
    @JsonProperty("description")
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotNull
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("work_order_id")
    private Long workOrderId;

    @JsonProperty("work_order_name")
    private String workOrderName;

    /** Returns date. */
    public LocalDate getDate() {
        return date;
    }

    /** Sets date. */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /** Returns expense code. */
    public String getExpenseCode() {
        return expenseCode;
    }

    /** Sets expense code. */
    public void setExpenseCode(String expenseCode) {
        this.expenseCode = expenseCode;
    }

    /** Returns description. */
    public String getDescription() {
        return description;
    }

    /** Sets description. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Returns amount. */
    public BigDecimal getAmount() {
        return amount;
    }

    /** Sets amount. */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /** Returns user id. */
    public Long getUserId() {
        return userId;
    }

    /** Sets user id. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** Returns work order id. */
    public Long getWorkOrderId() {
        return workOrderId;
    }

    /** Sets work order id. */
    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    /** Returns work order name. */
    public String getWorkOrderName() {
        return workOrderName;
    }

    /** Sets work order name. */
    public void setWorkOrderName(String workOrderName) {
        this.workOrderName = workOrderName;
    }
}
