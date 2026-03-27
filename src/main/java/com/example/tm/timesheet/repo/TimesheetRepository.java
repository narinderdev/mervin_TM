package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Defines operations for timesheet repository.
 */
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    List<Timesheet> findByTechnicianId(Long technicianId);

    boolean existsByTechnicianIdAndPeriodStartDateAndPeriodEndDate(Long technicianId,
                                                                   java.time.LocalDate periodStartDate,
                                                                   java.time.LocalDate periodEndDate);
}
