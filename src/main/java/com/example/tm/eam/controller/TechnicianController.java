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
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /dashboard/technicians limit={} companyId={} cid={}", limit, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician dashboard fetched", eamLookupService.getDashboardTechnicians(limit, companyId)));
    }

    @PostMapping("/technicians")
    public ResponseEntity<ApiResponse<?>> createTechnician(
            @RequestParam("companyId") Long companyId,
            @Valid @RequestBody TechnicianCreateRequest requestBody,
            HttpServletRequest request) {
        requestBody.setCompanyId(companyId);
        log.info("EAM POST /technicians companyId={} cid={}", companyId, correlationId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Technician created successfully",
                eamLookupService.createTechnician(requestBody)));
    }

    @GetMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<?>> getTechnicianById(
            @PathVariable("id") Long technicianId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{} companyId={} cid={}", technicianId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician fetched successfully",
                eamLookupService.getTechnicianById(technicianId, companyId)));
    }

    @PatchMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<?>> patchTechnician(
            @PathVariable("id") Long technicianId,
            @RequestParam("companyId") Long companyId,
            @RequestBody TechnicianPatchRequest requestBody,
            HttpServletRequest request) {
        log.info("EAM PATCH /technicians/{} companyId={} cid={}", technicianId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician updated successfully",
                eamLookupService.patchTechnician(technicianId, companyId, requestBody)));
    }

    @DeleteMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<?>> deleteTechnician(
            @PathVariable("id") Long technicianId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM DELETE /technicians/{} companyId={} cid={}", technicianId, companyId, correlationId(request));
        eamLookupService.deleteTechnician(technicianId, companyId);
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician deleted successfully",
                null));
    }

    @GetMapping("/technicians")
    public ResponseEntity<ApiResponse<?>> getTechnicians(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technicians fetched successfully", eamLookupService.getTechnicians(page, size, companyId)));
    }

    @GetMapping("/technicians/{id}/availability/monthly")
    public ResponseEntity<ApiResponse<?>> getTechnicianAvailabilityMonthly(
            @PathVariable("id") Long technicianId,
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{}/availability/monthly days={} companyId={} cid={}", technicianId, days, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician monthly availability fetched",
                eamLookupService.getTechnicianAvailabilityMonthly(technicianId, days, companyId)));
    }

    @GetMapping("/technicians/leaves")
    public ResponseEntity<ApiResponse<?>> getTechniciansLeaves(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/leaves page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "All technician leaves fetched successfully", eamLookupService.getTechniciansLeaves(page, size, companyId)));
    }

    @GetMapping("/technicians/{id}/leaves")
    public ResponseEntity<ApiResponse<?>> getTechnicianLeaves(
            @PathVariable("id") Long technicianId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{}/leaves companyId={} cid={}", technicianId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician leaves fetched successfully", eamLookupService.getTechnicianLeaves(technicianId, companyId)));
    }

    @GetMapping("/technicians/{id}/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<?>> getTechnicianLeaveById(
            @PathVariable("id") Long technicianId,
            @PathVariable("leaveId") Long leaveId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technicians/{}/leaves/{} companyId={} cid={}", technicianId, leaveId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician leave fetched successfully", eamLookupService.getTechnicianLeaveById(technicianId, leaveId, companyId)));
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
