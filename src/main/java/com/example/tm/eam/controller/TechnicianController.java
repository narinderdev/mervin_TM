package com.example.tm.eam.controller;

import com.example.tm.eam.dto.TechnicianCreateRequest;
import com.example.tm.eam.dto.TechnicianPatchRequest;
import com.example.tm.eam.service.EamLookupService;
import com.example.tm.shared.constants.HeaderConstants;
import com.example.tm.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TechnicianController {

    private final EamLookupService eamLookupService;

    @GetMapping("/dashboard/technicians")
    public ResponseEntity<ApiResponse<?>> getDashboardTechnicians(
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {
        log.info("EAM GET /dashboard/technicians limit={} cid={}", limit, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician dashboard fetched", eamLookupService.getDashboardTechnicians(limit)));
    }

    @PostMapping("/technicians")
    public ResponseEntity<ApiResponse<?>> createTechnician(
            @Valid @RequestBody TechnicianCreateRequest requestBody,
            HttpServletRequest request) {
        log.info("EAM POST /technicians cid={}", correlationId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Technician created successfully",
                eamLookupService.createTechnician(requestBody)));
    }

    @GetMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<?>> getTechnicianById(
            @PathVariable("id") Long technicianId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{} cid={}", technicianId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician fetched successfully",
                eamLookupService.getTechnicianById(technicianId)));
    }

    @PatchMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<?>> patchTechnician(
            @PathVariable("id") Long technicianId,
            @RequestBody TechnicianPatchRequest requestBody,
            HttpServletRequest request) {
        log.info("EAM PATCH /technicians/{} cid={}", technicianId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician updated successfully",
                eamLookupService.patchTechnician(technicianId, requestBody)));
    }

    @DeleteMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<?>> deleteTechnician(
            @PathVariable("id") Long technicianId,
            HttpServletRequest request) {
        log.info("EAM DELETE /technicians/{} cid={}", technicianId, correlationId(request));
        eamLookupService.deleteTechnician(technicianId);
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician deleted successfully",
                null));
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
