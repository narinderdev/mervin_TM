package com.example.tm.eam.service;

import com.example.tm.eam.dto.DailyAvailabilityDto;
import com.example.tm.eam.dto.TechnicianCreateRequest;
import com.example.tm.eam.dto.TechnicianActivityDto;
import com.example.tm.eam.dto.TechnicianDashboardResponse;
import com.example.tm.eam.dto.TechnicianDetailsResponse;
import com.example.tm.eam.dto.TechnicianHolidayListResponse;
import com.example.tm.eam.dto.TechnicianHolidayResponse;
import com.example.tm.eam.dto.TechnicianLeaveListResponse;
import com.example.tm.eam.dto.TechnicianLeaveResponse;
import com.example.tm.eam.dto.TechnicianListResponse;
import com.example.tm.eam.dto.TechnicianPatchRequest;
import com.example.tm.eam.dto.TechnicianTeamCreateRequest;
import com.example.tm.eam.dto.TechnicianTeamDetailsResponse;
import com.example.tm.eam.dto.TechnicianTeamListResponse;
import com.example.tm.eam.dto.TechnicianTeamMembershipResponse;
import com.example.tm.eam.dto.TechnicianTeamPatchRequest;
import com.example.tm.eam.dto.TimeWindowDto;
import com.example.tm.eam.dto.WorkOrderDetailsResponse;
import com.example.tm.eam.dto.WorkOrderGlAccountListResponse;
import com.example.tm.eam.dto.WorkOrderListResponse;
import com.example.tm.eam.dto.WorkOrderNumberListResponse;
import com.example.tm.eam.dto.WorkOrderNumberOptionDto;
import com.example.tm.eam.dto.WorkOrderTypeListResponse;
import com.example.tm.eam.dto.WorkRequestTypePropertyUnitListResponse;
import com.example.tm.shared.exception.ResourceNotFoundException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements service logic for eam lookup service impl.
 */
@Service
public class EamLookupServiceImpl implements EamLookupService {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate tmJdbcTemplate;

    /** Creates a new instance of eam lookup service impl. */
    public EamLookupServiceImpl(
            @Qualifier("eamJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("tmJdbcTemplate") JdbcTemplate tmJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.tmJdbcTemplate = tmJdbcTemplate;
    }

    /** Returns dashboard technicians. */
    @Override
    public TechnicianDashboardResponse getDashboardTechnicians(Integer limit, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int resolvedLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 50));
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        long totalTechnicians = queryLongTm("SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ?", safeCompanyId);

        boolean companyScopedHolidays = hasCompanyIdColumn("technician_holidays");
        boolean isHolidayToday = (companyScopedHolidays
                ? queryLong("SELECT COUNT(1) FROM technician_holidays WHERE holiday_date = ? AND company_id = ?", today, safeCompanyId)
                : queryLong("SELECT COUNT(1) FROM technician_holidays WHERE holiday_date = ?", today)) > 0;

        boolean companyScopedLeaves = hasCompanyIdColumn("technician_leaves");
        long onLeave = companyScopedLeaves
                ? queryLong("SELECT COUNT(DISTINCT technician_id) FROM technician_leaves WHERE start_date <= ? AND end_date >= ? AND company_id = ?", today, today, safeCompanyId)
                : queryLong("SELECT COUNT(DISTINCT technician_id) FROM technician_leaves WHERE start_date <= ? AND end_date >= ?", today, today);

        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        long workOrders = companyScopedWorkOrders
                ? queryLong("SELECT COUNT(1) FROM work_orders WHERE deleted = 0 AND company_id = ?", safeCompanyId)
                : queryLong("SELECT COUNT(1) FROM work_orders WHERE deleted = 0");

        String workOrderCompanyFilter = companyScopedWorkOrders ? " AND wo.company_id = ? " : "";
        long busyCount = companyScopedWorkOrders
                ? queryLong("""
                    SELECT COUNT(DISTINCT x.technician_id)
                    FROM (
                        SELECT wo.assigned_technician_id AS technician_id
                        FROM work_orders wo
                        WHERE wo.deleted = 0
                          AND wo.status IN ('SCHEDULED','IN_PROGRESS')
                          AND wo.planned_start_datetime < ?
                          AND wo.planned_end_datetime > ?
                          AND wo.assigned_technician_id IS NOT NULL
                          %s
                        UNION
                        SELECT tm.technician_id AS technician_id
                        FROM work_orders wo
                        INNER JOIN technician_team_members tm ON tm.team_id = wo.assigned_team_id
                        WHERE wo.deleted = 0
                          AND wo.status IN ('SCHEDULED','IN_PROGRESS')
                          AND wo.planned_start_datetime < ?
                          AND wo.planned_end_datetime > ?
                          AND wo.assigned_team_id IS NOT NULL
                          %s
                    ) x
                    """.formatted(workOrderCompanyFilter, workOrderCompanyFilter),
                dayEnd, dayStart, safeCompanyId, dayEnd, dayStart, safeCompanyId)
                : queryLong("""
                    SELECT COUNT(DISTINCT x.technician_id)
                    FROM (
                        SELECT wo.assigned_technician_id AS technician_id
                        FROM work_orders wo
                        WHERE wo.deleted = 0
                          AND wo.status IN ('SCHEDULED','IN_PROGRESS')
                          AND wo.planned_start_datetime < ?
                          AND wo.planned_end_datetime > ?
                          AND wo.assigned_technician_id IS NOT NULL
                        UNION
                        SELECT tm.technician_id AS technician_id
                        FROM work_orders wo
                        INNER JOIN technician_team_members tm ON tm.team_id = wo.assigned_team_id
                        WHERE wo.deleted = 0
                          AND wo.status IN ('SCHEDULED','IN_PROGRESS')
                          AND wo.planned_start_datetime < ?
                          AND wo.planned_end_datetime > ?
                          AND wo.assigned_team_id IS NOT NULL
                    ) x
                    """, dayEnd, dayStart, dayEnd, dayStart);

        List<Map<String, Object>> recentRows = companyScopedWorkOrders
                ? jdbcTemplate.queryForList("""
                    SELECT wo.work_order_id, wo.status, wo.updated_at,
                           t.full_name AS technician_name, tt.team_name AS team_name
                    FROM work_orders wo
                    LEFT JOIN technicians t ON t.id = wo.assigned_technician_id
                    LEFT JOIN technician_teams tt ON tt.id = wo.assigned_team_id
                    WHERE wo.deleted = 0
                      AND wo.company_id = ?
                    ORDER BY wo.updated_at DESC, wo.id DESC
                    OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
                    """, safeCompanyId, resolvedLimit)
                : jdbcTemplate.queryForList("""
                    SELECT wo.work_order_id, wo.status, wo.updated_at,
                           t.full_name AS technician_name, tt.team_name AS team_name
                    FROM work_orders wo
                    LEFT JOIN technicians t ON t.id = wo.assigned_technician_id
                    LEFT JOIN technician_teams tt ON tt.id = wo.assigned_team_id
                    WHERE wo.deleted = 0
                    ORDER BY wo.updated_at DESC, wo.id DESC
                    OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
                    """, resolvedLimit);

        List<TechnicianActivityDto> activities = recentRows.stream().map(this::mapActivity).toList();
        long availableToday = isHolidayToday ? 0 : Math.max(totalTechnicians - busyCount - onLeave, 0);

        return TechnicianDashboardResponse.builder()
                .totalTechnicians(totalTechnicians)
                .availableToday(availableToday)
                .onLeave(onLeave)
                .workOrders(workOrders)
                .recentActivities(activities)
                .build();
    }

    /** Creates technician. */
    @Override
    @Transactional(transactionManager = "tmTransactionManager")
    public TechnicianDetailsResponse createTechnician(TechnicianCreateRequest request) {
        Long companyId = requireCompanyId(request.getCompanyId());
        ensureCompanyExistsInEam(companyId);
        String firstName = requireNonBlank(request.getFirstName(), "First name is required");
        String lastName = requireNonBlank(request.getLastName(), "Last name is required");
        String badgeNumber = requireBadgeUniqueTm(request.getBadgeNumber(), null, companyId);
        String technicianId = determineTechnicianIdTm(request.getTechnicianId(), null, companyId);
        String technicianType = normalizeUpperDefault(request.getTechnicianType(), "FULL_TIME");
        String status = normalizeUpperDefault(request.getStatus(), "ACTIVE");
        String email = safeTrim(request.getEmail());
        if (email != null && existsEmailForOtherTechnicianTm(email, null, companyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician with the same email already exists");
        }

        LocalDate terminationDate = resolveTerminationDate(technicianType, request.getTerminationDate());
        String fullName = resolveFullName(null, firstName, lastName);

        Long technicianPk = tmJdbcTemplate.queryForObject("""
                INSERT INTO technicians (
                    company_id, technician_id, badge_number, first_name, last_name, full_name,
                    technician_type, skills, phone_number, email, address, status,
                    hire_date, work_shift, technician_photo_url, certificate_url,
                    certificate_issue_date, certificate_expiry_date, termination_date,
                    certifications, notes, is_deleted
                )
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                Long.class,
                companyId,
                technicianId,
                badgeNumber,
                firstName,
                lastName,
                fullName,
                technicianType,
                request.getSkills(),
                safeTrim(request.getPhoneNumber()),
                email,
                request.getAddress(),
                status,
                request.getHireDate(),
                safeTrim(request.getWorkShift()),
                safeTrim(request.getTechnicianPhotoUrl()),
                safeTrim(request.getCertificateUrl()),
                request.getCertificateIssueDate(),
                request.getCertificateExpiryDate(),
                terminationDate,
                request.getCertifications(),
                request.getNotes());
        if (technicianPk == null || technicianPk <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create technician");
        }
        return getTechnicianById(technicianPk, companyId);
    }

    /** Returns technician by id. */
    @Override
    public TechnicianDetailsResponse getTechnicianById(Long technicianId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        Map<String, Object> row = getTechnicianRowOrThrowTm(technicianId, safeCompanyId);
        return mapTechnicianTm(row, safeCompanyId);
    }

    /** Handles patch technician. */
    @Override
    public TechnicianDetailsResponse patchTechnician(Long technicianId, Long companyId, TechnicianPatchRequest request) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        Map<String, Object> current = getTechnicianRowOrThrowTm(technicianId, safeCompanyId);

        String firstName = request.getFirstName() == null
                ? asString(current.get("first_name"))
                : requireNonBlank(request.getFirstName(), "First name cannot be blank");
        String lastName = request.getLastName() == null
                ? asString(current.get("last_name"))
                : requireNonBlank(request.getLastName(), "Last name cannot be blank");

        String badgeNumber = request.getBadgeNumber() == null
                ? asString(current.get("badge_number"))
                : requireBadgeUniqueTm(request.getBadgeNumber(), technicianId, safeCompanyId);

        String resolvedTechnicianId;
        if (request.getTechnicianId() == null) {
            resolvedTechnicianId = asString(current.get("technician_id"));
        } else {
            String trimmed = requireNonBlank(request.getTechnicianId(), "technicianId cannot be blank");
            ensureTechnicianIdUniqueTm(trimmed, technicianId, safeCompanyId);
            resolvedTechnicianId = trimmed;
        }

        String technicianType = request.getTechnicianType() == null
                ? asString(current.get("technician_type"))
                : normalizeUpperDefault(request.getTechnicianType(), "FULL_TIME");
        String status = request.getStatus() == null
                ? asString(current.get("status"))
                : normalizeUpperDefault(request.getStatus(), "ACTIVE");

        String email;
        if (request.getEmail() == null) {
            email = asString(current.get("email"));
        } else {
            email = safeTrim(request.getEmail());
            if (email != null && existsEmailForOtherTechnicianTm(email, technicianId, safeCompanyId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician with the same email already exists");
            }
        }

        LocalDate terminationDate = resolveTerminationDate(
                technicianType,
                request.getTerminationDate() == null ? asLocalDate(current.get("termination_date")) : request.getTerminationDate()
        );

        tmJdbcTemplate.update("""
                UPDATE technicians
                SET technician_id = ?, badge_number = ?, first_name = ?, last_name = ?, full_name = ?,
                    technician_type = ?, skills = ?, phone_number = ?, email = ?, address = ?, status = ?,
                    hire_date = ?, work_shift = ?, technician_photo_url = ?, certificate_url = ?,
                    certificate_issue_date = ?, certificate_expiry_date = ?, termination_date = ?,
                    certifications = ?, notes = ?
                WHERE id = ? AND company_id = ? AND is_deleted = 0
                """,
                resolvedTechnicianId,
                badgeNumber,
                firstName,
                lastName,
                resolveFullName(null, firstName, lastName),
                technicianType,
                request.getSkills() == null ? asString(current.get("skills")) : request.getSkills(),
                request.getPhoneNumber() == null ? asString(current.get("phone_number")) : safeTrim(request.getPhoneNumber()),
                email,
                request.getAddress() == null ? asString(current.get("address")) : request.getAddress(),
                status,
                request.getHireDate() == null ? asLocalDate(current.get("hire_date")) : request.getHireDate(),
                request.getWorkShift() == null ? asString(current.get("work_shift")) : safeTrim(request.getWorkShift()),
                request.getTechnicianPhotoUrl() == null ? asString(current.get("technician_photo_url")) : safeTrim(request.getTechnicianPhotoUrl()),
                request.getCertificateUrl() == null ? asString(current.get("certificate_url")) : safeTrim(request.getCertificateUrl()),
                request.getCertificateIssueDate() == null ? asLocalDate(current.get("certificate_issue_date")) : request.getCertificateIssueDate(),
                request.getCertificateExpiryDate() == null ? asLocalDate(current.get("certificate_expiry_date")) : request.getCertificateExpiryDate(),
                terminationDate,
                request.getCertifications() == null ? asString(current.get("certifications")) : request.getCertifications(),
                request.getNotes() == null ? asString(current.get("notes")) : request.getNotes(),
                technicianId,
                safeCompanyId);

        return getTechnicianById(technicianId, safeCompanyId);
    }

    /** Deletes technician. */
    @Override
    public void deleteTechnician(Long technicianId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        ensureTechnicianExistsTm(technicianId, safeCompanyId);
        tmJdbcTemplate.update("DELETE FROM technician_team_members WHERE technician_id = ?", technicianId);
        tmJdbcTemplate.update("UPDATE technicians SET is_deleted = 1 WHERE id = ? AND company_id = ?", technicianId, safeCompanyId);
    }

    /** Returns technicians. */
    @Override
    public TechnicianListResponse getTechnicians(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLongTm("SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ?", safeCompanyId);
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, company_id, technician_id, badge_number, first_name, last_name, full_name,
                       technician_type, skills, phone_number, email, address, status,
                       hire_date, work_shift, technician_photo_url, certificate_url,
                       certificate_issue_date, certificate_expiry_date, termination_date,
                       certifications, notes
                FROM technicians
                WHERE is_deleted = 0 AND company_id = ?
                ORDER BY id
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, safeCompanyId, offset, safeSize);

        List<TechnicianDetailsResponse> technicians = rows.stream().map(row -> mapTechnicianTm(row, safeCompanyId)).toList();
        return TechnicianListResponse.builder()
                .technicians(technicians)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize))
                .last(safePage >= Math.max((int) Math.ceil((double) total / safeSize) - 1, 0))
                .build();
    }

    /** Returns technician availability monthly. */
    @Override
    public List<DailyAvailabilityDto> getTechnicianAvailabilityMonthly(Long technicianId, Integer days, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        ensureTechnicianExistsTm(technicianId, safeCompanyId);
        return buildAvailability(technicianId, days, safeCompanyId);
    }

    /** Creates technician team. */
    @Override
    public TechnicianTeamDetailsResponse createTechnicianTeam(TechnicianTeamCreateRequest request) {
        Long companyId = requireCompanyId(request.getCompanyId());
        ensureCompanyExistsInEam(companyId);
        String teamName = requireNonBlank(request.getTeamName(), "Team name is required");
        ensureTeamNameUniqueTm(teamName, null, companyId);
        String status = normalizeUpperDefault(request.getStatus(), "ACTIVE");

        Long teamId = tmJdbcTemplate.queryForObject("""
                INSERT INTO technician_teams (
                    company_id, team_name, team_description, status, start_date, end_date, notes
                )
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                Long.class,
                companyId,
                teamName,
                request.getTeamDescription(),
                status,
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes());
        if (teamId == null || teamId <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create technician team");
        }
        applyTeamMembershipTm(teamId, companyId, request.getTechnicianIds(), request.getTeamLeaderId());
        return getTechnicianTeamById(teamId, companyId);
    }

    /** Returns technician team by id. */
    @Override
    public TechnicianTeamDetailsResponse getTechnicianTeamById(Long teamId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        Map<String, Object> row = getTeamRowOrThrowTm(teamId, safeCompanyId);
        return mapTeamTm(row, safeCompanyId);
    }

    /** Handles patch technician team. */
    @Override
    public TechnicianTeamDetailsResponse patchTechnicianTeam(Long teamId, Long companyId, TechnicianTeamPatchRequest request) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        Map<String, Object> current = getTeamRowOrThrowTm(teamId, safeCompanyId);

        String teamName = request.getTeamName() == null
                ? asString(current.get("team_name"))
                : requireNonBlank(request.getTeamName(), "Team name cannot be blank");
        if (request.getTeamName() != null) {
            ensureTeamNameUniqueTm(teamName, teamId, safeCompanyId);
        }

        tmJdbcTemplate.update("""
                UPDATE technician_teams
                SET team_name = ?, team_description = ?, status = ?, start_date = ?, end_date = ?, notes = ?
                WHERE id = ? AND company_id = ?
                """,
                teamName,
                request.getTeamDescription() == null ? asString(current.get("team_description")) : request.getTeamDescription(),
                request.getStatus() == null ? asString(current.get("status")) : normalizeUpperDefault(request.getStatus(), "ACTIVE"),
                request.getStartDate() == null ? asLocalDate(current.get("start_date")) : request.getStartDate(),
                request.getEndDate() == null ? asLocalDate(current.get("end_date")) : request.getEndDate(),
                request.getNotes() == null ? asString(current.get("notes")) : request.getNotes(),
                teamId,
                safeCompanyId);

        applyTeamMembershipTm(teamId, safeCompanyId, request.getTechnicianIds(), request.getTeamLeaderId());
        return getTechnicianTeamById(teamId, safeCompanyId);
    }

    /** Deletes technician team. */
    @Override
    public void deleteTechnicianTeam(Long teamId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        getTeamRowOrThrowTm(teamId, safeCompanyId);
        long membersCount = queryLongTm("SELECT COUNT(1) FROM technician_team_members WHERE team_id = ?", teamId);
        if (membersCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete team with assigned technicians");
        }
        tmJdbcTemplate.update("DELETE FROM technician_teams WHERE id = ? AND company_id = ?", teamId, safeCompanyId);
    }

    /** Returns technician teams. */
    @Override
    public TechnicianTeamListResponse getTechnicianTeams(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLongTm("SELECT COUNT(1) FROM technician_teams WHERE company_id = ?", safeCompanyId);
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, company_id, team_name, team_description, status, start_date, end_date, notes
                FROM technician_teams
                WHERE company_id = ?
                ORDER BY id
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, safeCompanyId, offset, safeSize);
        List<TechnicianTeamDetailsResponse> teams = rows.stream().map(row -> mapTeamTm(row, safeCompanyId)).toList();
        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return TechnicianTeamListResponse.builder()
                .teams(teams)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns work order numbers. */
    @Override
    public WorkOrderNumberListResponse getWorkOrderNumbers(int page, int size, Long technicianId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        String companyFilter = companyScopedWorkOrders ? " AND wo.company_id = ? " : "";
        long total = companyScopedWorkOrders
                ? queryLong("""
                    SELECT COUNT(DISTINCT wo.work_order_number)
                    FROM work_orders wo
                    WHERE wo.deleted = 0
                      AND wo.work_order_number IS NOT NULL
                      AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                      %s
                    """.formatted(companyFilter), safeCompanyId)
                : queryLong("""
                    SELECT COUNT(DISTINCT wo.work_order_number)
                    FROM work_orders wo
                    WHERE wo.deleted = 0
                      AND wo.work_order_number IS NOT NULL
                      AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                    """);

        List<WorkOrderNumberOptionDto> workOrderNumbers = companyScopedWorkOrders
                ? jdbcTemplate.query("""
                    SELECT x.id, x.work_order_number
                    FROM (
                        SELECT MIN(wo.id) AS id, LTRIM(RTRIM(wo.work_order_number)) AS work_order_number
                        FROM work_orders wo
                        WHERE wo.deleted = 0
                          AND wo.work_order_number IS NOT NULL
                          AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                          AND wo.company_id = ?
                        GROUP BY LTRIM(RTRIM(wo.work_order_number))
                    ) x
                    ORDER BY x.work_order_number ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """,
                (rs, rowNum) -> WorkOrderNumberOptionDto.builder()
                        .id(rs.getLong("id"))
                        .workOrderNumber(rs.getString("work_order_number"))
                        .build(),
                safeCompanyId,
                offset,
                safeSize)
                : jdbcTemplate.query("""
                    SELECT x.id, x.work_order_number
                    FROM (
                        SELECT MIN(wo.id) AS id, LTRIM(RTRIM(wo.work_order_number)) AS work_order_number
                        FROM work_orders wo
                        WHERE wo.deleted = 0
                          AND wo.work_order_number IS NOT NULL
                          AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                        GROUP BY LTRIM(RTRIM(wo.work_order_number))
                    ) x
                    ORDER BY x.work_order_number ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """,
                (rs, rowNum) -> WorkOrderNumberOptionDto.builder()
                        .id(rs.getLong("id"))
                        .workOrderNumber(rs.getString("work_order_number"))
                        .build(),
                offset,
                safeSize);

        List<WorkOrderNumberOptionDto> favouriteWorkOrderNumbers = getFavouriteWorkOrderNumbers(technicianId, false, safeCompanyId);
        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return WorkOrderNumberListResponse.builder()
                .workOrderNumbers(workOrderNumbers)
                .favouriteWorkOrderNumbers(favouriteWorkOrderNumbers)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns capex work order numbers. */
    @Override
    public WorkOrderNumberListResponse getCapexWorkOrderNumbers(int page, int size, Long technicianId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        if (!supportsCapexWorkOrderFilter()) {
            return WorkOrderNumberListResponse.builder()
                    .workOrderNumbers(List.of())
                    .favouriteWorkOrderNumbers(List.of())
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        String workOrderCompanyFilter = companyScopedWorkOrders ? " AND wo.company_id = ? " : "";
        long total = companyScopedWorkOrders
                ? queryLong("""
                    SELECT COUNT(DISTINCT LTRIM(RTRIM(wo.work_order_number)))
                    FROM work_orders wo
                    INNER JOIN work_order_types wot ON wot.id = wo.work_order_type_id
                    WHERE wo.deleted = 0
                      AND wo.work_order_number IS NOT NULL
                      AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                      AND UPPER(LTRIM(RTRIM(wot.cost_treatment))) IN ('CAPEX', 'CAPITAL')
                      %s
                    """.formatted(workOrderCompanyFilter), safeCompanyId)
                : queryLong("""
                    SELECT COUNT(DISTINCT LTRIM(RTRIM(wo.work_order_number)))
                    FROM work_orders wo
                    INNER JOIN work_order_types wot ON wot.id = wo.work_order_type_id
                    WHERE wo.deleted = 0
                      AND wo.work_order_number IS NOT NULL
                      AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                      AND UPPER(LTRIM(RTRIM(wot.cost_treatment))) IN ('CAPEX', 'CAPITAL')
                    """);

        List<WorkOrderNumberOptionDto> workOrderNumbers = companyScopedWorkOrders
                ? jdbcTemplate.query("""
                    SELECT x.id, x.work_order_number
                    FROM (
                        SELECT MIN(wo.id) AS id, LTRIM(RTRIM(wo.work_order_number)) AS work_order_number
                        FROM work_orders wo
                        INNER JOIN work_order_types wot ON wot.id = wo.work_order_type_id
                        WHERE wo.deleted = 0
                          AND wo.work_order_number IS NOT NULL
                          AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                          AND UPPER(LTRIM(RTRIM(wot.cost_treatment))) IN ('CAPEX', 'CAPITAL')
                          AND wo.company_id = ?
                        GROUP BY LTRIM(RTRIM(wo.work_order_number))
                    ) x
                    ORDER BY x.work_order_number ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """,
                (rs, rowNum) -> WorkOrderNumberOptionDto.builder()
                        .id(rs.getLong("id"))
                        .workOrderNumber(rs.getString("work_order_number"))
                        .build(),
                safeCompanyId,
                offset,
                safeSize)
                : jdbcTemplate.query("""
                    SELECT x.id, x.work_order_number
                    FROM (
                        SELECT MIN(wo.id) AS id, LTRIM(RTRIM(wo.work_order_number)) AS work_order_number
                        FROM work_orders wo
                        INNER JOIN work_order_types wot ON wot.id = wo.work_order_type_id
                        WHERE wo.deleted = 0
                          AND wo.work_order_number IS NOT NULL
                          AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                          AND UPPER(LTRIM(RTRIM(wot.cost_treatment))) IN ('CAPEX', 'CAPITAL')
                        GROUP BY LTRIM(RTRIM(wo.work_order_number))
                    ) x
                    ORDER BY x.work_order_number ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """,
                (rs, rowNum) -> WorkOrderNumberOptionDto.builder()
                        .id(rs.getLong("id"))
                        .workOrderNumber(rs.getString("work_order_number"))
                        .build(),
                offset,
                safeSize);

        List<WorkOrderNumberOptionDto> favouriteWorkOrderNumbers = getFavouriteWorkOrderNumbers(technicianId, true, safeCompanyId);
        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return WorkOrderNumberListResponse.builder()
                .workOrderNumbers(workOrderNumbers)
                .favouriteWorkOrderNumbers(favouriteWorkOrderNumbers)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns work order gl accounts. */
    @Override
    public WorkOrderGlAccountListResponse getWorkOrderGlAccounts(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        String companyFilter = companyScopedWorkOrders ? " AND wo.company_id = ? " : "";
        long total = companyScopedWorkOrders
                ? queryLong("""
                    SELECT COUNT(DISTINCT LTRIM(RTRIM(wo.gl_account)))
                    FROM work_orders wo
                    WHERE wo.deleted = 0
                      AND wo.gl_account IS NOT NULL
                      AND LTRIM(RTRIM(wo.gl_account)) <> ''
                      %s
                    """.formatted(companyFilter), safeCompanyId)
                : queryLong("""
                    SELECT COUNT(DISTINCT LTRIM(RTRIM(wo.gl_account)))
                    FROM work_orders wo
                    WHERE wo.deleted = 0
                      AND wo.gl_account IS NOT NULL
                      AND LTRIM(RTRIM(wo.gl_account)) <> ''
                    """);
        List<String> glAccounts = companyScopedWorkOrders
                ? jdbcTemplate.queryForList("""
                    SELECT DISTINCT LTRIM(RTRIM(wo.gl_account)) AS gl_account
                    FROM work_orders wo
                    WHERE wo.deleted = 0
                      AND wo.gl_account IS NOT NULL
                      AND LTRIM(RTRIM(wo.gl_account)) <> ''
                      AND wo.company_id = ?
                    ORDER BY gl_account ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, String.class, safeCompanyId, offset, safeSize)
                : jdbcTemplate.queryForList("""
                    SELECT DISTINCT LTRIM(RTRIM(wo.gl_account)) AS gl_account
                    FROM work_orders wo
                    WHERE wo.deleted = 0
                      AND wo.gl_account IS NOT NULL
                      AND LTRIM(RTRIM(wo.gl_account)) <> ''
                    ORDER BY gl_account ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, String.class, offset, safeSize);

        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return WorkOrderGlAccountListResponse.builder()
                .glAccounts(glAccounts)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns work order types. */
    @Override
    public WorkOrderTypeListResponse getWorkOrderTypes(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        if (!tableExists("work_order_types")) {
            return WorkOrderTypeListResponse.builder()
                    .workOrderTypes(List.of())
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        List<String> columns = resolveTableColumns("work_order_types");
        if (columns.isEmpty()) {
            return WorkOrderTypeListResponse.builder()
                    .workOrderTypes(List.of())
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        String quotedTable = quoteIdentifier("work_order_types");
        String selectedColumns = String.join(", ", columns.stream()
                .map(this::quoteIdentifier)
                .map(column -> "wot." + column)
                .toList());
        String orderColumn = resolvePreferredOrderColumn(columns);
        boolean companyScopedWorkOrderTypes = columns.stream().anyMatch(column -> "company_id".equalsIgnoreCase(column));
        String companyFilter = companyScopedWorkOrderTypes ? "WHERE wot.company_id = ?" : "";
        String listSql = """
                SELECT %s
                FROM %s wot
                %s
                ORDER BY wot.%s ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.formatted(selectedColumns, quotedTable, companyFilter, quoteIdentifier(orderColumn));

        long total = companyScopedWorkOrderTypes
                ? queryLong("SELECT COUNT(1) FROM " + quotedTable + " WHERE company_id = ?", safeCompanyId)
                : queryLong("SELECT COUNT(1) FROM " + quotedTable);
        List<Map<String, Object>> workOrderTypes = companyScopedWorkOrderTypes
                ? jdbcTemplate.queryForList(listSql, safeCompanyId, offset, safeSize)
                : jdbcTemplate.queryForList(listSql, offset, safeSize);

        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return WorkOrderTypeListResponse.builder()
                .workOrderTypes(workOrderTypes)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Resolves table columns. */
    private List<String> resolveTableColumns(String tableName) {
        return jdbcTemplate.query("""
                        SELECT c.COLUMN_NAME
                        FROM INFORMATION_SCHEMA.COLUMNS c
                        WHERE c.TABLE_NAME = ?
                        ORDER BY c.ORDINAL_POSITION
                        """,
                (rs, rowNum) -> rs.getString("COLUMN_NAME"),
                tableName);
    }

    /** Resolves preferred order column. */
    private String resolvePreferredOrderColumn(List<String> columns) {
        List<String> preferred = List.of("id", "work_order_type_id", "code", "name", "description");
        for (String candidate : preferred) {
            for (String column : columns) {
                if (candidate.equalsIgnoreCase(column)) {
                    return column;
                }
            }
        }
        return columns.get(0);
    }

    /** Handles quote identifier. */
    private String quoteIdentifier(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    /** Returns work request type property units. */
    @Override
    public WorkRequestTypePropertyUnitListResponse getWorkRequestTypePropertyUnits(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        PropertyUnitSource propertyUnitSource = resolveWorkRequestTypePropertyUnitSource();
        if (propertyUnitSource == null) {
            return WorkRequestTypePropertyUnitListResponse.builder()
                    .propertyUnits(List.of())
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        String quotedTable = "[" + propertyUnitSource.tableName().replace("]", "]]") + "]";
        String quotedColumn = "[" + propertyUnitSource.columnName().replace("]", "]]") + "]";
        boolean companyScopedSource = columnExists(propertyUnitSource.tableName(), "company_id");
        String companyFilter = companyScopedSource ? " AND src.[company_id] = ? " : "";
        String totalSql = """
                SELECT COUNT(DISTINCT LTRIM(RTRIM(src.%s)))
                FROM %s src
                WHERE src.%s IS NOT NULL
                  AND LTRIM(RTRIM(src.%s)) <> ''
                  %s
                """.formatted(quotedColumn, quotedTable, quotedColumn, quotedColumn, companyFilter);
        String listSql = """
                SELECT DISTINCT LTRIM(RTRIM(src.%s)) AS property_unit
                FROM %s src
                WHERE src.%s IS NOT NULL
                  AND LTRIM(RTRIM(src.%s)) <> ''
                  %s
                ORDER BY property_unit ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.formatted(quotedColumn, quotedTable, quotedColumn, quotedColumn, companyFilter);

        long total = companyScopedSource ? queryLong(totalSql, safeCompanyId) : queryLong(totalSql);
        List<String> propertyUnits = companyScopedSource
                ? jdbcTemplate.queryForList(listSql, String.class, safeCompanyId, offset, safeSize)
                : jdbcTemplate.queryForList(listSql, String.class, offset, safeSize);

        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return WorkRequestTypePropertyUnitListResponse.builder()
                .propertyUnits(propertyUnits)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Resolves work request type property unit source. */
    private PropertyUnitSource resolveWorkRequestTypePropertyUnitSource() {
        List<String> preferredTables = List.of(
                "work_request_types",
                "work_order_types"
        );
        List<String> preferred = List.of(
                "property_unit",
                "propertyunit",
                "unit",
                "accounting_unit"
        );

        for (String table : preferredTables) {
            if (!tableExists(table)) {
                continue;
            }
            for (String candidate : preferred) {
                if (columnExists(table, candidate)) {
                    return new PropertyUnitSource(table, candidate);
                }
            }
        }

        List<Map<String, Object>> guessed = jdbcTemplate.queryForList("""
                SELECT TOP 1
                    c.TABLE_NAME AS table_name,
                    c.COLUMN_NAME AS column_name
                FROM INFORMATION_SCHEMA.COLUMNS c
                WHERE c.TABLE_NAME IN ('work_request_types', 'work_order_types')
                  AND (
                      LOWER(c.COLUMN_NAME) LIKE '%property%unit%'
                      OR LOWER(c.COLUMN_NAME) LIKE '%unit%'
                      OR LOWER(c.COLUMN_NAME) LIKE '%property%'
                  )
                ORDER BY
                  CASE
                      WHEN LOWER(c.TABLE_NAME) = 'work_request_types' THEN 0
                      WHEN LOWER(c.TABLE_NAME) = 'work_order_types' THEN 1
                      ELSE 2
                  END,
                  CASE
                      WHEN LOWER(c.COLUMN_NAME) = 'property_unit' THEN 0
                      WHEN LOWER(c.COLUMN_NAME) LIKE '%property%unit%' THEN 1
                      WHEN LOWER(c.COLUMN_NAME) LIKE '%unit%' THEN 2
                      ELSE 3
                  END,
                  c.ORDINAL_POSITION
                """);
        if (guessed.isEmpty()) {
            return null;
        }
        Map<String, Object> row = guessed.get(0);
        return new PropertyUnitSource(asString(row.get("table_name")), asString(row.get("column_name")));
    }

    /** Returns work orders. */
    @Override
    public WorkOrderListResponse getWorkOrders(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        long total = companyScopedWorkOrders
                ? queryLong("SELECT COUNT(1) FROM work_orders WHERE deleted = 0 AND company_id = ?", safeCompanyId)
                : queryLong("SELECT COUNT(1) FROM work_orders WHERE deleted = 0");
        List<Map<String, Object>> rows = companyScopedWorkOrders
                ? jdbcTemplate.queryForList(workOrderSelect() + """
                    WHERE wo.deleted = 0
                      AND wo.company_id = ?
                    ORDER BY wo.planned_end_datetime ASC, wo.id ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, safeCompanyId, offset, safeSize)
                : jdbcTemplate.queryForList(workOrderSelect() + """
                    WHERE wo.deleted = 0
                    ORDER BY wo.planned_end_datetime ASC, wo.id ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, offset, safeSize);
        List<WorkOrderDetailsResponse> workOrders = rows.stream().map(this::mapWorkOrder).toList();
        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return WorkOrderListResponse.builder()
                .workOrders(workOrders)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns work order by id. */
    @Override
    public WorkOrderDetailsResponse getWorkOrderById(Long workOrderId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        List<Map<String, Object>> rows = companyScopedWorkOrders
                ? jdbcTemplate.queryForList(workOrderSelect() + " WHERE wo.deleted = 0 AND wo.id = ? AND wo.company_id = ?", workOrderId, safeCompanyId)
                : jdbcTemplate.queryForList(workOrderSelect() + " WHERE wo.deleted = 0 AND wo.id = ?", workOrderId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Work order not found: " + workOrderId);
        }
        return mapWorkOrder(rows.get(0));
    }

    /** Handles add work order to favourites. */
    @Override
    public WorkOrderDetailsResponse addWorkOrderToFavourites(Long technicianId, Long workOrderId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        ensureTechnicianExistsTm(technicianId, safeCompanyId);
        WorkOrderDetailsResponse workOrder = getWorkOrderById(workOrderId, safeCompanyId);
        boolean companyScopedFavourites = hasTmCompanyIdColumn("work_order_favourites");
        if (companyScopedFavourites) {
            tmJdbcTemplate.update("""
                    IF NOT EXISTS (
                        SELECT 1
                        FROM work_order_favourites
                        WHERE technician_id = ? AND work_order_id = ? AND company_id = ?
                    )
                    BEGIN
                        INSERT INTO work_order_favourites (technician_id, work_order_id, company_id)
                        VALUES (?, ?, ?)
                    END
                    """, technicianId, workOrderId, safeCompanyId, technicianId, workOrderId, safeCompanyId);
        } else {
            tmJdbcTemplate.update("""
                    IF NOT EXISTS (
                        SELECT 1
                        FROM work_order_favourites
                        WHERE technician_id = ? AND work_order_id = ?
                    )
                    BEGIN
                        INSERT INTO work_order_favourites (technician_id, work_order_id)
                        VALUES (?, ?)
                    END
                    """, technicianId, workOrderId, technicianId, workOrderId);
        }

        return workOrder;
    }

    /** Returns favourite work order numbers. */
    private List<WorkOrderNumberOptionDto> getFavouriteWorkOrderNumbers(Long technicianId, boolean capexOnly, Long companyId) {
        if (technicianId == null) {
            return List.of();
        }
        if (capexOnly && !supportsCapexWorkOrderFilter()) {
            return List.of();
        }

        ensureTechnicianExistsTm(technicianId, companyId);
        boolean companyScopedFavourites = hasTmCompanyIdColumn("work_order_favourites");
        List<Long> workOrderIds = companyScopedFavourites
                ? tmJdbcTemplate.query("""
                            SELECT f.work_order_id
                            FROM work_order_favourites f
                            JOIN technicians t ON t.id = f.technician_id
                            WHERE f.technician_id = ?
                              AND t.is_deleted = 0
                              AND t.company_id = ?
                              AND f.company_id = ?
                            ORDER BY f.created_at DESC, f.id DESC
                            """,
                    (rs, rowNum) -> rs.getLong("work_order_id"),
                    technicianId,
                    companyId,
                    companyId)
                : tmJdbcTemplate.query("""
                            SELECT f.work_order_id
                            FROM work_order_favourites f
                            JOIN technicians t ON t.id = f.technician_id
                            WHERE f.technician_id = ?
                              AND t.is_deleted = 0
                              AND t.company_id = ?
                            ORDER BY f.created_at DESC, f.id DESC
                            """,
                    (rs, rowNum) -> rs.getLong("work_order_id"),
                    technicianId,
                    companyId);
        if (workOrderIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(workOrderIds.size(), "?"));
        String capexJoin = capexOnly ? "INNER JOIN work_order_types wot ON wot.id = wo.work_order_type_id" : "";
        String capexFilter = capexOnly ? "AND UPPER(LTRIM(RTRIM(wot.cost_treatment))) IN ('CAPEX', 'CAPITAL')" : "";
        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        String companyFilter = companyScopedWorkOrders ? "AND wo.company_id = ? " : "";
        String sql = """
                SELECT wo.id, LTRIM(RTRIM(wo.work_order_number)) AS work_order_number
                FROM work_orders wo
                %s
                WHERE wo.deleted = 0
                  AND wo.id IN (%s)
                  AND wo.work_order_number IS NOT NULL
                  AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                  %s
                  %s
                """.formatted(capexJoin, placeholders, capexFilter, companyFilter);
        Object[] args = companyScopedWorkOrders
                ? appendArg(workOrderIds.toArray(), companyId)
                : workOrderIds.toArray();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);

        Map<Long, String> numberById = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = asLong(row.get("id"));
            String number = asString(row.get("work_order_number"));
            if (id != null && number != null && !number.isBlank()) {
                numberById.put(id, number);
            }
        }

        Set<Long> orderedUniqueIds = new LinkedHashSet<>();
        for (Long workOrderId : workOrderIds) {
            if (numberById.containsKey(workOrderId)) {
                orderedUniqueIds.add(workOrderId);
            }
        }
        return orderedUniqueIds.stream()
                .map(id -> WorkOrderNumberOptionDto.builder()
                        .id(id)
                        .workOrderNumber(numberById.get(id))
                        .build())
                .toList();
    }

    /** Handles supports capex work order filter. */
    private boolean supportsCapexWorkOrderFilter() {
        return tableExists("work_order_types")
                && columnExists("work_order_types", "cost_treatment")
                && columnExists("work_orders", "work_order_type_id");
    }

    /** Returns holidays. */
    @Override
    public TechnicianHolidayListResponse getHolidays(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        boolean companyScopedHolidays = hasCompanyIdColumn("technician_holidays");
        long total = companyScopedHolidays
                ? queryLong("SELECT COUNT(1) FROM technician_holidays WHERE company_id = ?", safeCompanyId)
                : queryLong("SELECT COUNT(1) FROM technician_holidays");
        List<Map<String, Object>> rows = companyScopedHolidays
                ? jdbcTemplate.queryForList("""
                    SELECT id, holiday_name, holiday_type, holiday_date, notes, created_at
                    FROM technician_holidays
                    WHERE company_id = ?
                    ORDER BY holiday_date ASC, id ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, safeCompanyId, offset, safeSize)
                : jdbcTemplate.queryForList("""
                    SELECT id, holiday_name, holiday_type, holiday_date, notes, created_at
                    FROM technician_holidays
                    ORDER BY holiday_date ASC, id ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, offset, safeSize);
        List<TechnicianHolidayResponse> holidays = rows.stream().map(this::mapHoliday).toList();
        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return TechnicianHolidayListResponse.builder()
                .holidays(holidays)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns holiday by id. */
    @Override
    public TechnicianHolidayResponse getHolidayById(Long holidayId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        boolean companyScopedHolidays = hasCompanyIdColumn("technician_holidays");
        List<Map<String, Object>> rows = companyScopedHolidays
                ? jdbcTemplate.queryForList("""
                    SELECT id, holiday_name, holiday_type, holiday_date, notes, created_at
                    FROM technician_holidays
                    WHERE id = ? AND company_id = ?
                    """, holidayId, safeCompanyId)
                : jdbcTemplate.queryForList("""
                    SELECT id, holiday_name, holiday_type, holiday_date, notes, created_at
                    FROM technician_holidays
                    WHERE id = ?
                    """, holidayId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Holiday not found: " + holidayId);
        }
        return mapHoliday(rows.get(0));
    }

    /** Returns technicians leaves. */
    @Override
    public TechnicianLeaveListResponse getTechniciansLeaves(int page, int size, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        boolean companyScopedLeaves = hasCompanyIdColumn("technician_leaves");
        long total = companyScopedLeaves
                ? queryLong("SELECT COUNT(1) FROM technician_leaves WHERE company_id = ?", safeCompanyId)
                : queryLong("SELECT COUNT(1) FROM technician_leaves");
        List<Map<String, Object>> rows = companyScopedLeaves
                ? jdbcTemplate.queryForList(leavesSelect() + """
                    WHERE l.company_id = ?
                    ORDER BY l.start_date ASC, l.id ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, safeCompanyId, offset, safeSize)
                : jdbcTemplate.queryForList(leavesSelect() + """
                    ORDER BY l.start_date ASC, l.id ASC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, offset, safeSize);
        List<TechnicianLeaveResponse> leaves = rows.stream().map(this::mapLeave).toList();
        int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return TechnicianLeaveListResponse.builder()
                .leaves(leaves)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= Math.max(totalPages - 1, 0))
                .build();
    }

    /** Returns technician leaves. */
    @Override
    public TechnicianLeaveListResponse getTechnicianLeaves(Long technicianId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        ensureTechnicianExistsTm(technicianId, safeCompanyId);
        boolean companyScopedLeaves = hasCompanyIdColumn("technician_leaves");
        List<Map<String, Object>> rows = companyScopedLeaves
                ? jdbcTemplate.queryForList(leavesSelect() + " WHERE l.technician_id = ? AND l.company_id = ? ORDER BY l.start_date ASC, l.id ASC", technicianId, safeCompanyId)
                : jdbcTemplate.queryForList(leavesSelect() + " WHERE l.technician_id = ? ORDER BY l.start_date ASC, l.id ASC", technicianId);
        List<TechnicianLeaveResponse> leaves = rows.stream().map(this::mapLeave).toList();
        return TechnicianLeaveListResponse.builder()
                .leaves(leaves)
                .page(0)
                .size(leaves.size())
                .totalElements((long) leaves.size())
                .totalPages(1)
                .last(true)
                .build();
    }

    /** Returns technician leave by id. */
    @Override
    public TechnicianLeaveResponse getTechnicianLeaveById(Long technicianId, Long leaveId, Long companyId) {
        Long safeCompanyId = requireCompanyId(companyId);
        ensureCompanyExistsInEam(safeCompanyId);
        ensureTechnicianExistsTm(technicianId, safeCompanyId);
        boolean companyScopedLeaves = hasCompanyIdColumn("technician_leaves");
        List<Map<String, Object>> rows = companyScopedLeaves
                ? jdbcTemplate.queryForList(leavesSelect() + " WHERE l.technician_id = ? AND l.id = ? AND l.company_id = ?", technicianId, leaveId, safeCompanyId)
                : jdbcTemplate.queryForList(leavesSelect() + " WHERE l.technician_id = ? AND l.id = ?", technicianId, leaveId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Leave not found: " + leaveId);
        }
        return mapLeave(rows.get(0));
    }

    /** Returns technician row or throw. */
    private Map<String, Object> getTechnicianRowOrThrow(Long technicianId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, technician_id, badge_number, first_name, last_name, full_name,
                       technician_type, skills, phone_number, email, address, status,
                       hire_date, work_shift, technician_photo_url, certificate_url,
                       certificate_issue_date, certificate_expiry_date, termination_date,
                       certifications, notes
                FROM technicians
                WHERE id = ? AND is_deleted = 0
                """, technicianId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
        return rows.get(0);
    }

    /** Returns team row or throw. */
    private Map<String, Object> getTeamRowOrThrow(Long teamId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, team_name, team_description, status, start_date, end_date, notes
                FROM technician_teams
                WHERE id = ?
                """, teamId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Technician team not found: " + teamId);
        }
        return rows.get(0);
    }

    /** Handles ensure team name unique. */
    private void ensureTeamNameUnique(String teamName, Long currentTeamId) {
        String sql = currentTeamId == null
                ? "SELECT COUNT(1) FROM technician_teams WHERE LOWER(team_name) = LOWER(?)"
                : "SELECT COUNT(1) FROM technician_teams WHERE LOWER(team_name) = LOWER(?) AND id <> ?";
        long count = currentTeamId == null
                ? queryLong(sql, teamName)
                : queryLong(sql, teamName, currentTeamId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician team with the same name already exists");
        }
    }

    /** Applies team membership. */
    private void applyTeamMembership(Long teamId, List<Long> technicianIds, Long requestedLeaderId) {
        boolean replaceMembership = technicianIds != null;
        if (!replaceMembership && requestedLeaderId == null) {
            return;
        }

        List<Map<String, Object>> currentMembers = jdbcTemplate.queryForList("""
                SELECT technician_id, team_leader
                FROM technician_team_members
                WHERE team_id = ?
                """, teamId);

        Set<Long> desiredIds;
        if (replaceMembership) {
            if (technicianIds.stream().anyMatch(Objects::isNull)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician IDs cannot be null");
            }
            desiredIds = new LinkedHashSet<>(technicianIds);
        } else {
            desiredIds = currentMembers.stream()
                    .map(member -> asLong(member.get("technician_id")))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }

        if (requestedLeaderId != null && !desiredIds.contains(requestedLeaderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team leader must be part of the assigned technicians");
        }

        if (replaceMembership) {
            syncTeamTechnicians(teamId, desiredIds, requestedLeaderId, currentMembers);
        } else {
            updateLeaderOnly(currentMembers, teamId, requestedLeaderId);
        }
    }

    /** Synchronizes team technicians. */
    private void syncTeamTechnicians(Long teamId,
                                     Set<Long> desiredIds,
                                     Long requestedLeaderId,
                                     List<Map<String, Object>> currentMembers) {
        if (desiredIds.isEmpty()) {
            if (!currentMembers.isEmpty()) {
                jdbcTemplate.update("DELETE FROM technician_team_members WHERE team_id = ?", teamId);
            }
            return;
        }

        Set<Long> existingTechnicians = fetchExistingTechnicianIds(desiredIds);
        if (existingTechnicians.size() != desiredIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more technicians were not found");
        }

        Map<Long, Boolean> currentMap = new HashMap<>();
        for (Map<String, Object> member : currentMembers) {
            currentMap.put(asLong(member.get("technician_id")), truthy(member.get("team_leader")));
        }

        Long leaderId = resolveLeaderId(requestedLeaderId, desiredIds, currentMap);

        for (Long currentTechnicianId : currentMap.keySet()) {
            if (!desiredIds.contains(currentTechnicianId)) {
                jdbcTemplate.update(
                        "DELETE FROM technician_team_members WHERE team_id = ? AND technician_id = ?",
                        teamId,
                        currentTechnicianId
                );
            }
        }

        for (Long technicianId : desiredIds) {
            boolean isLeader = leaderId != null && leaderId.equals(technicianId);
            if (currentMap.containsKey(technicianId)) {
                jdbcTemplate.update(
                        "UPDATE technician_team_members SET team_leader = ? WHERE team_id = ? AND technician_id = ?",
                        isLeader,
                        teamId,
                        technicianId
                );
            } else {
                jdbcTemplate.update(
                        "INSERT INTO technician_team_members (team_id, technician_id, team_leader) VALUES (?, ?, ?)",
                        teamId,
                        technicianId,
                        isLeader
                );
            }
        }
    }

    /** Updates leader only. */
    private void updateLeaderOnly(List<Map<String, Object>> currentMembers, Long teamId, Long leaderId) {
        if (leaderId == null) {
            return;
        }
        if (currentMembers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team has no technicians to assign as leader");
        }
        boolean found = currentMembers.stream()
                .map(member -> asLong(member.get("technician_id")))
                .anyMatch(leaderId::equals);
        if (!found) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team leader must be part of the team");
        }
        for (Map<String, Object> member : currentMembers) {
            Long technicianId = asLong(member.get("technician_id"));
            jdbcTemplate.update(
                    "UPDATE technician_team_members SET team_leader = ? WHERE team_id = ? AND technician_id = ?",
                    leaderId.equals(technicianId),
                    teamId,
                    technicianId
            );
        }
    }

    /** Resolves leader id. */
    private Long resolveLeaderId(Long requestedLeaderId, Set<Long> desiredIds, Map<Long, Boolean> currentMap) {
        if (requestedLeaderId != null) {
            return requestedLeaderId;
        }
        for (Map.Entry<Long, Boolean> entry : currentMap.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue()) && desiredIds.contains(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Handles fetch existing technician ids. */
    private Set<Long> fetchExistingTechnicianIds(Set<Long> technicianIds) {
        if (technicianIds.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(technicianIds.size(), "?"));
        String sql = "SELECT id FROM technicians WHERE is_deleted = 0 AND id IN (" + placeholders + ")";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, technicianIds.toArray());
        Set<Long> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.add(asLong(row.get("id")));
        }
        return ids;
    }

    /** Handles exists email for other technician. */
    private boolean existsEmailForOtherTechnician(String email, Long currentTechnicianId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(email) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(email) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLong(sql, email)
                : queryLong(sql, email, currentTechnicianId);
        return count > 0;
    }

    /** Validates badge unique. */
    private String requireBadgeUnique(String badgeNumber, Long currentTechnicianId) {
        String trimmed = requireNonBlank(badgeNumber, "badgeNumber is required");
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(badge_number) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(badge_number) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLong(sql, trimmed)
                : queryLong(sql, trimmed, currentTechnicianId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "badgeNumber already exists");
        }
        return trimmed;
    }

    /** Handles ensure technician id unique. */
    private void ensureTechnicianIdUnique(String technicianId, Long currentTechnicianId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(technician_id) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(technician_id) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLong(sql, technicianId)
                : queryLong(sql, technicianId, currentTechnicianId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "technicianId already exists");
        }
    }

    /** Handles determine technician id. */
    private String determineTechnicianId(String providedTechnicianId, Long currentTechnicianId) {
        if (providedTechnicianId != null && !providedTechnicianId.isBlank()) {
            String trimmed = providedTechnicianId.trim();
            ensureTechnicianIdUnique(trimmed, currentTechnicianId);
            return trimmed;
        }

        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 30; i++) {
            int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("TECH-%s-%04d", date, random);
            long count = queryLong(
                    "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(technician_id) = LOWER(?)",
                    candidate
            );
            if (count == 0) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate technicianId");
    }

    /** Returns technician row or throw tm. */
    private Map<String, Object> getTechnicianRowOrThrowTm(Long technicianId, Long companyId) {
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, company_id, technician_id, badge_number, first_name, last_name, full_name,
                       technician_type, skills, phone_number, email, address, status,
                       hire_date, work_shift, technician_photo_url, certificate_url,
                       certificate_issue_date, certificate_expiry_date, termination_date,
                       certifications, notes
                FROM technicians
                WHERE id = ? AND is_deleted = 0 AND company_id = ?
                """, technicianId, companyId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
        return rows.get(0);
    }

    /** Returns team row or throw tm. */
    private Map<String, Object> getTeamRowOrThrowTm(Long teamId, Long companyId) {
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, company_id, team_name, team_description, status, start_date, end_date, notes
                FROM technician_teams
                WHERE id = ? AND company_id = ?
                """, teamId, companyId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Technician team not found: " + teamId);
        }
        return rows.get(0);
    }

    /** Handles ensure team name unique tm. */
    private void ensureTeamNameUniqueTm(String teamName, Long currentTeamId, Long companyId) {
        String sql = currentTeamId == null
                ? "SELECT COUNT(1) FROM technician_teams WHERE company_id = ? AND LOWER(team_name) = LOWER(?)"
                : "SELECT COUNT(1) FROM technician_teams WHERE company_id = ? AND LOWER(team_name) = LOWER(?) AND id <> ?";
        long count = currentTeamId == null
                ? queryLongTm(sql, companyId, teamName)
                : queryLongTm(sql, companyId, teamName, currentTeamId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician team with the same name already exists");
        }
    }

    /** Applies team membership tm. */
    private void applyTeamMembershipTm(Long teamId, Long companyId, List<Long> technicianIds, Long requestedLeaderId) {
        boolean replaceMembership = technicianIds != null;
        if (!replaceMembership && requestedLeaderId == null) {
            return;
        }

        List<Map<String, Object>> currentMembers = tmJdbcTemplate.queryForList("""
                SELECT technician_id, team_leader
                FROM technician_team_members
                WHERE team_id = ?
                """, teamId);

        Set<Long> desiredIds;
        if (replaceMembership) {
            if (technicianIds.stream().anyMatch(Objects::isNull)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician IDs cannot be null");
            }
            desiredIds = new LinkedHashSet<>(technicianIds);
        } else {
            desiredIds = currentMembers.stream()
                    .map(member -> asLong(member.get("technician_id")))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }

        if (requestedLeaderId != null && !desiredIds.contains(requestedLeaderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team leader must be part of the assigned technicians");
        }

        if (replaceMembership) {
            syncTeamTechniciansTm(teamId, companyId, desiredIds, requestedLeaderId, currentMembers);
        } else {
            updateLeaderOnlyTm(currentMembers, teamId, requestedLeaderId);
        }
    }

    /** Synchronizes team technicians tm. */
    private void syncTeamTechniciansTm(Long teamId,
                                       Long companyId,
                                       Set<Long> desiredIds,
                                       Long requestedLeaderId,
                                       List<Map<String, Object>> currentMembers) {
        if (desiredIds.isEmpty()) {
            if (!currentMembers.isEmpty()) {
                tmJdbcTemplate.update("DELETE FROM technician_team_members WHERE team_id = ?", teamId);
            }
            return;
        }

        Set<Long> existingTechnicians = fetchExistingTechnicianIdsTm(desiredIds, companyId);
        if (existingTechnicians.size() != desiredIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more technicians were not found");
        }

        Map<Long, Boolean> currentMap = new HashMap<>();
        for (Map<String, Object> member : currentMembers) {
            currentMap.put(asLong(member.get("technician_id")), truthy(member.get("team_leader")));
        }

        Long leaderId = resolveLeaderId(requestedLeaderId, desiredIds, currentMap);

        for (Long currentTechnicianId : currentMap.keySet()) {
            if (!desiredIds.contains(currentTechnicianId)) {
                tmJdbcTemplate.update(
                        "DELETE FROM technician_team_members WHERE team_id = ? AND technician_id = ?",
                        teamId,
                        currentTechnicianId
                );
            }
        }

        for (Long technicianId : desiredIds) {
            boolean isLeader = leaderId != null && leaderId.equals(technicianId);
            if (currentMap.containsKey(technicianId)) {
                tmJdbcTemplate.update(
                        "UPDATE technician_team_members SET team_leader = ? WHERE team_id = ? AND technician_id = ?",
                        isLeader,
                        teamId,
                        technicianId
                );
            } else {
                tmJdbcTemplate.update(
                        "INSERT INTO technician_team_members (team_id, technician_id, team_leader) VALUES (?, ?, ?)",
                        teamId,
                        technicianId,
                        isLeader
                );
            }
        }
    }

    /** Updates leader only tm. */
    private void updateLeaderOnlyTm(List<Map<String, Object>> currentMembers, Long teamId, Long leaderId) {
        if (leaderId == null) {
            return;
        }
        if (currentMembers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team has no technicians to assign as leader");
        }
        boolean found = currentMembers.stream()
                .map(member -> asLong(member.get("technician_id")))
                .anyMatch(leaderId::equals);
        if (!found) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team leader must be part of the team");
        }
        for (Map<String, Object> member : currentMembers) {
            Long technicianId = asLong(member.get("technician_id"));
            tmJdbcTemplate.update(
                    "UPDATE technician_team_members SET team_leader = ? WHERE team_id = ? AND technician_id = ?",
                    leaderId.equals(technicianId),
                    teamId,
                    technicianId
            );
        }
    }

    /** Handles fetch existing technician ids tm. */
    private Set<Long> fetchExistingTechnicianIdsTm(Set<Long> technicianIds, Long companyId) {
        if (technicianIds.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(technicianIds.size(), "?"));
        String sql = "SELECT id FROM technicians WHERE is_deleted = 0 AND company_id = ? AND id IN (" + placeholders + ")";
        Object[] args = new Object[technicianIds.size() + 1];
        args[0] = companyId;
        int idx = 1;
        for (Long technicianId : technicianIds) {
            args[idx++] = technicianId;
        }
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList(sql, args);
        Set<Long> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.add(asLong(row.get("id")));
        }
        return ids;
    }

    /** Handles exists email for other technician tm. */
    private boolean existsEmailForOtherTechnicianTm(String email, Long currentTechnicianId, Long companyId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(email) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(email) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLongTm(sql, companyId, email)
                : queryLongTm(sql, companyId, email, currentTechnicianId);
        return count > 0;
    }

    /** Validates badge unique tm. */
    private String requireBadgeUniqueTm(String badgeNumber, Long currentTechnicianId, Long companyId) {
        String trimmed = requireNonBlank(badgeNumber, "badgeNumber is required");
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(badge_number) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(badge_number) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLongTm(sql, companyId, trimmed)
                : queryLongTm(sql, companyId, trimmed, currentTechnicianId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "badgeNumber already exists");
        }
        return trimmed;
    }

    /** Handles ensure technician id unique tm. */
    private void ensureTechnicianIdUniqueTm(String technicianId, Long currentTechnicianId, Long companyId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(technician_id) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(technician_id) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLongTm(sql, companyId, technicianId)
                : queryLongTm(sql, companyId, technicianId, currentTechnicianId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "technicianId already exists");
        }
    }

    /** Handles determine technician id tm. */
    private String determineTechnicianIdTm(String providedTechnicianId, Long currentTechnicianId, Long companyId) {
        if (providedTechnicianId != null && !providedTechnicianId.isBlank()) {
            String trimmed = providedTechnicianId.trim();
            ensureTechnicianIdUniqueTm(trimmed, currentTechnicianId, companyId);
            return trimmed;
        }

        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 30; i++) {
            int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("TECH-%s-%04d", date, random);
            long count = queryLongTm(
                    "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND company_id = ? AND LOWER(technician_id) = LOWER(?)",
                    companyId, candidate
            );
            if (count == 0) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate technicianId");
    }

    /** Handles ensure technician exists tm. */
    private void ensureTechnicianExistsTm(Long technicianId, Long companyId) {
        long count = queryLongTm("SELECT COUNT(1) FROM technicians WHERE id = ? AND company_id = ? AND is_deleted = 0", technicianId, companyId);
        if (count == 0) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
    }

    /** Handles ensure technician exists tm. */
    private void ensureTechnicianExistsTm(Long technicianId) {
        long count = queryLongTm("SELECT COUNT(1) FROM technicians WHERE id = ? AND is_deleted = 0", technicianId);
        if (count == 0) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
    }

    /** Validates company id. */
    private Long requireCompanyId(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId is required");
        }
        return companyId;
    }

    /** Handles ensure company exists in eam. */
    private void ensureCompanyExistsInEam(Long companyId) {
        long count = queryLong("SELECT COUNT(1) FROM companies WHERE id = ? AND active = 1", companyId);
        if (count == 0) {
            throw new ResourceNotFoundException("Company not found in EAM: " + companyId);
        }
    }

    /** Handles query long tm. */
    private long queryLongTm(String sql, Object... args) {
        Number value = tmJdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    /** Resolves termination date. */
    private LocalDate resolveTerminationDate(String technicianType, LocalDate terminationDate) {
        if ("CONTRACT".equalsIgnoreCase(technicianType)) {
            return terminationDate;
        }
        if (terminationDate != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "terminationDate allowed only for CONTRACT technicians");
        }
        return null;
    }

    /** Validates non blank. */
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    /** Handles safe trim. */
    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Normalizes upper default. */
    private String normalizeUpperDefault(String value, String fallback) {
        String resolved = safeTrim(value);
        if (resolved == null) {
            return fallback;
        }
        return resolved.toUpperCase();
    }

    /** Maps data to activity. */
    private TechnicianActivityDto mapActivity(Map<String, Object> row) {
        String status = asString(row.get("status"));
        String workOrderId = asString(row.get("work_order_id"));
        String activity = switch (status) {
            case "COMPLETED" -> "Completed Work Order #" + workOrderId;
            case "IN_PROGRESS" -> "Started Work Order #" + workOrderId;
            case "SCHEDULED" -> "Scheduled Work Order #" + workOrderId;
            default -> "Updated Work Order #" + workOrderId;
        };

        String technicianName = asString(row.get("technician_name"));
        if (technicianName == null || technicianName.isBlank()) {
            technicianName = asString(row.get("team_name"));
        }
        if (technicianName == null || technicianName.isBlank()) {
            technicianName = "Unassigned";
        }

        return TechnicianActivityDto.builder()
                .technicianName(technicianName)
                .activity(activity)
                .timeAgo(formatTimeAgo(asLocalDateTime(row.get("updated_at"))))
                .build();
    }

    /** Handles work order select. */
    private String workOrderSelect() {
        return """
                SELECT wo.id, wo.work_order_number, wo.work_order_id, wo.work_request_type_id,
                       wrt.code AS work_request_type_code, wrt.description AS work_request_type_description,
                       wo.location, wo.work_type, wo.priority, wo.wo_title, wo.description_scope,
                       wo.planner, wo.assigned_technician_id, t.full_name AS assigned_technician_name,
                       wo.assigned_team_id, tt.team_name AS assigned_team_name,
                       wo.planned_start_datetime, wo.planned_end_datetime,
                       wo.actual_start_datetime, wo.actual_end_datetime,
                       wo.target_completion_date, wo.status, wo.source,
                       wo.created_at, wo.updated_at
                FROM work_orders wo
                LEFT JOIN technicians t ON t.id = wo.assigned_technician_id
                LEFT JOIN technician_teams tt ON tt.id = wo.assigned_team_id
                LEFT JOIN work_request_types wrt ON wrt.id = wo.work_request_type_id
                """;
    }

    /** Handles leaves select. */
    private String leavesSelect() {
        return """
                SELECT l.id, l.technician_id, l.start_date, l.end_date, l.reason, l.created_at,
                       COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, '')))) AS technician_name
                FROM technician_leaves l
                JOIN technicians t ON t.id = l.technician_id
                """;
    }

    /** Maps data to technician tm. */
    private TechnicianDetailsResponse mapTechnicianTm(Map<String, Object> row, Long companyId) {
        Long technicianId = asLong(row.get("id"));
        List<TechnicianTeamMembershipResponse> memberships = tmJdbcTemplate.queryForList("""
                SELECT m.team_id, tt.team_name, m.team_leader
                FROM technician_team_members m
                JOIN technician_teams tt ON tt.id = m.team_id
                WHERE m.technician_id = ?
                  AND tt.company_id = ?
                """, technicianId, companyId).stream().map(member -> {
            Long teamId = asLong(member.get("team_id"));
            List<String> leaderNames = tmJdbcTemplate.query("""
                            SELECT COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                            LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, ''))))
                            FROM technician_team_members tm
                            JOIN technicians t ON t.id = tm.technician_id
                            JOIN technician_teams tt ON tt.id = tm.team_id
                            WHERE tm.team_id = ? AND tm.team_leader = 1 AND tt.company_id = ?
                            """,
                    (rs, rowNum) -> rs.getString(1),
                    teamId, companyId);
            return TechnicianTeamMembershipResponse.builder()
                    .teamId(teamId)
                    .teamName(asString(member.get("team_name")))
                    .teamLeader(truthy(member.get("team_leader")))
                    .teamLeaderNames(leaderNames)
                    .build();
        }).toList();

        return TechnicianDetailsResponse.builder()
                .id(technicianId)
                .companyId(asLong(row.get("company_id")))
                .technicianId(asString(row.get("technician_id")))
                .badgeNumber(asString(row.get("badge_number")))
                .firstName(asString(row.get("first_name")))
                .lastName(asString(row.get("last_name")))
                .fullName(resolveFullName(asString(row.get("full_name")), asString(row.get("first_name")), asString(row.get("last_name"))))
                .technicianType(asString(row.get("technician_type")))
                .skills(asString(row.get("skills")))
                .phoneNumber(asString(row.get("phone_number")))
                .email(asString(row.get("email")))
                .address(asString(row.get("address")))
                .status(asString(row.get("status")))
                .workStatus("AVAILABLE")
                .hireDate(asLocalDate(row.get("hire_date")))
                .workShift(asString(row.get("work_shift")))
                .technicianPhotoUrl(asString(row.get("technician_photo_url")))
                .certificateUrl(asString(row.get("certificate_url")))
                .certificateIssueDate(asLocalDate(row.get("certificate_issue_date")))
                .certificateExpiryDate(asLocalDate(row.get("certificate_expiry_date")))
                .terminationDate(asLocalDate(row.get("termination_date")))
                .certifications(asString(row.get("certifications")))
                .notes(asString(row.get("notes")))
                .teamLeader(memberships.stream().anyMatch(TechnicianTeamMembershipResponse::isTeamLeader))
                .teamMemberships(memberships)
                .build();
    }

    /** Maps data to team tm. */
    private TechnicianTeamDetailsResponse mapTeamTm(Map<String, Object> row, Long companyId) {
        Long teamId = asLong(row.get("id"));
        List<Map<String, Object>> technicianRows = tmJdbcTemplate.queryForList("""
                SELECT t.id, t.company_id, t.technician_id, t.badge_number, t.first_name, t.last_name, t.full_name,
                       t.technician_type, t.skills, t.phone_number, t.email, t.address, t.status,
                       t.hire_date, t.work_shift, t.technician_photo_url, t.certificate_url,
                       t.certificate_issue_date, t.certificate_expiry_date, t.termination_date,
                       t.certifications, t.notes
                FROM technician_team_members m
                JOIN technicians t ON t.id = m.technician_id
                WHERE m.team_id = ? AND t.is_deleted = 0 AND t.company_id = ?
                ORDER BY t.id
                """, teamId, companyId);
        List<TechnicianDetailsResponse> technicians = technicianRows.stream()
                .map(technicianRow -> mapTechnicianTm(technicianRow, companyId))
                .toList();

        List<Map<String, Object>> leaderRows = tmJdbcTemplate.queryForList("""
                SELECT TOP 1 t.id AS leader_id,
                       COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, '')))) AS leader_name
                FROM technician_team_members m
                JOIN technicians t ON t.id = m.technician_id
                JOIN technician_teams tt ON tt.id = m.team_id
                WHERE m.team_id = ? AND m.team_leader = 1 AND tt.company_id = ?
                """, teamId, companyId);
        Long teamLeaderId = leaderRows.isEmpty() ? null : asLong(leaderRows.get(0).get("leader_id"));
        String teamLeaderName = leaderRows.isEmpty() ? null : asString(leaderRows.get(0).get("leader_name"));

        return TechnicianTeamDetailsResponse.builder()
                .id(teamId)
                .companyId(asLong(row.get("company_id")))
                .teamName(asString(row.get("team_name")))
                .teamDescription(asString(row.get("team_description")))
                .status(asString(row.get("status")))
                .startDate(asLocalDate(row.get("start_date")))
                .endDate(asLocalDate(row.get("end_date")))
                .notes(asString(row.get("notes")))
                .teamLeaderId(teamLeaderId)
                .teamLeaderName(teamLeaderName)
                .availability("Available")
                .technicians(technicians)
                .build();
    }

    /** Maps data to technician. */
    private TechnicianDetailsResponse mapTechnician(Map<String, Object> row) {
        Long technicianId = asLong(row.get("id"));
        List<TechnicianTeamMembershipResponse> memberships = jdbcTemplate.queryForList("""
                SELECT m.team_id, tt.team_name, m.team_leader
                FROM technician_team_members m
                JOIN technician_teams tt ON tt.id = m.team_id
                WHERE m.technician_id = ?
                """, technicianId).stream().map(member -> {
            Long teamId = asLong(member.get("team_id"));
            List<String> leaderNames = jdbcTemplate.query("""
                            SELECT COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                            LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, ''))))
                            FROM technician_team_members tm
                            JOIN technicians t ON t.id = tm.technician_id
                            WHERE tm.team_id = ? AND tm.team_leader = 1
                            """,
                    (rs, rowNum) -> rs.getString(1),
                    teamId);
            return TechnicianTeamMembershipResponse.builder()
                    .teamId(teamId)
                    .teamName(asString(member.get("team_name")))
                    .teamLeader(truthy(member.get("team_leader")))
                    .teamLeaderNames(leaderNames)
                    .build();
        }).toList();

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        long activeBookings = queryLong("""
                SELECT COUNT(1)
                FROM work_orders wo
                WHERE wo.deleted = 0
                  AND wo.status IN ('SCHEDULED','IN_PROGRESS')
                  AND wo.planned_start_datetime < ?
                  AND wo.planned_end_datetime > ?
                  AND (
                        wo.assigned_technician_id = ?
                        OR wo.assigned_team_id IN (SELECT tm.team_id FROM technician_team_members tm WHERE tm.technician_id = ?)
                  )
                """, dayEnd, dayStart, technicianId, technicianId);

        return TechnicianDetailsResponse.builder()
                .id(technicianId)
                .technicianId(asString(row.get("technician_id")))
                .badgeNumber(asString(row.get("badge_number")))
                .firstName(asString(row.get("first_name")))
                .lastName(asString(row.get("last_name")))
                .fullName(resolveFullName(asString(row.get("full_name")), asString(row.get("first_name")), asString(row.get("last_name"))))
                .technicianType(asString(row.get("technician_type")))
                .skills(asString(row.get("skills")))
                .phoneNumber(asString(row.get("phone_number")))
                .email(asString(row.get("email")))
                .address(asString(row.get("address")))
                .status(asString(row.get("status")))
                .workStatus(activeBookings > 0 ? "WORKING" : "AVAILABLE")
                .hireDate(asLocalDate(row.get("hire_date")))
                .workShift(asString(row.get("work_shift")))
                .technicianPhotoUrl(asString(row.get("technician_photo_url")))
                .certificateUrl(asString(row.get("certificate_url")))
                .certificateIssueDate(asLocalDate(row.get("certificate_issue_date")))
                .certificateExpiryDate(asLocalDate(row.get("certificate_expiry_date")))
                .terminationDate(asLocalDate(row.get("termination_date")))
                .certifications(asString(row.get("certifications")))
                .notes(asString(row.get("notes")))
                .teamLeader(memberships.stream().anyMatch(TechnicianTeamMembershipResponse::isTeamLeader))
                .teamMemberships(memberships)
                .build();
    }

    /** Builds availability. */
    private List<DailyAvailabilityDto> buildAvailability(Long technicianId, Integer days, Long companyId) {
        int horizon = days == null ? 31 : Math.max(days, 1);
        LocalDate start = LocalDate.now();
        LocalDate endExclusive = start.plusDays(horizon);

        Map<LocalDate, String> statusByDate = new HashMap<>();
        for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
            statusByDate.put(d, d.getDayOfWeek().getValue() == 7 ? "HOLIDAY" : "AVAILABLE");
        }

        boolean companyScopedHolidays = hasCompanyIdColumn("technician_holidays");
        List<LocalDate> holidays = companyScopedHolidays
                ? jdbcTemplate.query("""
                            SELECT holiday_date
                            FROM technician_holidays
                            WHERE holiday_date >= ? AND holiday_date < ? AND company_id = ?
                            """,
                    (rs, rowNum) -> rs.getObject("holiday_date", LocalDate.class),
                    start,
                    endExclusive,
                    companyId)
                : jdbcTemplate.query("""
                            SELECT holiday_date
                            FROM technician_holidays
                            WHERE holiday_date >= ? AND holiday_date < ?
                            """,
                    (rs, rowNum) -> rs.getObject("holiday_date", LocalDate.class),
                    start,
                    endExclusive);
        for (LocalDate holiday : holidays) {
            statusByDate.put(holiday, "HOLIDAY");
        }

        boolean companyScopedLeaves = hasCompanyIdColumn("technician_leaves");
        List<Map<String, Object>> leaves = companyScopedLeaves
                ? jdbcTemplate.queryForList("""
                    SELECT start_date, end_date
                    FROM technician_leaves
                    WHERE technician_id = ? AND end_date >= ? AND start_date < ? AND company_id = ?
                    """, technicianId, start, endExclusive, companyId)
                : jdbcTemplate.queryForList("""
                    SELECT start_date, end_date
                    FROM technician_leaves
                    WHERE technician_id = ? AND end_date >= ? AND start_date < ?
                    """, technicianId, start, endExclusive);
        for (Map<String, Object> leave : leaves) {
            LocalDate leaveStart = asLocalDate(leave.get("start_date"));
            LocalDate leaveEnd = asLocalDate(leave.get("end_date"));
            LocalDate cursor = leaveStart.isBefore(start) ? start : leaveStart;
            LocalDate bound = leaveEnd.isBefore(endExclusive.minusDays(1)) ? leaveEnd : endExclusive.minusDays(1);
            for (; !cursor.isAfter(bound); cursor = cursor.plusDays(1)) {
                statusByDate.put(cursor, "PTO");
            }
        }

        boolean companyScopedWorkOrders = hasCompanyIdColumn("work_orders");
        List<Map<String, Object>> bookings = companyScopedWorkOrders
                ? jdbcTemplate.queryForList("""
                    SELECT planned_start_datetime, planned_end_datetime
                    FROM work_orders
                    WHERE deleted = 0
                      AND status IN ('SCHEDULED','IN_PROGRESS')
                      AND planned_start_datetime < ?
                      AND planned_end_datetime > ?
                      AND company_id = ?
                      AND (
                            assigned_technician_id = ?
                            OR assigned_team_id IN (SELECT tm.team_id FROM technician_team_members tm WHERE tm.technician_id = ?)
                      )
                    """, endExclusive.atStartOfDay(), start.atStartOfDay(), companyId, technicianId, technicianId)
                : jdbcTemplate.queryForList("""
                    SELECT planned_start_datetime, planned_end_datetime
                    FROM work_orders
                    WHERE deleted = 0
                      AND status IN ('SCHEDULED','IN_PROGRESS')
                      AND planned_start_datetime < ?
                      AND planned_end_datetime > ?
                      AND (
                            assigned_technician_id = ?
                            OR assigned_team_id IN (SELECT tm.team_id FROM technician_team_members tm WHERE tm.technician_id = ?)
                      )
                    """, endExclusive.atStartOfDay(), start.atStartOfDay(), technicianId, technicianId);

        List<DailyAvailabilityDto> response = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atTime(9, 0);
            LocalDateTime dayEnd = d.atTime(21, 0);
            String status = statusByDate.getOrDefault(d, "AVAILABLE");

            List<Window> dayBusy = bookings.stream()
                    .map(booking -> new Window(asLocalDateTime(booking.get("planned_start_datetime")), asLocalDateTime(booking.get("planned_end_datetime"))))
                    .filter(w -> w.end().isAfter(dayStart) && w.start().isBefore(dayEnd))
                    .map(w -> new Window(w.start().isAfter(dayStart) ? w.start() : dayStart, w.end().isBefore(dayEnd) ? w.end() : dayEnd))
                    .filter(w -> w.start().isBefore(w.end()))
                    .sorted(Comparator.comparing(Window::start))
                    .toList();

            List<TimeWindowDto> busyWindows;
            List<TimeWindowDto> freeWindows;
            if ("HOLIDAY".equals(status) || "PTO".equals(status)) {
                busyWindows = List.of(windowMap(dayStart.toLocalTime(), dayEnd.toLocalTime()));
                freeWindows = List.of();
            } else {
                List<Window> merged = merge(dayBusy);
                busyWindows = merged.stream().map(w -> windowMap(w.start().toLocalTime(), w.end().toLocalTime())).toList();
                freeWindows = free(dayStart, dayEnd, merged).stream().map(w -> windowMap(w.start().toLocalTime(), w.end().toLocalTime())).toList();
                if (!merged.isEmpty()) {
                    status = "WORKING";
                }
            }

            response.add(DailyAvailabilityDto.builder()
                    .date(d)
                    .status(status)
                    .busyWindows(busyWindows)
                    .freeWindows(freeWindows)
                    .build());
        }
        return response;
    }

    /** Maps data to team. */
    private TechnicianTeamDetailsResponse mapTeam(Map<String, Object> row) {
        Long teamId = asLong(row.get("id"));
        List<Map<String, Object>> technicianRows = jdbcTemplate.queryForList("""
                SELECT t.id, t.technician_id, t.badge_number, t.first_name, t.last_name, t.full_name,
                       t.technician_type, t.skills, t.phone_number, t.email, t.address, t.status,
                       t.hire_date, t.work_shift, t.technician_photo_url, t.certificate_url,
                       t.certificate_issue_date, t.certificate_expiry_date, t.termination_date,
                       t.certifications, t.notes
                FROM technician_team_members m
                JOIN technicians t ON t.id = m.technician_id
                WHERE m.team_id = ? AND t.is_deleted = 0
                ORDER BY t.id
                """, teamId);
        List<TechnicianDetailsResponse> technicians = technicianRows.stream().map(this::mapTechnician).toList();

        List<Map<String, Object>> leaderRows = jdbcTemplate.queryForList("""
                SELECT TOP 1 t.id AS leader_id,
                       COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, '')))) AS leader_name
                FROM technician_team_members m
                JOIN technicians t ON t.id = m.technician_id
                WHERE m.team_id = ? AND m.team_leader = 1
                """, teamId);
        Long teamLeaderId = leaderRows.isEmpty() ? null : asLong(leaderRows.get(0).get("leader_id"));
        String teamLeaderName = leaderRows.isEmpty() ? null : asString(leaderRows.get(0).get("leader_name"));

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        long activeBookings = queryLong("""
                SELECT COUNT(1)
                FROM work_orders
                WHERE deleted = 0
                  AND status IN ('SCHEDULED','IN_PROGRESS')
                AND assigned_team_id = ?
                  AND planned_start_datetime < ?
                  AND planned_end_datetime > ?
                """, teamId, dayEnd, dayStart);

        return TechnicianTeamDetailsResponse.builder()
                .id(teamId)
                .teamName(asString(row.get("team_name")))
                .teamDescription(asString(row.get("team_description")))
                .status(asString(row.get("status")))
                .startDate(asLocalDate(row.get("start_date")))
                .endDate(asLocalDate(row.get("end_date")))
                .notes(asString(row.get("notes")))
                .teamLeaderId(teamLeaderId)
                .teamLeaderName(teamLeaderName)
                .availability(activeBookings > 0 ? "Unavailable" : "Available")
                .technicians(technicians)
                .build();
    }

    /** Maps data to work order. */
    private WorkOrderDetailsResponse mapWorkOrder(Map<String, Object> row) {
        return WorkOrderDetailsResponse.builder()
                .id(asLong(row.get("id")))
                .workOrderNumber(asString(row.get("work_order_number")))
                .workOrderId(asString(row.get("work_order_id")))
                .workRequestTypeId(asLong(row.get("work_request_type_id")))
                .workRequestTypeCode(asString(row.get("work_request_type_code")))
                .workRequestTypeDescription(asString(row.get("work_request_type_description")))
                .location(asString(row.get("location")))
                .workType(asString(row.get("work_type")))
                .priority(asString(row.get("priority")))
                .woTitle(asString(row.get("wo_title")))
                .descriptionScope(asString(row.get("description_scope")))
                .planner(asString(row.get("planner")))
                .assignedTechnicianId(asLong(row.get("assigned_technician_id")))
                .assignedTechnicianName(asString(row.get("assigned_technician_name")))
                .assignedTeamId(asLong(row.get("assigned_team_id")))
                .assignedTeamName(asString(row.get("assigned_team_name")))
                .plannedStartDateTime(asLocalDateTime(row.get("planned_start_datetime")))
                .plannedEndDateTime(asLocalDateTime(row.get("planned_end_datetime")))
                .actualStartDateTime(asLocalDateTime(row.get("actual_start_datetime")))
                .actualEndDateTime(asLocalDateTime(row.get("actual_end_datetime")))
                .targetCompletionDate(asLocalDate(row.get("target_completion_date")))
                .status(asString(row.get("status")))
                .source(asString(row.get("source")))
                .createdAt(asLocalDateTime(row.get("created_at")))
                .updatedAt(asLocalDateTime(row.get("updated_at")))
                .build();
    }

    /** Maps data to holiday. */
    private TechnicianHolidayResponse mapHoliday(Map<String, Object> row) {
        return TechnicianHolidayResponse.builder()
                .id(asLong(row.get("id")))
                .holidayName(asString(row.get("holiday_name")))
                .holidayType(asString(row.get("holiday_type")))
                .holidayDate(asLocalDate(row.get("holiday_date")))
                .notes(asString(row.get("notes")))
                .createdAt(asLocalDateTime(row.get("created_at")))
                .build();
    }

    /** Maps data to leave. */
    private TechnicianLeaveResponse mapLeave(Map<String, Object> row) {
        return TechnicianLeaveResponse.builder()
                .id(asLong(row.get("id")))
                .technicianId(asLong(row.get("technician_id")))
                .technicianName(asString(row.get("technician_name")))
                .startDate(asLocalDate(row.get("start_date")))
                .endDate(asLocalDate(row.get("end_date")))
                .reason(asString(row.get("reason")))
                .createdAt(asLocalDateTime(row.get("created_at")))
                .build();
    }

    /** Handles ensure technician exists. */
    private void ensureTechnicianExists(Long technicianId) {
        long count = queryLong("SELECT COUNT(1) FROM technicians WHERE id = ? AND is_deleted = 0", technicianId);
        if (count == 0) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
    }

    /** Handles query long. */
    private long queryLong(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    /** Checks whether company id column. */
    private boolean hasCompanyIdColumn(String tableName) {
        return columnExists(tableName, "company_id");
    }

    /** Checks whether tm company id column. */
    private boolean hasTmCompanyIdColumn(String tableName) {
        return columnExistsTm(tableName, "company_id");
    }

    /** Handles column exists. */
    private boolean columnExists(String tableName, String columnName) {
        long count = queryLong("""
                SELECT COUNT(1)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ?
                  AND LOWER(COLUMN_NAME) = LOWER(?)
                """, tableName, columnName);
        return count > 0;
    }

    /** Handles column exists tm. */
    private boolean columnExistsTm(String tableName, String columnName) {
        long count = queryLongTm("""
                SELECT COUNT(1)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ?
                  AND LOWER(COLUMN_NAME) = LOWER(?)
                """, tableName, columnName);
        return count > 0;
    }

    /** Handles table exists. */
    private boolean tableExists(String tableName) {
        long count = queryLong("""
                SELECT COUNT(1)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_TYPE = 'BASE TABLE'
                  AND TABLE_NAME = ?
                """, tableName);
        return count > 0;
    }

    /** Handles append arg. */
    private Object[] appendArg(Object[] source, Object extraArg) {
        Object[] args = java.util.Arrays.copyOf(source, source.length + 1);
        args[source.length] = extraArg;
        return args;
    }

    /** Handles format time ago. */
    private String formatTimeAgo(LocalDateTime value) {
        if (value == null) {
            return "just now";
        }
        long minutes = ChronoUnit.MINUTES.between(value, LocalDateTime.now());
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " mins ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        long days = hours / 24;
        return days == 1 ? "1 day ago" : days + " days ago";
    }

    /** Handles merge. */
    private List<Window> merge(List<Window> input) {
        if (input.isEmpty()) {
            return List.of();
        }
        List<Window> merged = new ArrayList<>();
        Window current = input.get(0);
        for (int i = 1; i < input.size(); i++) {
            Window next = input.get(i);
            if (!next.start().isAfter(current.end())) {
                LocalDateTime end = next.end().isAfter(current.end()) ? next.end() : current.end();
                current = new Window(current.start(), end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    /** Handles free. */
    private List<Window> free(LocalDateTime rangeStart, LocalDateTime rangeEnd, List<Window> busy) {
        List<Window> free = new ArrayList<>();
        LocalDateTime cursor = rangeStart;
        for (Window block : busy) {
            if (cursor.isBefore(block.start())) {
                free.add(new Window(cursor, block.start()));
            }
            if (cursor.isBefore(block.end())) {
                cursor = block.end();
            }
        }
        if (cursor.isBefore(rangeEnd)) {
            free.add(new Window(cursor, rangeEnd));
        }
        return free;
    }

    /** Handles window map. */
    private TimeWindowDto windowMap(LocalTime start, LocalTime end) {
        return TimeWindowDto.builder()
                .start(start)
                .end(end)
                .build();
    }

    /** Handles truthy. */
    private boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    /** Handles as string. */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** Handles as long. */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /** Handles as local date. */
    private LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    /** Handles as local date time. */
    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime t) {
            return t;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate().atStartOfDay();
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    /** Resolves full name. */
    private String resolveFullName(String fullName, String firstName, String lastName) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        String resolved = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return resolved.isBlank() ? null : resolved;
    }

    private record Window(LocalDateTime start, LocalDateTime end) {
    }

    private record PropertyUnitSource(String tableName, String columnName) {
    }
}
