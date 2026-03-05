package com.example.tm.timesheet.service;

import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import java.util.List;

public interface TimesheetService {

    TimesheetResponseDto create(TimesheetRequestDto requestDto, String actorRole);

    List<TimesheetResponseDto> getAll();

    TimesheetResponseDto getById(Long id);

    TimesheetResponseDto update(Long id, TimesheetRequestDto requestDto, String actorRole);

    void delete(Long id, String actorRole);

    TimesheetResponseDto approve(Long id);

    List<TimesheetResponseDto> getByTechnician(Long technicianId);
}
