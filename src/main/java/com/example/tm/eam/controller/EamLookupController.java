package com.example.tm.eam.controller;

import com.example.tm.eam.service.EamLookupService;
import com.example.tm.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
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
public class EamLookupController {

    private final EamLookupService eamLookupService;

    @GetMapping("/dashboard/technicians")
    public ResponseEntity<ApiResponse<?>> getDashboardTechnicians(
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician dashboard fetched", eamLookupService.getDashboardTechnicians(limit)));
    }

    @GetMapping("/technicians")
    public ResponseEntity<ApiResponse<?>> getTechnicians(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technicians fetched successfully", eamLookupService.getTechnicians(page, size)));
    }

    @GetMapping("/technicians/{id}/availability/monthly")
    public ResponseEntity<ApiResponse<?>> getTechnicianAvailabilityMonthly(
            @PathVariable("id") Long technicianId,
            @RequestParam(value = "days", required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician monthly availability fetched",
                eamLookupService.getTechnicianAvailabilityMonthly(technicianId, days)));
    }

    @GetMapping("/technician-teams")
    public ResponseEntity<ApiResponse<?>> getTechnicianTeams(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician teams fetched successfully", eamLookupService.getTechnicianTeams(page, size)));
    }

    @GetMapping("/work-orders")
    public ResponseEntity<ApiResponse<?>> getWorkOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work orders fetched successfully", eamLookupService.getWorkOrders(page, size)));
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<ApiResponse<?>> getWorkOrderById(@PathVariable("id") Long workOrderId) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work order details fetched successfully", eamLookupService.getWorkOrderById(workOrderId)));
    }

    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<?>> getHolidays(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holidays fetched successfully", eamLookupService.getHolidays(page, size)));
    }

    @GetMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<?>> getHolidayById(@PathVariable("id") Long holidayId) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holiday fetched successfully", eamLookupService.getHolidayById(holidayId)));
    }

    @GetMapping("/technicians/leaves")
    public ResponseEntity<ApiResponse<?>> getTechniciansLeaves(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "All technician leaves fetched successfully", eamLookupService.getTechniciansLeaves(page, size)));
    }

    @GetMapping("/technicians/{id}/leaves")
    public ResponseEntity<ApiResponse<?>> getTechnicianLeaves(@PathVariable("id") Long technicianId) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician leaves fetched successfully", eamLookupService.getTechnicianLeaves(technicianId)));
    }

    @GetMapping("/technicians/{id}/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<?>> getTechnicianLeaveById(
            @PathVariable("id") Long technicianId,
            @PathVariable("leaveId") Long leaveId) {
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician leave fetched successfully", eamLookupService.getTechnicianLeaveById(technicianId, leaveId)));
    }
}
