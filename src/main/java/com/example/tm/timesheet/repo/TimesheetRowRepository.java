package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.TimesheetRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetRowRepository extends JpaRepository<TimesheetRow, Long> {
}
