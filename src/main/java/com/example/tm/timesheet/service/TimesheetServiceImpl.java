package com.example.tm.timesheet.service;

import com.example.tm.shared.exception.ResourceNotFoundException;
import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetRowRequestDto;
import com.example.tm.timesheet.dto.TimesheetRowResponseDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import com.example.tm.timesheet.entity.Timesheet;
import com.example.tm.timesheet.entity.TimesheetRow;
import com.example.tm.timesheet.repo.TimesheetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tmTransactionManager")
public class TimesheetServiceImpl implements TimesheetService {

    private final TimesheetRepository timesheetRepository;

    @Override
    public TimesheetResponseDto create(TimesheetRequestDto requestDto) {
        Timesheet entity = new Timesheet();
        populateEntity(requestDto, entity);

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
        populateEntity(requestDto, existing);

        return toResponse(timesheetRepository.save(existing));
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
        entity.clearRows();
        requestDto.getTimesheetRows()
                .stream()
                .map(this::toRowEntity)
                .forEach(entity::addRow);
    }

    private TimesheetRow toRowEntity(TimesheetRowRequestDto rowDto) {
        TimesheetRow row = new TimesheetRow();
        row.setDate(rowDto.getDate());
        row.setDayOfWeek(rowDto.getDayOfWeek());
        row.setPayCode(rowDto.getPayCode());
        row.setHours(rowDto.getHours());
        row.setDailyTotal(rowDto.getDailyTotal());
        row.setAccountingUnit(rowDto.getAccountingUnit());
        row.setFerc(rowDto.getFerc());
        row.setActivity(rowDto.getActivity());
        row.setComment(rowDto.getComment());
        row.setIsDeleted(rowDto.getIsDeleted());
        return row;
    }

    private TimesheetResponseDto toResponse(Timesheet entity) {
        return TimesheetResponseDto.builder()
                .id(entity.getId())
                .periodStartDate(entity.getPeriodStartDate())
                .periodEndDate(entity.getPeriodEndDate())
                .viewType(entity.getViewType())
                .technicianId(entity.getTechnicianId())
                .totalWorked(entity.getTotalWorked())
                .totalNonWorked(entity.getTotalNonWorked())
                .totalPremium(entity.getTotalPremium())
                .timesheetRows(entity.getTimesheetRows()
                        .stream()
                        .map(this::toRowResponse)
                        .toList())
                .build();
    }

    private TimesheetRowResponseDto toRowResponse(TimesheetRow row) {
        return TimesheetRowResponseDto.builder()
                .id(row.getId())
                .date(row.getDate())
                .dayOfWeek(row.getDayOfWeek())
                .payCode(row.getPayCode())
                .hours(row.getHours())
                .dailyTotal(row.getDailyTotal())
                .accountingUnit(row.getAccountingUnit())
                .ferc(row.getFerc())
                .activity(row.getActivity())
                .comment(row.getComment())
                .isDeleted(row.getIsDeleted())
                .build();
    }
}
