package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.TimesheetRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimesheetRowRepository extends JpaRepository<TimesheetRow, Long> {

    @Query("""
            SELECT r
            FROM TimesheetRow r
            JOIN r.timesheetDay d
            JOIN d.timesheet t
            WHERE t.technicianId = :technicianId
              AND COALESCE(r.isDeleted, false) = false
            ORDER BY r.id DESC
            """)
    List<TimesheetRow> findRecentRowsByTechnicianId(@Param("technicianId") Long technicianId, Pageable pageable);
}
