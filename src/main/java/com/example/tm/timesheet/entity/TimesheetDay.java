package com.example.tm.timesheet.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates timesheet day functionality.
 */
@Getter
@Setter
@Entity
@Table(name = "timesheet_days")
public class TimesheetDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "day_of_week", length = 20)
    private String dayOfWeek;

    @Column(name = "daily_total", precision = 8, scale = 2)
    private BigDecimal dailyTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;

    @OneToMany(mappedBy = "timesheetDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetRow> rows = new ArrayList<>();

    /** Handles add row. */
    public void addRow(TimesheetRow row) {
        row.setTimesheetDay(this);
        rows.add(row);
    }

    /** Handles clear rows. */
    public void clearRows() {
        rows.forEach(r -> r.setTimesheetDay(null));
        rows.clear();
    }
}
