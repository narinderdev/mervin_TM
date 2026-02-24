package com.example.tm.timesheet.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
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
@Table(name = "timesheets")
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate periodStartDate;

    private LocalDate periodEndDate;

    private String viewType;

    private Long technicianId;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetRow> timesheetRows = new ArrayList<>();

    public void addRow(TimesheetRow row) {
        row.setTimesheet(this);
        this.timesheetRows.add(row);
    }

    public void clearRows() {
        this.timesheetRows.forEach(row -> row.setTimesheet(null));
        this.timesheetRows.clear();
    }
}
