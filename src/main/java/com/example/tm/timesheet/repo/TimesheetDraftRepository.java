package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.TimesheetDraft;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetDraftRepository extends JpaRepository<TimesheetDraft, Long> {

    Optional<TimesheetDraft> findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
            Long technicianId,
            LocalDate periodStartDate,
            LocalDate periodEndDate);
}
