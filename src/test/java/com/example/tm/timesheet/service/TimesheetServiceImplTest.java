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
import com.example.tm.timesheet.entity.TimesheetDraft;
import com.example.tm.timesheet.entity.TimesheetDraftDay;
import com.example.tm.timesheet.entity.Timesheet;
import com.example.tm.timesheet.repo.TimesheetDraftRepository;
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
    private TimesheetDraftRepository timesheetDraftRepository;

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

    @Test
    void saveDraftUpsertsCurrentPeriodAndMarksStatusDraft() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(4);
        TimesheetRequestDto request = buildRequest(start, end, "weekly");

        when(timesheetDraftRepository.findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
                request.getTechnicianId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate())).thenReturn(Optional.empty());
        when(timesheetDraftRepository.save(any(TimesheetDraft.class))).thenAnswer(invocation -> {
            TimesheetDraft saved = invocation.getArgument(0);
            saved.setId(501L);
            return saved;
        });
        when(tmUserRepository.findById(request.getTechnicianId())).thenReturn(Optional.empty());

        TimesheetResponseDto response = timesheetService.saveDraft(request);

        assertEquals(501L, response.getId());
        assertEquals("DRAFT", response.getStatus());
        assertEquals("WEEKLY", response.getViewType());
        assertEquals(request.getPeriodStartDate(), response.getPeriodStartDate());
        assertEquals(request.getPeriodEndDate(), response.getPeriodEndDate());
    }

    @Test
    void saveDraftRejectsWhenPeriodIsNotCurrent() {
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end = start.plusDays(6);
        TimesheetRequestDto request = buildRequest(start, end, "WEEKLY");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> timesheetService.saveDraft(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("currently selected pay period"));
    }

    @Test
    void saveDraftAllowsMissingExpenseCodeWithoutEntryTypeValidation() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(4);
        TimesheetRequestDto request = buildRequest(start, end, "WEEKLY");

        TimesheetRowRequestDto row = request.getTimesheetDays().get(0).getRows().get(0);
        row.setWorkOrderType("CAPEX");
        row.setPayCode(null);
        row.setHours(null);
        row.setExpenseCode(null);
        row.setCompanyNumber("100200");

        when(timesheetDraftRepository.findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
                request.getTechnicianId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate())).thenReturn(Optional.empty());
        when(timesheetDraftRepository.save(any(TimesheetDraft.class))).thenAnswer(invocation -> {
            TimesheetDraft saved = invocation.getArgument(0);
            saved.setId(501L);
            return saved;
        });
        when(tmUserRepository.findById(request.getTechnicianId())).thenReturn(Optional.empty());

        TimesheetResponseDto response = timesheetService.saveDraft(request);
        assertEquals("CAPEX", response.getTimesheetDays().get(0).getRows().get(0).getWorkOrderType());
        assertEquals("100200", response.getTimesheetDays().get(0).getRows().get(0).getCompanyNumber());
    }

    @Test
    void saveDraftTrimsWorkOrderTypeAndCompanyNumber() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(4);
        TimesheetRequestDto request = buildRequest(start, end, "WEEKLY");

        TimesheetRowRequestDto row = request.getTimesheetDays().get(0).getRows().get(0);
        row.setWorkOrderType("  CAPEX  ");
        row.setPayCode("PTO");
        row.setHours(new BigDecimal("12.0"));
        row.setExpenseCode("MEAL");
        row.setCompanyNumber("  34500  ");

        when(timesheetDraftRepository.findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
                request.getTechnicianId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate())).thenReturn(Optional.empty());
        when(timesheetDraftRepository.save(any(TimesheetDraft.class))).thenAnswer(invocation -> {
            TimesheetDraft saved = invocation.getArgument(0);
            saved.setId(501L);
            return saved;
        });
        when(tmUserRepository.findById(request.getTechnicianId())).thenReturn(Optional.empty());

        TimesheetResponseDto response = timesheetService.saveDraft(request);
        assertEquals("CAPEX", response.getTimesheetDays().get(0).getRows().get(0).getWorkOrderType());
        assertEquals("34500", response.getTimesheetDays().get(0).getRows().get(0).getCompanyNumber());
        assertEquals("PTO", response.getTimesheetDays().get(0).getRows().get(0).getPayCode());
        assertEquals(new BigDecimal("12.0"), response.getTimesheetDays().get(0).getRows().get(0).getHours());
        assertEquals("MEAL", response.getTimesheetDays().get(0).getRows().get(0).getExpenseCode());
    }

    @Test
    void saveDraftAllowsNullWorkOrderType() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(4);
        TimesheetRequestDto request = buildRequest(start, end, "WEEKLY");

        TimesheetRowRequestDto row = request.getTimesheetDays().get(0).getRows().get(0);
        row.setWorkOrderType(null);
        row.setPayCode("REG");
        row.setHours(new BigDecimal("8.0"));
        row.setExpenseCode("MEAL");
        row.setCompanyNumber("20000");

        when(timesheetDraftRepository.findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
                request.getTechnicianId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate())).thenReturn(Optional.empty());
        when(timesheetDraftRepository.save(any(TimesheetDraft.class))).thenAnswer(invocation -> {
            TimesheetDraft saved = invocation.getArgument(0);
            saved.setId(502L);
            return saved;
        });
        when(tmUserRepository.findById(request.getTechnicianId())).thenReturn(Optional.empty());

        TimesheetResponseDto response = timesheetService.saveDraft(request);
        assertEquals(null, response.getTimesheetDays().get(0).getRows().get(0).getWorkOrderType());
        assertEquals("MEAL", response.getTimesheetDays().get(0).getRows().get(0).getExpenseCode());
        assertEquals("20000", response.getTimesheetDays().get(0).getRows().get(0).getCompanyNumber());
    }

    @Test
    void saveDraftExistingDraftClearsAndFlushesDaysBeforeInsert() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(4);
        TimesheetRequestDto request = buildRequest(start, end, "WEEKLY");

        TimesheetDraft existing = new TimesheetDraft();
        existing.setId(1L);
        existing.setTechnicianId(1L);
        existing.setPeriodStartDate(start);
        existing.setPeriodEndDate(end);
        TimesheetDraftDay existingDay = new TimesheetDraftDay();
        existingDay.setDate(start);
        existing.addDay(existingDay);

        when(timesheetDraftRepository.findByTechnicianIdAndPeriodStartDateAndPeriodEndDate(
                request.getTechnicianId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate())).thenReturn(Optional.of(existing));
        when(timesheetDraftRepository.saveAndFlush(any(TimesheetDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(timesheetDraftRepository.save(any(TimesheetDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tmUserRepository.findById(request.getTechnicianId())).thenReturn(Optional.empty());

        timesheetService.saveDraft(request);

        verify(timesheetDraftRepository).saveAndFlush(existing);
    }

    @Test
    void sendBackByAdminSetsStatusSentBack() {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(10L);
        timesheet.setStatus("PENDING");
        timesheet.setPeriodStartDate(LocalDate.of(2026, 3, 1));
        timesheet.setPeriodEndDate(LocalDate.of(2026, 3, 7));
        timesheet.setTechnicianId(1L);

        when(timesheetRepository.findById(10L)).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tmUserRepository.findById(1L)).thenReturn(Optional.empty());

        TimesheetResponseDto response = timesheetService.sendBack(10L, "ADMIN");

        assertEquals("SENT_BACK", response.getStatus());
    }

    @Test
    void sendBackByTechnicianIsForbidden() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> timesheetService.sendBack(10L, "TECHNICIAN"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Only admin"));
    }

    private TimesheetRequestDto buildRequest(LocalDate start, LocalDate end, String viewType) {
        TimesheetRowRequestDto row = new TimesheetRowRequestDto();
        row.setPayCode("REG");
        row.setHours(new BigDecimal("8.0"));
        row.setAccountingUnit("AU");
        row.setFerc("FERC");
        row.setActivity("Activity");
        row.setComment("Comment");
        row.setWorkOrderType("TIME");
        row.setCompanyNumber("1000");
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
