package com.example.tm.timesheet.service;

import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import java.util.List;

public interface TimesheetService {

    TimesheetResponseDto create(TimesheetRequestDto requestDto);

    List<TimesheetResponseDto> getAll();

    TimesheetResponseDto getById(Long id);

    TimesheetResponseDto update(Long id, TimesheetRequestDto requestDto);

    void delete(Long id);

    TimesheetResponseDto approve(Long id);

    List<TimesheetResponseDto> getByTechnician(Long technicianId);
}
