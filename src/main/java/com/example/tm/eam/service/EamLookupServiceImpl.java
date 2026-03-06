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
import org.springframework.web.server.ResponseStatusException;

@Service
public class EamLookupServiceImpl implements EamLookupService {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate tmJdbcTemplate;

    public EamLookupServiceImpl(
            @Qualifier("eamJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("tmJdbcTemplate") JdbcTemplate tmJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.tmJdbcTemplate = tmJdbcTemplate;
    }

    @Override
    public TechnicianDashboardResponse getDashboardTechnicians(Integer limit) {
        int resolvedLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 50));
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        long totalTechnicians = queryLong("SELECT COUNT(1) FROM technicians WHERE is_deleted = 0");
        boolean isHolidayToday = queryLong("SELECT COUNT(1) FROM technician_holidays WHERE holiday_date = ?", today) > 0;
        long onLeave = queryLong("SELECT COUNT(DISTINCT technician_id) FROM technician_leaves WHERE start_date <= ? AND end_date >= ?", today, today);
        long workOrders = queryLong("SELECT COUNT(1) FROM work_orders WHERE deleted = 0");
        long busyCount = queryLong("""
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

        List<Map<String, Object>> recentRows = jdbcTemplate.queryForList("""
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

    @Override
    public TechnicianDetailsResponse createTechnician(TechnicianCreateRequest request) {
        String firstName = requireNonBlank(request.getFirstName(), "First name is required");
        String lastName = requireNonBlank(request.getLastName(), "Last name is required");
        String badgeNumber = requireBadgeUniqueTm(request.getBadgeNumber(), null);
        String technicianId = determineTechnicianIdTm(request.getTechnicianId(), null);
        String technicianType = normalizeUpperDefault(request.getTechnicianType(), "FULL_TIME");
        String status = normalizeUpperDefault(request.getStatus(), "ACTIVE");
        String email = safeTrim(request.getEmail());
        if (email != null && existsEmailForOtherTechnicianTm(email, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician with the same email already exists");
        }

        LocalDate terminationDate = resolveTerminationDate(technicianType, request.getTerminationDate());
        String fullName = resolveFullName(null, firstName, lastName);

        Long technicianPk = tmJdbcTemplate.queryForObject("""
                INSERT INTO technicians (
                    technician_id, badge_number, first_name, last_name, full_name,
                    technician_type, skills, phone_number, email, address, status,
                    hire_date, work_shift, technician_photo_url, certificate_url,
                    certificate_issue_date, certificate_expiry_date, termination_date,
                    certifications, notes, is_deleted
                )
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                Long.class,
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
        return getTechnicianById(technicianPk);
    }

    @Override
    public TechnicianDetailsResponse getTechnicianById(Long technicianId) {
        Map<String, Object> row = getTechnicianRowOrThrowTm(technicianId);
        return mapTechnicianTm(row);
    }

    @Override
    public TechnicianDetailsResponse patchTechnician(Long technicianId, TechnicianPatchRequest request) {
        Map<String, Object> current = getTechnicianRowOrThrowTm(technicianId);

        String firstName = request.getFirstName() == null
                ? asString(current.get("first_name"))
                : requireNonBlank(request.getFirstName(), "First name cannot be blank");
        String lastName = request.getLastName() == null
                ? asString(current.get("last_name"))
                : requireNonBlank(request.getLastName(), "Last name cannot be blank");

        String badgeNumber = request.getBadgeNumber() == null
                ? asString(current.get("badge_number"))
                : requireBadgeUniqueTm(request.getBadgeNumber(), technicianId);

        String resolvedTechnicianId;
        if (request.getTechnicianId() == null) {
            resolvedTechnicianId = asString(current.get("technician_id"));
        } else {
            String trimmed = requireNonBlank(request.getTechnicianId(), "technicianId cannot be blank");
            ensureTechnicianIdUniqueTm(trimmed, technicianId);
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
            if (email != null && existsEmailForOtherTechnicianTm(email, technicianId)) {
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
                WHERE id = ? AND is_deleted = 0
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
                technicianId);

        return getTechnicianById(technicianId);
    }

    @Override
    public void deleteTechnician(Long technicianId) {
        ensureTechnicianExistsTm(technicianId);
        tmJdbcTemplate.update("DELETE FROM technician_team_members WHERE technician_id = ?", technicianId);
        tmJdbcTemplate.update("UPDATE technicians SET is_deleted = 1 WHERE id = ?", technicianId);
    }

    @Override
    public TechnicianListResponse getTechnicians(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLongTm("SELECT COUNT(1) FROM technicians WHERE is_deleted = 0");
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, technician_id, badge_number, first_name, last_name, full_name,
                       technician_type, skills, phone_number, email, address, status,
                       hire_date, work_shift, technician_photo_url, certificate_url,
                       certificate_issue_date, certificate_expiry_date, termination_date,
                       certifications, notes
                FROM technicians
                WHERE is_deleted = 0
                ORDER BY id
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, offset, safeSize);

        List<TechnicianDetailsResponse> technicians = rows.stream().map(this::mapTechnicianTm).toList();
        return TechnicianListResponse.builder()
                .technicians(technicians)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize))
                .last(safePage >= Math.max((int) Math.ceil((double) total / safeSize) - 1, 0))
                .build();
    }

    @Override
    public List<DailyAvailabilityDto> getTechnicianAvailabilityMonthly(Long technicianId, Integer days) {
        ensureTechnicianExists(technicianId);
        return buildAvailability(technicianId, days);
    }

    @Override
    public TechnicianTeamDetailsResponse createTechnicianTeam(TechnicianTeamCreateRequest request) {
        String teamName = requireNonBlank(request.getTeamName(), "Team name is required");
        ensureTeamNameUniqueTm(teamName, null);
        String status = normalizeUpperDefault(request.getStatus(), "ACTIVE");

        Long teamId = tmJdbcTemplate.queryForObject("""
                INSERT INTO technician_teams (
                    team_name, team_description, status, start_date, end_date, notes
                )
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                Long.class,
                teamName,
                request.getTeamDescription(),
                status,
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes());
        if (teamId == null || teamId <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create technician team");
        }
        applyTeamMembershipTm(teamId, request.getTechnicianIds(), request.getTeamLeaderId());
        return getTechnicianTeamById(teamId);
    }

    @Override
    public TechnicianTeamDetailsResponse getTechnicianTeamById(Long teamId) {
        Map<String, Object> row = getTeamRowOrThrowTm(teamId);
        return mapTeamTm(row);
    }

    @Override
    public TechnicianTeamDetailsResponse patchTechnicianTeam(Long teamId, TechnicianTeamPatchRequest request) {
        Map<String, Object> current = getTeamRowOrThrowTm(teamId);

        String teamName = request.getTeamName() == null
                ? asString(current.get("team_name"))
                : requireNonBlank(request.getTeamName(), "Team name cannot be blank");
        if (request.getTeamName() != null) {
            ensureTeamNameUniqueTm(teamName, teamId);
        }

        tmJdbcTemplate.update("""
                UPDATE technician_teams
                SET team_name = ?, team_description = ?, status = ?, start_date = ?, end_date = ?, notes = ?
                WHERE id = ?
                """,
                teamName,
                request.getTeamDescription() == null ? asString(current.get("team_description")) : request.getTeamDescription(),
                request.getStatus() == null ? asString(current.get("status")) : normalizeUpperDefault(request.getStatus(), "ACTIVE"),
                request.getStartDate() == null ? asLocalDate(current.get("start_date")) : request.getStartDate(),
                request.getEndDate() == null ? asLocalDate(current.get("end_date")) : request.getEndDate(),
                request.getNotes() == null ? asString(current.get("notes")) : request.getNotes(),
                teamId);

        applyTeamMembershipTm(teamId, request.getTechnicianIds(), request.getTeamLeaderId());
        return getTechnicianTeamById(teamId);
    }

    @Override
    public void deleteTechnicianTeam(Long teamId) {
        getTeamRowOrThrowTm(teamId);
        long membersCount = queryLongTm("SELECT COUNT(1) FROM technician_team_members WHERE team_id = ?", teamId);
        if (membersCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete team with assigned technicians");
        }
        tmJdbcTemplate.update("DELETE FROM technician_teams WHERE id = ?", teamId);
    }

    @Override
    public TechnicianTeamListResponse getTechnicianTeams(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLongTm("SELECT COUNT(1) FROM technician_teams");
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, team_name, team_description, status, start_date, end_date, notes
                FROM technician_teams
                ORDER BY id
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, offset, safeSize);
        List<TechnicianTeamDetailsResponse> teams = rows.stream().map(this::mapTeamTm).toList();
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

    @Override
    public WorkOrderNumberListResponse getWorkOrderNumbers(int page, int size, Long technicianId) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        long total = queryLong("""
                SELECT COUNT(DISTINCT wo.work_order_number)
                FROM work_orders wo
                WHERE wo.deleted = 0
                  AND wo.work_order_number IS NOT NULL
                  AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                """);
        List<String> workOrderNumbers = jdbcTemplate.queryForList("""
                SELECT DISTINCT wo.work_order_number
                FROM work_orders wo
                WHERE wo.deleted = 0
                  AND wo.work_order_number IS NOT NULL
                  AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                ORDER BY wo.work_order_number ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, String.class, offset, safeSize);
        List<String> favouriteWorkOrderNumbers = getFavouriteWorkOrderNumbers(technicianId);

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

    @Override
    public WorkOrderGlAccountListResponse getWorkOrderGlAccounts(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        long total = queryLong("""
                SELECT COUNT(DISTINCT LTRIM(RTRIM(wo.gl_account)))
                FROM work_orders wo
                WHERE wo.deleted = 0
                  AND wo.gl_account IS NOT NULL
                  AND LTRIM(RTRIM(wo.gl_account)) <> ''
                """);
        List<String> glAccounts = jdbcTemplate.queryForList("""
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

    @Override
    public WorkRequestTypePropertyUnitListResponse getWorkRequestTypePropertyUnits(int page, int size) {
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
        String totalSql = """
                SELECT COUNT(DISTINCT LTRIM(RTRIM(src.%s)))
                FROM %s src
                WHERE src.%s IS NOT NULL
                  AND LTRIM(RTRIM(src.%s)) <> ''
                """.formatted(quotedColumn, quotedTable, quotedColumn, quotedColumn);
        String listSql = """
                SELECT DISTINCT LTRIM(RTRIM(src.%s)) AS property_unit
                FROM %s src
                WHERE src.%s IS NOT NULL
                  AND LTRIM(RTRIM(src.%s)) <> ''
                ORDER BY property_unit ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.formatted(quotedColumn, quotedTable, quotedColumn, quotedColumn);

        long total = queryLong(totalSql);
        List<String> propertyUnits = jdbcTemplate.queryForList(listSql, String.class, offset, safeSize);

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

    @Override
    public WorkOrderListResponse getWorkOrders(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLong("SELECT COUNT(1) FROM work_orders WHERE deleted = 0");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(workOrderSelect() + """
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

    @Override
    public WorkOrderDetailsResponse getWorkOrderById(Long workOrderId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(workOrderSelect() + " WHERE wo.deleted = 0 AND wo.id = ?", workOrderId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Work order not found: " + workOrderId);
        }
        return mapWorkOrder(rows.get(0));
    }

    @Override
    public WorkOrderDetailsResponse addWorkOrderToFavourites(Long technicianId, Long workOrderId) {
        ensureTechnicianExistsTm(technicianId);
        WorkOrderDetailsResponse workOrder = getWorkOrderById(workOrderId);

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

        return workOrder;
    }

    private List<String> getFavouriteWorkOrderNumbers(Long technicianId) {
        if (technicianId == null) {
            return List.of();
        }

        ensureTechnicianExistsTm(technicianId);
        List<Long> workOrderIds = tmJdbcTemplate.query("""
                        SELECT work_order_id
                        FROM work_order_favourites
                        WHERE technician_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                (rs, rowNum) -> rs.getLong("work_order_id"),
                technicianId);
        if (workOrderIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(workOrderIds.size(), "?"));
        String sql = """
                SELECT wo.id, LTRIM(RTRIM(wo.work_order_number)) AS work_order_number
                FROM work_orders wo
                WHERE wo.deleted = 0
                  AND wo.id IN (%s)
                  AND wo.work_order_number IS NOT NULL
                  AND LTRIM(RTRIM(wo.work_order_number)) <> ''
                """.formatted(placeholders);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, workOrderIds.toArray());

        Map<Long, String> numberById = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = asLong(row.get("id"));
            String number = asString(row.get("work_order_number"));
            if (id != null && number != null && !number.isBlank()) {
                numberById.put(id, number);
            }
        }

        Set<String> orderedUnique = new LinkedHashSet<>();
        for (Long workOrderId : workOrderIds) {
            String number = numberById.get(workOrderId);
            if (number != null && !number.isBlank()) {
                orderedUnique.add(number);
            }
        }
        return new ArrayList<>(orderedUnique);
    }

    @Override
    public TechnicianHolidayListResponse getHolidays(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        long total = queryLong("SELECT COUNT(1) FROM technician_holidays");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
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

    @Override
    public TechnicianHolidayResponse getHolidayById(Long holidayId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, holiday_name, holiday_type, holiday_date, notes, created_at
                FROM technician_holidays
                WHERE id = ?
                """, holidayId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Holiday not found: " + holidayId);
        }
        return mapHoliday(rows.get(0));
    }

    @Override
    public TechnicianLeaveListResponse getTechniciansLeaves(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);
        int offset = safePage * safeSize;

        long total = queryLong("SELECT COUNT(1) FROM technician_leaves");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(leavesSelect() + """
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

    @Override
    public TechnicianLeaveListResponse getTechnicianLeaves(Long technicianId) {
        ensureTechnicianExists(technicianId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(leavesSelect() + " WHERE l.technician_id = ? ORDER BY l.start_date ASC, l.id ASC", technicianId);
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

    @Override
    public TechnicianLeaveResponse getTechnicianLeaveById(Long technicianId, Long leaveId) {
        ensureTechnicianExists(technicianId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(leavesSelect() + " WHERE l.technician_id = ? AND l.id = ?", technicianId, leaveId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Leave not found: " + leaveId);
        }
        return mapLeave(rows.get(0));
    }

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

    private boolean existsEmailForOtherTechnician(String email, Long currentTechnicianId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(email) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(email) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLong(sql, email)
                : queryLong(sql, email, currentTechnicianId);
        return count > 0;
    }

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

    private Map<String, Object> getTechnicianRowOrThrowTm(Long technicianId) {
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
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

    private Map<String, Object> getTeamRowOrThrowTm(Long teamId) {
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList("""
                SELECT id, team_name, team_description, status, start_date, end_date, notes
                FROM technician_teams
                WHERE id = ?
                """, teamId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Technician team not found: " + teamId);
        }
        return rows.get(0);
    }

    private void ensureTeamNameUniqueTm(String teamName, Long currentTeamId) {
        String sql = currentTeamId == null
                ? "SELECT COUNT(1) FROM technician_teams WHERE LOWER(team_name) = LOWER(?)"
                : "SELECT COUNT(1) FROM technician_teams WHERE LOWER(team_name) = LOWER(?) AND id <> ?";
        long count = currentTeamId == null
                ? queryLongTm(sql, teamName)
                : queryLongTm(sql, teamName, currentTeamId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician team with the same name already exists");
        }
    }

    private void applyTeamMembershipTm(Long teamId, List<Long> technicianIds, Long requestedLeaderId) {
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
            syncTeamTechniciansTm(teamId, desiredIds, requestedLeaderId, currentMembers);
        } else {
            updateLeaderOnlyTm(currentMembers, teamId, requestedLeaderId);
        }
    }

    private void syncTeamTechniciansTm(Long teamId,
                                       Set<Long> desiredIds,
                                       Long requestedLeaderId,
                                       List<Map<String, Object>> currentMembers) {
        if (desiredIds.isEmpty()) {
            if (!currentMembers.isEmpty()) {
                tmJdbcTemplate.update("DELETE FROM technician_team_members WHERE team_id = ?", teamId);
            }
            return;
        }

        Set<Long> existingTechnicians = fetchExistingTechnicianIdsTm(desiredIds);
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

    private Set<Long> fetchExistingTechnicianIdsTm(Set<Long> technicianIds) {
        if (technicianIds.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(technicianIds.size(), "?"));
        String sql = "SELECT id FROM technicians WHERE is_deleted = 0 AND id IN (" + placeholders + ")";
        List<Map<String, Object>> rows = tmJdbcTemplate.queryForList(sql, technicianIds.toArray());
        Set<Long> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.add(asLong(row.get("id")));
        }
        return ids;
    }

    private boolean existsEmailForOtherTechnicianTm(String email, Long currentTechnicianId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(email) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(email) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLongTm(sql, email)
                : queryLongTm(sql, email, currentTechnicianId);
        return count > 0;
    }

    private String requireBadgeUniqueTm(String badgeNumber, Long currentTechnicianId) {
        String trimmed = requireNonBlank(badgeNumber, "badgeNumber is required");
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(badge_number) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(badge_number) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLongTm(sql, trimmed)
                : queryLongTm(sql, trimmed, currentTechnicianId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "badgeNumber already exists");
        }
        return trimmed;
    }

    private void ensureTechnicianIdUniqueTm(String technicianId, Long currentTechnicianId) {
        String sql = currentTechnicianId == null
                ? "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(technician_id) = LOWER(?)"
                : "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(technician_id) = LOWER(?) AND id <> ?";
        long count = currentTechnicianId == null
                ? queryLongTm(sql, technicianId)
                : queryLongTm(sql, technicianId, currentTechnicianId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "technicianId already exists");
        }
    }

    private String determineTechnicianIdTm(String providedTechnicianId, Long currentTechnicianId) {
        if (providedTechnicianId != null && !providedTechnicianId.isBlank()) {
            String trimmed = providedTechnicianId.trim();
            ensureTechnicianIdUniqueTm(trimmed, currentTechnicianId);
            return trimmed;
        }

        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 30; i++) {
            int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("TECH-%s-%04d", date, random);
            long count = queryLongTm(
                    "SELECT COUNT(1) FROM technicians WHERE is_deleted = 0 AND LOWER(technician_id) = LOWER(?)",
                    candidate
            );
            if (count == 0) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate technicianId");
    }

    private void ensureTechnicianExistsTm(Long technicianId) {
        long count = queryLongTm("SELECT COUNT(1) FROM technicians WHERE id = ? AND is_deleted = 0", technicianId);
        if (count == 0) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
    }

    private long queryLongTm(String sql, Object... args) {
        Number value = tmJdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    private LocalDate resolveTerminationDate(String technicianType, LocalDate terminationDate) {
        if ("CONTRACT".equalsIgnoreCase(technicianType)) {
            return terminationDate;
        }
        if (terminationDate != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "terminationDate allowed only for CONTRACT technicians");
        }
        return null;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpperDefault(String value, String fallback) {
        String resolved = safeTrim(value);
        if (resolved == null) {
            return fallback;
        }
        return resolved.toUpperCase();
    }

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

    private String leavesSelect() {
        return """
                SELECT l.id, l.technician_id, l.start_date, l.end_date, l.reason, l.created_at,
                       COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, '')))) AS technician_name
                FROM technician_leaves l
                JOIN technicians t ON t.id = l.technician_id
                """;
    }

    private TechnicianDetailsResponse mapTechnicianTm(Map<String, Object> row) {
        Long technicianId = asLong(row.get("id"));
        List<TechnicianTeamMembershipResponse> memberships = tmJdbcTemplate.queryForList("""
                SELECT m.team_id, tt.team_name, m.team_leader
                FROM technician_team_members m
                JOIN technician_teams tt ON tt.id = m.team_id
                WHERE m.technician_id = ?
                """, technicianId).stream().map(member -> {
            Long teamId = asLong(member.get("team_id"));
            List<String> leaderNames = tmJdbcTemplate.query("""
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

    private TechnicianTeamDetailsResponse mapTeamTm(Map<String, Object> row) {
        Long teamId = asLong(row.get("id"));
        List<Map<String, Object>> technicianRows = tmJdbcTemplate.queryForList("""
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
        List<TechnicianDetailsResponse> technicians = technicianRows.stream().map(this::mapTechnicianTm).toList();

        List<Map<String, Object>> leaderRows = tmJdbcTemplate.queryForList("""
                SELECT TOP 1 t.id AS leader_id,
                       COALESCE(NULLIF(LTRIM(RTRIM(t.full_name)), ''),
                                LTRIM(RTRIM(COALESCE(t.first_name, '') + ' ' + COALESCE(t.last_name, '')))) AS leader_name
                FROM technician_team_members m
                JOIN technicians t ON t.id = m.technician_id
                WHERE m.team_id = ? AND m.team_leader = 1
                """, teamId);
        Long teamLeaderId = leaderRows.isEmpty() ? null : asLong(leaderRows.get(0).get("leader_id"));
        String teamLeaderName = leaderRows.isEmpty() ? null : asString(leaderRows.get(0).get("leader_name"));

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
                .availability("Available")
                .technicians(technicians)
                .build();
    }

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

    private List<DailyAvailabilityDto> buildAvailability(Long technicianId, Integer days) {
        int horizon = days == null ? 31 : Math.max(days, 1);
        LocalDate start = LocalDate.now();
        LocalDate endExclusive = start.plusDays(horizon);

        Map<LocalDate, String> statusByDate = new HashMap<>();
        for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
            statusByDate.put(d, d.getDayOfWeek().getValue() == 7 ? "HOLIDAY" : "AVAILABLE");
        }

        List<LocalDate> holidays = jdbcTemplate.query("""
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

        List<Map<String, Object>> leaves = jdbcTemplate.queryForList("""
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

        List<Map<String, Object>> bookings = jdbcTemplate.queryForList("""
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

    private void ensureTechnicianExists(Long technicianId) {
        long count = queryLong("SELECT COUNT(1) FROM technicians WHERE id = ? AND is_deleted = 0", technicianId);
        if (count == 0) {
            throw new ResourceNotFoundException("Technician not found: " + technicianId);
        }
    }

    private long queryLong(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    private boolean columnExists(String tableName, String columnName) {
        long count = queryLong("""
                SELECT COUNT(1)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ?
                  AND LOWER(COLUMN_NAME) = LOWER(?)
                """, tableName, columnName);
        return count > 0;
    }

    private boolean tableExists(String tableName) {
        long count = queryLong("""
                SELECT COUNT(1)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_TYPE = 'BASE TABLE'
                  AND TABLE_NAME = ?
                """, tableName);
        return count > 0;
    }

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

    private TimeWindowDto windowMap(LocalTime start, LocalTime end) {
        return TimeWindowDto.builder()
                .start(start)
                .end(end)
                .build();
    }

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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

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
