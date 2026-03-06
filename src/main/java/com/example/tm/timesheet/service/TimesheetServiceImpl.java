package com.example.tm.timesheet.service;

import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.shared.exception.ResourceNotFoundException;
import com.example.tm.timesheet.dto.TimesheetDayRequestDto;
import com.example.tm.timesheet.dto.TimesheetDayResponseDto;
import com.example.tm.timesheet.dto.TimesheetRecentEntryResponseDto;
import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetRowRequestDto;
import com.example.tm.timesheet.dto.TimesheetRowResponseDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import com.example.tm.timesheet.entity.Timesheet;
import com.example.tm.timesheet.entity.TimesheetDay;
import com.example.tm.timesheet.entity.TimesheetRow;
import com.example.tm.timesheet.repo.TimesheetRowRepository;
import com.example.tm.timesheet.repo.TimesheetRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tmTransactionManager")
public class TimesheetServiceImpl implements TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final TimesheetRowRepository timesheetRowRepository;
    private final TmUserRepository tmUserRepository;

    @Value("${timesheet.pay-period-grace-days:2}")
    private int payPeriodGraceDays;

    @Override
    public TimesheetResponseDto create(TimesheetRequestDto requestDto, String actorRole) {
        requireActorRole(actorRole);
        validatePeriodRange(requestDto.getPeriodStartDate(), requestDto.getPeriodEndDate());
        validateDayDatesWithinPeriod(requestDto);
        if (isTechnicianRole(actorRole)) {
            rejectIfLockedByDeadline(computeDeadlineDate(requestDto.getPeriodEndDate()));
        }
        rejectDuplicatePeriod(requestDto.getTechnicianId(), requestDto.getPeriodStartDate(), requestDto.getPeriodEndDate(), null);

        Timesheet entity = new Timesheet();
        populateEntity(requestDto, entity);
        applyPayPeriodDates(entity);
        entity.setStatus("PENDING");

        return toResponse(timesheetRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public List<TimesheetResponseDto> getAll() {
        return timesheetRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public TimesheetResponseDto getById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    public TimesheetResponseDto update(Long id, TimesheetRequestDto requestDto, String actorRole) {
        requireActorRole(actorRole);
        Timesheet existing = findByIdOrThrow(id);
        if (isTechnicianRole(actorRole)) {
            rejectIfLockedByDeadline(resolveDeadlineDate(existing));
        }
        validatePeriodRange(requestDto.getPeriodStartDate(), requestDto.getPeriodEndDate());
        validateDayDatesWithinPeriod(requestDto);

        boolean periodChanged = !requestDto.getPeriodStartDate().equals(existing.getPeriodStartDate())
                || !requestDto.getPeriodEndDate().equals(existing.getPeriodEndDate());
        if (periodChanged && isTechnicianRole(actorRole)) {
            rejectIfLockedByDeadline(computeDeadlineDate(requestDto.getPeriodEndDate()));
        }

        rejectDuplicatePeriod(requestDto.getTechnicianId(), requestDto.getPeriodStartDate(), requestDto.getPeriodEndDate(), id);
        populateEntity(requestDto, existing);
        applyPayPeriodDates(existing);

        return toResponse(timesheetRepository.save(existing));
    }

    @Override
    public TimesheetResponseDto approve(Long id) {
        Timesheet existing = findByIdOrThrow(id);
        existing.setStatus("APPROVED");
        return toResponse(timesheetRepository.save(existing));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public List<TimesheetResponseDto> getByTechnician(Long technicianId) {
        return timesheetRepository.findByTechnicianId(technicianId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public TimesheetRecentEntryResponseDto getRecentEntryByTechnician(Long technicianId) {
        if (technicianId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianId is required");
        }

        List<TimesheetRow> rows = timesheetRowRepository.findRecentRowsByTechnicianId(technicianId, PageRequest.of(0, 1));
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No saved template");
        }

        TimesheetRow row = rows.get(0);
        return TimesheetRecentEntryResponseDto.builder()
                .technicianId(technicianId)
                .project(row.getActivity())
                .payCode(row.getPayCode())
                .department(row.getAccountingUnit())
                .account(row.getFerc())
                .totalHours(row.getHours())
                .build();
    }

    @Override
    public void delete(Long id, String actorRole) {
        requireActorRole(actorRole);
        Timesheet existing = findByIdOrThrow(id);
        if (isTechnicianRole(actorRole)) {
            rejectIfLockedByDeadline(resolveDeadlineDate(existing));
        }
        timesheetRepository.delete(existing);
    }

    private Timesheet findByIdOrThrow(Long id) {
        return timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found: " + id));
    }

    private void populateEntity(TimesheetRequestDto requestDto, Timesheet entity) {
        entity.setPeriodStartDate(requestDto.getPeriodStartDate());
        entity.setPeriodEndDate(requestDto.getPeriodEndDate());
        entity.setViewType(requestDto.getViewType());
        entity.setTechnicianId(requestDto.getTechnicianId());
        entity.setTotalWorked(requestDto.getTotalWorked());
        entity.setTotalNonWorked(requestDto.getTotalNonWorked());
        entity.setTotalPremium(requestDto.getTotalPremium());
        entity.setSaveAsTemplate(Boolean.TRUE.equals(requestDto.getSaveAsTemplate()));
        // For updates, remove existing days and flush so unique (timesheet_id, date) constraints
        // are cleared before inserting replacement rows.
        if (entity.getId() != null && !entity.getTimesheetDays().isEmpty()) {
            entity.clearDays();
            timesheetRepository.saveAndFlush(entity);
        } else {
            entity.clearDays();
        }
        requestDto.getTimesheetDays().forEach(dayDto -> {
            TimesheetDay day = toDayEntity(dayDto);
            dayDto.getRows()
                    .stream()
                    .map(rowDto -> toRowEntity(day, rowDto))
                    .forEach(day::addRow);
            entity.addDay(day);
        });
    }

    private TimesheetResponseDto toResponse(Timesheet entity) {
        TmUser technician = entity.getTechnicianId() == null
                ? null
                : tmUserRepository.findById(entity.getTechnicianId()).orElse(null);

        String technicianFirstName = technician == null ? null : technician.getFirstName();
        String technicianLastName = technician == null ? null : technician.getLastName();
        String technicianName = (technicianFirstName == null && technicianLastName == null)
                ? null
                : String.join(" ", technicianFirstName == null ? "" : technicianFirstName,
                        technicianLastName == null ? "" : technicianLastName).trim();

        List<TimesheetDayResponseDto> days = entity.getTimesheetDays()
                .stream()
                .map(this::toDayResponse)
                .toList();

        return TimesheetResponseDto.builder()
                .id(entity.getId())
                .periodStartDate(entity.getPeriodStartDate())
                .periodEndDate(entity.getPeriodEndDate())
                .deadlineDate(resolveDeadlineDate(entity))
                .lockDate(resolveLockDate(entity))
                .payPeriodStatus(resolvePayPeriodStatus(entity))
                .adminUnlocked(Boolean.TRUE.equals(entity.getAdminUnlocked()))
                .viewType(entity.getViewType())
                .technicianId(entity.getTechnicianId())
                .technicianFirstName(technicianFirstName)
                .technicianLastName(technicianLastName)
                .technicianName(technicianName)
                .totalWorked(entity.getTotalWorked())
                .totalNonWorked(entity.getTotalNonWorked())
                .totalPremium(entity.getTotalPremium())
                .status(entity.getStatus() == null ? "PENDING" : entity.getStatus())
                .saveAsTemplate(Boolean.TRUE.equals(entity.getSaveAsTemplate()))
                .timesheetDays(days)
                .build();
    }

    private TimesheetDay toDayEntity(TimesheetDayRequestDto dayDto) {
        TimesheetDay day = new TimesheetDay();
        day.setDate(dayDto.getDate());
        day.setDayOfWeek(dayDto.getDayOfWeek());
        day.setDailyTotal(dayDto.getDailyTotal());
        return day;
    }

    private TimesheetRow toRowEntity(TimesheetDay day, TimesheetRowRequestDto rowDto) {
        TimesheetRow row = new TimesheetRow();
        row.setTimesheetDay(day);
        row.setPayCode(rowDto.getPayCode());
        row.setHours(rowDto.getHours());
        row.setAccountingUnit(rowDto.getAccountingUnit());
        row.setFerc(rowDto.getFerc());
        row.setActivity(rowDto.getActivity());
        row.setComment(rowDto.getComment());
        row.setIsDeleted(rowDto.getIsDeleted());
        return row;
    }

    private TimesheetDayResponseDto toDayResponse(TimesheetDay day) {
        return TimesheetDayResponseDto.builder()
                .date(day.getDate())
                .dayOfWeek(day.getDayOfWeek())
                .dailyTotal(day.getDailyTotal())
                .rows(day.getRows().stream().map(this::toRowResponse).toList())
                .build();
    }

    private TimesheetRowResponseDto toRowResponse(TimesheetRow row) {
        return TimesheetRowResponseDto.builder()
                .id(row.getId())
                .payCode(row.getPayCode())
                .hours(row.getHours())
                .accountingUnit(row.getAccountingUnit())
                .ferc(row.getFerc())
                .activity(row.getActivity())
                .comment(row.getComment())
                .isDeleted(row.getIsDeleted())
                .build();
    }

    private void rejectDuplicatePeriod(Long technicianId, LocalDate start, LocalDate end, Long currentId) {
        boolean exists = timesheetRepository.existsByTechnicianIdAndPeriodStartDateAndPeriodEndDate(technicianId, start, end);
        if (exists) {
            // If updating, allow the same record
            if (currentId != null) {
                Timesheet found = timesheetRepository.findById(currentId).orElse(null);
                if (found != null
                        && technicianId.equals(found.getTechnicianId())
                        && start.equals(found.getPeriodStartDate())
                        && end.equals(found.getPeriodEndDate())) {
                    return;
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Timesheet already exists for period " + start + " to " + end);
        }
    }

    private void validatePeriodRange(LocalDate periodStartDate, LocalDate periodEndDate) {
        if (periodStartDate == null || periodEndDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period_start_date and period_end_date are required");
        }
        if (periodEndDate.isBefore(periodStartDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "period_end_date must be on or after period_start_date");
        }
    }

    private void validateDayDatesWithinPeriod(TimesheetRequestDto requestDto) {
        LocalDate start = requestDto.getPeriodStartDate();
        LocalDate end = requestDto.getPeriodEndDate();
        if (requestDto.getTimesheetDays() == null || requestDto.getTimesheetDays().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timesheet_days must contain at least one day");
        }
        requestDto.getTimesheetDays().forEach(day -> {
            LocalDate date = day.getDate();
            if (date.isBefore(start) || date.isAfter(end)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "timesheet_days date " + date + " is outside pay period " + start + " to " + end);
            }
        });
    }

    private void rejectIfLockedByDeadline(LocalDate deadlineDate) {
        if (deadlineDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deadline_date could not be determined");
        }
        if (LocalDate.now().isAfter(deadlineDate)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Pay period is locked after deadline_date " + deadlineDate);
        }
    }

    private void applyPayPeriodDates(Timesheet entity) {
        LocalDate deadlineDate = computeDeadlineDate(entity.getPeriodEndDate());
        entity.setDeadlineDate(deadlineDate);
        entity.setLockDate(deadlineDate.plusDays(1));
        entity.setAdminUnlocked(Boolean.FALSE);
    }

    private LocalDate computeDeadlineDate(LocalDate periodEndDate) {
        int graceDays = Math.max(payPeriodGraceDays, 0);
        return periodEndDate.plusDays(graceDays);
    }

    private LocalDate resolveDeadlineDate(Timesheet entity) {
        return entity.getDeadlineDate() != null
                ? entity.getDeadlineDate()
                : computeDeadlineDate(entity.getPeriodEndDate());
    }

    private LocalDate resolveLockDate(Timesheet entity) {
        return entity.getLockDate() != null
                ? entity.getLockDate()
                : resolveDeadlineDate(entity).plusDays(1);
    }

    private String resolvePayPeriodStatus(Timesheet entity) {
        return LocalDate.now().isAfter(resolveDeadlineDate(entity)) ? "LOCKED" : "OPEN";
    }

    private void requireActorRole(String actorRole) {
        if (actorRole == null || actorRole.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Role claim not present in token");
        }
    }

    private boolean isTechnicianRole(String actorRole) {
        String normalized = actorRole == null ? "" : actorRole.trim().toUpperCase();
        return normalized.equals("TECHNICIAN") || normalized.contains("TECHNICIAN");
    }
}
