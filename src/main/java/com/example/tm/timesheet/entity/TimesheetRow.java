package com.example.tm.timesheet.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "timesheet_rows")
public class TimesheetRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "day_of_week", length = 20)
    private String dayOfWeek;

    @Column(name = "pay_code", length = 50)
    private String payCode;

    @Column(name = "hours", precision = 8, scale = 2)
    private BigDecimal hours;

    @Column(name = "daily_total", precision = 8, scale = 2)
    private BigDecimal dailyTotal;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "account", length = 100)
    private String account;

    @Column(name = "project", length = 255)
    private String project;

    @Column(length = 2000)
    private String comment;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;
}
