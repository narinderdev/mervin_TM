package com.example.tm.timesheet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.timesheet.dto.TimesheetDayRequestDto;
import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import com.example.tm.timesheet.dto.TimesheetRowRequestDto;
import com.example.tm.timesheet.entity.Timesheet;
import com.example.tm.timesheet.repo.TimesheetRepository;
import com.example.tm.timesheet.repo.TimesheetRowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private TimesheetRowRepository timesheetRowRepository;

    @Mock
    private TmUserRepository tmUserRepository;

    @InjectMocks
    private TimesheetServiceImpl timesheetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(timesheetService, "payPeriodGraceDays", 3);
    }

    @Test
    void createMonthlyTimesheetNormalizesViewTypeAndComputesDeadline() {
        TimesheetRequestDto request = buildRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "monthly");

        when(timesheetRepository.existsByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
                request.getTechnicianId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate())).thenReturn(false);
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> {
            Timesheet saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(tmUserRepository.findById(request.getTechnicianId())).thenReturn(Optional.empty());

        TimesheetResponseDto response = timesheetService.create(request, "TECHNICIAN");

        assertEquals("MONTHLY", response.getViewType());
        assertEquals(LocalDate.of(2026, 4, 3), response.getDeadlineDate());

        ArgumentCaptor<Timesheet> timesheetCaptor = ArgumentCaptor.forClass(Timesheet.class);
        verify(timesheetRepository).save(timesheetCaptor.capture());
        assertEquals("MONTHLY", timesheetCaptor.getValue().getViewType());
    }

    @Test
    void createRejectsWeeklyPeriodWhenRangeIsNotSevenDays() {
        TimesheetRequestDto request = buildRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 8),
                "WEEKLY");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> timesheetService.create(request, "TECHNICIAN"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("exactly 7 days"));
    }

    @Test
    void createRejectsAfterDeadlineEvenForAdminRole() {
        LocalDate end = LocalDate.now().minusDays(5);
        LocalDate start = end.minusDays(6);
        TimesheetRequestDto request = buildRequest(start, end, "WEEKLY");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> timesheetService.create(request, "ADMIN"));

        assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Pay period is locked"));
    }

    private TimesheetRequestDto buildRequest(LocalDate start, LocalDate end, String viewType) {
        TimesheetRowRequestDto row = new TimesheetRowRequestDto();
        row.setPayCode("REG");
        row.setHours(new BigDecimal("8.0"));
        row.setAccountingUnit("AU");
        row.setFerc("FERC");
        row.setActivity("Activity");
        row.setComment("Comment");
        row.setIsDeleted(false);

        TimesheetDayRequestDto day = new TimesheetDayRequestDto();
        day.setDate(start);
        day.setDayOfWeek(start.getDayOfWeek().name());
        day.setDailyTotal(new BigDecimal("8.0"));
        day.setRows(List.of(row));

        TimesheetRequestDto request = new TimesheetRequestDto();
        request.setPeriodStartDate(start);
        request.setPeriodEndDate(end);
        request.setViewType(viewType);
        request.setTechnicianId(1L);
        request.setTotalWorked(new BigDecimal("8.0"));
        request.setTotalNonWorked(BigDecimal.ZERO);
        request.setTotalPremium(BigDecimal.ZERO);
        request.setTimesheetDays(List.of(day));
        request.setSaveAsTemplate(false);
        return request;
    }
}
