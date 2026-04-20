package com.example.tm.timesheet.repo;

import com.example.tm.timesheet.entity.TimesheetDraft;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Defines operations for timesheet draft repository.
 */
public interface TimesheetDraftRepository extends JpaRepository<TimesheetDraft, Long> {

    Optional<TimesheetDraft> findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
            Long technicianId,
            LocalDate periodStartDate,
            LocalDate periodEndDate);

    Optional<TimesheetDraft> findTopByTechnicianIdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqualOrderByPeriodStartDateDescIdDesc(
            Long technicianId,
            LocalDate periodStartDate,
            LocalDate periodEndDate);

    @Query(
            value = """
                    SELECT TOP 1 td.*
                    FROM timesheet_drafts td
                    JOIN technicians t ON t.id = td.technician_id
                    WHERE td.period_start_date = :periodStartDate
                      AND td.period_end_date = :periodEndDate
                      AND t.is_deleted = 0
                      AND LOWER(LTRIM(RTRIM(t.email))) = LOWER(LTRIM(RTRIM(:email)))
                      AND (:companyId IS NULL OR t.company_id = :companyId)
                    ORDER BY td.id DESC
                    """,
            nativeQuery = true)
    Optional<TimesheetDraft> findByTechnicianEmailAndPeriodStartDateAndPeriodEndDate(
            @Param("email") String email,
            @Param("periodStartDate") LocalDate periodStartDate,
            @Param("periodEndDate") LocalDate periodEndDate,
            @Param("companyId") Long companyId);

    @Query(
            value = """
                    SELECT TOP 1 td.*
                    FROM timesheet_drafts td
                    JOIN technicians t ON t.id = td.technician_id
                    WHERE td.period_start_date <= :periodStartDate
                      AND td.period_end_date >= :periodEndDate
                      AND t.is_deleted = 0
                      AND LOWER(LTRIM(RTRIM(t.email))) = LOWER(LTRIM(RTRIM(:email)))
                      AND (:companyId IS NULL OR t.company_id = :companyId)
                    ORDER BY td.period_start_date DESC, td.id DESC
                    """,
            nativeQuery = true)
    Optional<TimesheetDraft> findCoveringByTechnicianEmailAndPeriod(
            @Param("email") String email,
            @Param("periodStartDate") LocalDate periodStartDate,
            @Param("periodEndDate") LocalDate periodEndDate,
            @Param("companyId") Long companyId);
}
