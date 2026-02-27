package com.example.tm.timesheet.service;

import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.shared.exception.ResourceNotFoundException;
import com.example.tm.timesheet.dto.TimesheetDayRequestDto;
import com.example.tm.timesheet.dto.TimesheetDayResponseDto;
import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetRowRequestDto;
import com.example.tm.timesheet.dto.TimesheetRowResponseDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import com.example.tm.timesheet.entity.Timesheet;
import com.example.tm.timesheet.entity.TimesheetDay;
import com.example.tm.timesheet.entity.TimesheetRow;
import com.example.tm.timesheet.repo.TimesheetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tmTransactionManager")
public class TimesheetServiceImpl implements TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final TmUserRepository tmUserRepository;

    @Override
    public TimesheetResponseDto create(TimesheetRequestDto requestDto) {
        rejectDuplicatePeriod(requestDto.getTechnicianId(), requestDto.getPeriodStartDate(), requestDto.getPeriodEndDate(), null);

        Timesheet entity = new Timesheet();
        populateEntity(requestDto, entity);
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
    public TimesheetResponseDto update(Long id, TimesheetRequestDto requestDto) {
        Timesheet existing = findByIdOrThrow(id);
        rejectDuplicatePeriod(requestDto.getTechnicianId(), requestDto.getPeriodStartDate(), requestDto.getPeriodEndDate(), id);
        populateEntity(requestDto, existing);

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
    public void delete(Long id) {
        Timesheet existing = findByIdOrThrow(id);
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
        entity.clearDays();
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
                .viewType(entity.getViewType())
                .technicianId(entity.getTechnicianId())
                .technicianFirstName(technicianFirstName)
                .technicianLastName(technicianLastName)
                .technicianName(technicianName)
                .totalWorked(entity.getTotalWorked())
                .totalNonWorked(entity.getTotalNonWorked())
                .totalPremium(entity.getTotalPremium())
                .status(entity.getStatus() == null ? "PENDING" : entity.getStatus())
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

    private void rejectDuplicatePeriod(Long technicianId, java.time.LocalDate start, java.time.LocalDate end, Long currentId) {
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
}
