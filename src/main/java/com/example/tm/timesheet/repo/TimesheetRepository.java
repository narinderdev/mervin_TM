package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
}
