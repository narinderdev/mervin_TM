package com.example.tm.eam.controller;

import com.example.tm.eam.service.EamLookupService;
import com.example.tm.shared.constants.HeaderConstants;
import com.example.tm.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class EamLookupController {

    private final EamLookupService eamLookupService;

    @GetMapping("/dashboard/technicians")
    public ResponseEntity<ApiResponse<?>> getDashboardTechnicians(
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {
        log.info("EAM GET /dashboard/technicians limit={} cid={}", limit, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician dashboard fetched", eamLookupService.getDashboardTechnicians(limit)));
    }

    @GetMapping("/technicians")
    public ResponseEntity<ApiResponse<?>> getTechnicians(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("EAM GET /technicians page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technicians fetched successfully", eamLookupService.getTechnicians(page, size)));
    }

    @GetMapping("/technicians/{id}/availability/monthly")
    public ResponseEntity<ApiResponse<?>> getTechnicianAvailabilityMonthly(
            @PathVariable("id") Long technicianId,
            @RequestParam(value = "days", required = false) Integer days,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{}/availability/monthly days={} cid={}", technicianId, days, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician monthly availability fetched",
                eamLookupService.getTechnicianAvailabilityMonthly(technicianId, days)));
    }

    @GetMapping("/technician-teams")
    public ResponseEntity<ApiResponse<?>> getTechnicianTeams(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("EAM GET /technician-teams page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician teams fetched successfully", eamLookupService.getTechnicianTeams(page, size)));
    }

    @GetMapping("/work-orders")
    public ResponseEntity<ApiResponse<?>> getWorkOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work orders fetched successfully", eamLookupService.getWorkOrders(page, size)));
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<ApiResponse<?>> getWorkOrderById(
            @PathVariable("id") Long workOrderId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/{} cid={}", workOrderId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work order details fetched successfully", eamLookupService.getWorkOrderById(workOrderId)));
    }

    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<?>> getHolidays(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            HttpServletRequest request) {
        log.info("EAM GET /holidays page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holidays fetched successfully", eamLookupService.getHolidays(page, size)));
    }

    @GetMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<?>> getHolidayById(
            @PathVariable("id") Long holidayId,
            HttpServletRequest request) {
        log.info("EAM GET /holidays/{} cid={}", holidayId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holiday fetched successfully", eamLookupService.getHolidayById(holidayId)));
    }

    @GetMapping("/technicians/leaves")
    public ResponseEntity<ApiResponse<?>> getTechniciansLeaves(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/leaves page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "All technician leaves fetched successfully", eamLookupService.getTechniciansLeaves(page, size)));
    }

    @GetMapping("/technicians/{id}/leaves")
    public ResponseEntity<ApiResponse<?>> getTechnicianLeaves(
            @PathVariable("id") Long technicianId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{}/leaves cid={}", technicianId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician leaves fetched successfully", eamLookupService.getTechnicianLeaves(technicianId)));
    }

    @GetMapping("/technicians/{id}/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<?>> getTechnicianLeaveById(
            @PathVariable("id") Long technicianId,
            @PathVariable("leaveId") Long leaveId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{}/leaves/{} cid={}", technicianId, leaveId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician leave fetched successfully", eamLookupService.getTechnicianLeaveById(technicianId, leaveId)));
    }

    private String correlationId(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(HeaderConstants.CORRELATION_ID_HEADER);
        if (fromRequest instanceof String value && !value.isBlank()) {
            return value;
        }
        String fromHeader = request.getHeader(HeaderConstants.CORRELATION_ID_HEADER);
        return fromHeader == null ? "" : fromHeader;
    }
}
