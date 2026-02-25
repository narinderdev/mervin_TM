package com.example.tm.eam.service;

import com.example.tm.eam.dto.DailyAvailabilityDto;
import com.example.tm.eam.dto.TechnicianActivityDto;
import com.example.tm.eam.dto.TechnicianDashboardResponse;
import com.example.tm.eam.dto.TechnicianDetailsResponse;
import com.example.tm.eam.dto.TechnicianHolidayListResponse;
import com.example.tm.eam.dto.TechnicianHolidayResponse;
import com.example.tm.eam.dto.TechnicianLeaveListResponse;
import com.example.tm.eam.dto.TechnicianLeaveResponse;
import com.example.tm.eam.dto.TechnicianListResponse;
import com.example.tm.eam.dto.TechnicianTeamDetailsResponse;
import com.example.tm.eam.dto.TechnicianTeamListResponse;
import com.example.tm.eam.dto.TechnicianTeamMembershipResponse;
import com.example.tm.eam.dto.TimeWindowDto;
import com.example.tm.eam.dto.WorkOrderDetailsResponse;
import com.example.tm.eam.dto.WorkOrderListResponse;
import com.example.tm.shared.exception.ResourceNotFoundException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EamLookupServiceImpl implements EamLookupService {

    @Qualifier("eamJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

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
    public TechnicianListResponse getTechnicians(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLong("SELECT COUNT(1) FROM technicians WHERE is_deleted = 0");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
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

        List<TechnicianDetailsResponse> technicians = rows.stream().map(this::mapTechnician).toList();
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
    public TechnicianTeamListResponse getTechnicianTeams(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        int offset = safePage * safeSize;

        long total = queryLong("SELECT COUNT(1) FROM technician_teams");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, team_name, team_description, status, start_date, end_date, notes
                FROM technician_teams
                ORDER BY id
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, offset, safeSize);
        List<TechnicianTeamDetailsResponse> teams = rows.stream().map(this::mapTeam).toList();
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
}
