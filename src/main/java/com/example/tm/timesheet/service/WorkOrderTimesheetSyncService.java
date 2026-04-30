package com.example.tm.timesheet.service;

import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.timesheet.dto.WorkOrderTimesheetSyncResponseDto;
import com.example.tm.timesheet.entity.TimesheetDraft;
import com.example.tm.timesheet.entity.TimesheetDraftDay;
import com.example.tm.timesheet.entity.TimesheetDraftRow;
import com.example.tm.timesheet.repo.TimesheetDraftRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles automatic timesheet sync from completed work orders.
 */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tmTransactionManager")
public class WorkOrderTimesheetSyncService {

    private static final String VIEW_TYPE_MONTHLY = "MONTHLY";
    private static final String DEFAULT_PAY_CODE = "REGULAR";
    private static final BigDecimal MINIMUM_NON_ZERO_HOURS = new BigDecimal("0.01");

    private final TimesheetDraftRepository timesheetDraftRepository;
    private final TmUserRepository tmUserRepository;

    @Qualifier("eamJdbcTemplate")
    private final JdbcTemplate eamJdbcTemplate;

    /** Handles sync completed work order. */
    public WorkOrderTimesheetSyncResponseDto syncCompletedWorkOrder(Long workOrderId, Long companyId) {
        if (workOrderId == null || workOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workOrderId is required");
        }

        WorkOrderContext context = getWorkOrderContext(workOrderId, companyId);
        if (!"COMPLETED".equalsIgnoreCase(context.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Timesheet sync is allowed only for completed work orders");
        }
        validateWorkOrderSyncFields(context);

        List<DailyWorkLog> dailyLogs = getDailyLogs(workOrderId, companyId);
        if (dailyLogs.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No day-wise technician logs found for completed work order " + workOrderId);
        }

        Set<Long> timesheetDraftIds = new LinkedHashSet<>();
        Set<Long> tmUserIds = new LinkedHashSet<>();

        for (DailyWorkLog dailyLog : dailyLogs) {
            tmUserIds.add(dailyLog.tmUserId());
            TimesheetDraft draft = resolveOrCreateDraft(dailyLog.tmUserId(), dailyLog.workDate());
            TimesheetDraftDay day = resolveOrCreateDay(draft, dailyLog.workDate());
            upsertWorkOrderRow(day, context, dailyLog);
            recomputeDraftTotals(draft);
            TimesheetDraft saved = timesheetDraftRepository.save(draft);
            if (saved.getId() != null) {
                timesheetDraftIds.add(saved.getId());
            }
        }

        return WorkOrderTimesheetSyncResponseDto.builder()
                .workOrderId(context.workOrderId())
                .workOrderNumber(context.workOrderNumber())
                .rowsUpserted(dailyLogs.size())
                .timesheetsUpdated(timesheetDraftIds.size())
                .techniciansUpdated(tmUserIds.size())
                .build();
    }

    /** Handles get work order context. */
    private WorkOrderContext getWorkOrderContext(Long workOrderId, Long companyId) {
        String baseSql = """
                SELECT wo.id,
                       wo.work_order_number,
                       wo.work_type,
                       COALESCE(NULLIF(LTRIM(RTRIM(wo.labor_utility_account)), ''),
                                NULLIF(LTRIM(RTRIM(wo.utility_account)), '')) AS department,
                       COALESCE(NULLIF(LTRIM(RTRIM(wo.labor_gl_account)), ''),
                                NULLIF(LTRIM(RTRIM(wo.gl_account)), '')) AS account,
                       wo.status
                FROM work_orders wo
                WHERE wo.deleted = 0
                  AND wo.id = ?
                """;
        List<Map<String, Object>> rows = companyId == null
                ? eamJdbcTemplate.queryForList(baseSql, workOrderId)
                : eamJdbcTemplate.queryForList(baseSql + " AND wo.company_id = ?", workOrderId, companyId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order not found: " + workOrderId);
        }
        Map<String, Object> row = rows.get(0);
        return new WorkOrderContext(
                asLong(row.get("id")),
                asString(row.get("work_order_number")),
                asString(row.get("work_type")),
                asString(row.get("department")),
                asString(row.get("account")),
                asString(row.get("status")));
    }

    /** Handles get daily logs. */
    private List<DailyWorkLog> getDailyLogs(Long workOrderId, Long companyId) {
        String baseSql = """
                SELECT l.technician_id,
                       t.email AS technician_email,
                       l.work_date,
                       l.working_seconds,
                       t.company_id
                FROM work_order_technician_daily_logs l
                JOIN technicians t ON t.id = l.technician_id
                WHERE l.work_order_id = ?
                  AND COALESCE(l.working_seconds, 0) > 0
                  AND COALESCE(t.is_deleted, 0) = 0
                """;
        String orderBySql = " ORDER BY l.technician_id ASC, l.work_date ASC";

        List<Map<String, Object>> rows = companyId == null
                ? eamJdbcTemplate.queryForList(baseSql + orderBySql, workOrderId)
                : eamJdbcTemplate.queryForList(baseSql + " AND t.company_id = ?" + orderBySql, workOrderId, companyId);

        Map<String, Long> tmUserIdByEmail = new HashMap<>();
        return rows.stream()
                .map(row -> new DailyWorkLog(
                        resolveTmUserId(
                                asLong(row.get("technician_id")),
                                asString(row.get("technician_email")),
                                tmUserIdByEmail),
                        asLocalDate(row.get("work_date")),
                        toHours(asLong(row.get("working_seconds"))),
                        asString(row.get("company_id"))))
                .filter(entry -> entry.tmUserId() != null && entry.workDate() != null && entry.hours() != null)
                .toList();
    }

    /** Resolves tm user id from technician email for sync ownership. */
    private Long resolveTmUserId(Long sourceTechnicianId, String technicianEmail, Map<String, Long> tmUserIdByEmail) {
        String normalizedEmail = technicianEmail == null ? null : technicianEmail.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Technician " + sourceTechnicianId + " has no email; cannot map to TM user for timesheet sync");
        }
        Long cached = tmUserIdByEmail.get(normalizedEmail);
        if (cached != null) {
            return cached;
        }
        Long tmUserId = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(TmUser::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "TM user not found for technician " + sourceTechnicianId + " with email " + normalizedEmail));
        tmUserIdByEmail.put(normalizedEmail, tmUserId);
        return tmUserId;
    }

    /** Handles resolve or create draft. */
    private TimesheetDraft resolveOrCreateDraft(Long technicianId, LocalDate workDate) {
        LocalDate periodStart = workDate.withDayOfMonth(1);
        LocalDate periodEnd = workDate.with(TemporalAdjusters.lastDayOfMonth());
        return timesheetDraftRepository
                .findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(technicianId, periodStart, periodEnd)
                .orElseGet(() -> createMonthlyDraft(technicianId, periodStart, periodEnd));
    }

    /** Handles create monthly draft. */
    private TimesheetDraft createMonthlyDraft(Long technicianId, LocalDate periodStart, LocalDate periodEnd) {
        TimesheetDraft draft = new TimesheetDraft();
        draft.setTechnicianId(technicianId);
        draft.setPeriodStartDate(periodStart);
        draft.setPeriodEndDate(periodEnd);
        draft.setViewType(VIEW_TYPE_MONTHLY);
        draft.setSaveAsTemplate(Boolean.FALSE);
        draft.setTotalWorked(BigDecimal.ZERO);
        draft.setTotalNonWorked(BigDecimal.ZERO);
        draft.setTotalPremium(BigDecimal.ZERO);
        return draft;
    }

    /** Handles resolve or create day. */
    private TimesheetDraftDay resolveOrCreateDay(TimesheetDraft draft, LocalDate workDate) {
        return draft.getTimesheetDays().stream()
                .filter(day -> workDate.equals(day.getDate()))
                .findFirst()
                .orElseGet(() -> {
                    TimesheetDraftDay day = new TimesheetDraftDay();
                    day.setDate(workDate);
                    day.setDayOfWeek(workDate.getDayOfWeek().name());
                    day.setDailyTotal(BigDecimal.ZERO);
                    draft.addDay(day);
                    return day;
                });
    }

    /** Handles upsert work order row. */
    private void upsertWorkOrderRow(TimesheetDraftDay day, WorkOrderContext context, DailyWorkLog dailyLog) {
        TimesheetDraftRow row = day.getRows().stream()
                .filter(existing -> Objects.equals(existing.getWorkOrderId(), context.workOrderId()))
                .findFirst()
                .orElseGet(() -> {
                    TimesheetDraftRow created = new TimesheetDraftRow();
                    day.addRow(created);
                    return created;
                });

        row.setPayCode(DEFAULT_PAY_CODE);
        row.setHours(dailyLog.hours());
        row.setAccountingUnit(context.department());
        row.setFerc(context.account());
        row.setActivity(resolveActivity(context));
        row.setComment(null);
        row.setWorkOrderType(context.workType());
        row.setWorkOrderId(context.workOrderId());
        row.setExpenseCode(null);
        row.setCompanyNumber(dailyLog.companyNumber());
        row.setIsDeleted(Boolean.FALSE);
    }

    /** Validates mandatory work order fields required by timesheet row schema. */
    private void validateWorkOrderSyncFields(WorkOrderContext context) {
        if (trimToNull(context.department()) == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Work order " + context.workOrderId() + " is missing department; cannot sync timesheet row");
        }
        if (trimToNull(context.account()) == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Work order " + context.workOrderId() + " is missing account; cannot sync timesheet row");
        }
    }

    /** Handles recompute draft totals. */
    private void recomputeDraftTotals(TimesheetDraft draft) {
        BigDecimal totalWorked = BigDecimal.ZERO;
        for (TimesheetDraftDay day : draft.getTimesheetDays()) {
            BigDecimal dayTotal = sumRows(day.getRows());
            day.setDailyTotal(dayTotal);
            totalWorked = totalWorked.add(dayTotal);
        }
        draft.setTotalWorked(totalWorked);
    }

    /** Handles sum rows. */
    private BigDecimal sumRows(List<TimesheetDraftRow> rows) {
        return rows.stream()
                .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
                .map(TimesheetDraftRow::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Handles resolve activity. */
    private String resolveActivity(WorkOrderContext context) {
        String reference = trimToNull(context.workOrderNumber());
        if (reference == null && context.workOrderId() != null) {
            reference = context.workOrderId().toString();
        }
        if (reference == null) {
            return null;
        }
        return "Work Order #" + reference;
    }

    /** Handles to hours. */
    private BigDecimal toHours(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        BigDecimal hours = BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            return MINIMUM_NON_ZERO_HOURS;
        }
        return hours;
    }

    /** Handles as long. */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }

    /** Handles as local date. */
    private LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    /** Handles as string. */
    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return trimToNull(value.toString());
    }

    /** Handles trim to null. */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record WorkOrderContext(
            Long workOrderId,
            String workOrderNumber,
            String workType,
            String department,
            String account,
            String status) {
    }

    private record DailyWorkLog(
            Long tmUserId,
            LocalDate workDate,
            BigDecimal hours,
            String companyNumber) {
    }
}
