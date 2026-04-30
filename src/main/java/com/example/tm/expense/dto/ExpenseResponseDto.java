package com.example.tm.expense.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Transfers expense response dto data between layers.
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExpenseResponseDto {

    private Long id;
    private LocalDate date;
    private String expenseCode;
    private String description;
    private BigDecimal amount;
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String userName;
    private Long workOrderId;
    private String workOrderName;
    private String workOrderType;
    private String department;
    private String account;
    private String expenseType;
    private String status;
    private Instant submittedAt;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
