package com.example.tm.timesheet.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "timesheet_drafts")
public class TimesheetDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Column(name = "view_type", length = 50)
    private String viewType;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "total_worked", precision = 8, scale = 2)
    private BigDecimal totalWorked;

    @Column(name = "total_non_worked", precision = 8, scale = 2)
    private BigDecimal totalNonWorked;

    @Column(name = "total_premium", precision = 8, scale = 2)
    private BigDecimal totalPremium;

    @Column(name = "save_as_template")
    private Boolean saveAsTemplate;

    @OneToMany(mappedBy = "timesheetDraft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetDraftDay> timesheetDays = new ArrayList<>();

    public void addDay(TimesheetDraftDay day) {
        day.setTimesheetDraft(this);
        this.timesheetDays.add(day);
    }

    public void clearDays() {
        this.timesheetDays.forEach(day -> day.setTimesheetDraft(null));
        this.timesheetDays.clear();
    }
}
