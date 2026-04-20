package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.Timesheet;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Defines operations for timesheet repository.
 */
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    List<Timesheet> findByTechnicianId(Long technicianId);

    boolean existsByTechnicianIdAndPeriodStartDateAndPeriodEndDate(Long technicianId,
                                                                   java.time.LocalDate periodStartDate,
                                                                   java.time.LocalDate periodEndDate);

    @Query("""
            SELECT t
            FROM Timesheet t
            WHERE t.technicianId = :technicianId
              AND t.periodStartDate <= :workDate
              AND t.periodEndDate >= :workDate
            ORDER BY t.id DESC
            """)
    List<Timesheet> findByTechnicianAndWorkDate(
            @Param("technicianId") Long technicianId,
            @Param("workDate") LocalDate workDate);
}
