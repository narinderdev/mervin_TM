package com.example.tm.eam.controller;

import com.example.tm.eam.dto.TechnicianTeamCreateRequest;
import com.example.tm.eam.dto.TechnicianTeamPatchRequest;
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

/**
 * Exposes API endpoints for technician team controller.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TechnicianTeamController {

    private final EamLookupService eamLookupService;

    /** Creates technician team. */
    @PostMapping("/technician-teams")
    public ResponseEntity<ApiResponse<?>> createTechnicianTeam(
            @RequestParam("companyId") Long companyId,
            @Valid @RequestBody TechnicianTeamCreateRequest requestBody,
            HttpServletRequest request) {
        requestBody.setCompanyId(companyId);
        log.info("EAM POST /technician-teams companyId={} cid={}", companyId, correlationId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Technician team created successfully",
                eamLookupService.createTechnicianTeam(requestBody)));
    }

    /** Returns technician team by id. */
    @GetMapping("/technician-teams/{id}")
    public ResponseEntity<ApiResponse<?>> getTechnicianTeamById(
            @PathVariable("id") Long teamId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technician-teams/{} companyId={} cid={}", teamId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician team fetched successfully",
                eamLookupService.getTechnicianTeamById(teamId, companyId)));
    }

    /** Handles patch technician team. */
    @PatchMapping("/technician-teams/{id}")
    public ResponseEntity<ApiResponse<?>> patchTechnicianTeam(
            @PathVariable("id") Long teamId,
            @RequestParam("companyId") Long companyId,
            @RequestBody TechnicianTeamPatchRequest requestBody,
            HttpServletRequest request) {
        log.info("EAM PATCH /technician-teams/{} companyId={} cid={}", teamId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician team updated successfully",
                eamLookupService.patchTechnicianTeam(teamId, companyId, requestBody)));
    }

    /** Deletes technician team. */
    @DeleteMapping("/technician-teams/{id}")
    public ResponseEntity<ApiResponse<?>> deleteTechnicianTeam(
            @PathVariable("id") Long teamId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM DELETE /technician-teams/{} companyId={} cid={}", teamId, companyId, correlationId(request));
        eamLookupService.deleteTechnicianTeam(teamId, companyId);
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician team deleted successfully",
                null));
    }

    /** Returns technician teams. */
    @GetMapping("/technician-teams")
    public ResponseEntity<ApiResponse<?>> getTechnicianTeams(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /technician-teams page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Technician teams fetched successfully", eamLookupService.getTechnicianTeams(page, size, companyId)));
    }

    /** Handles correlation id. */
    private String correlationId(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(HeaderConstants.CORRELATION_ID_HEADER);
        if (fromRequest instanceof String value && !value.isBlank()) {
            return value;
        }
        String fromHeader = request.getHeader(HeaderConstants.CORRELATION_ID_HEADER);
        return fromHeader == null ? "" : fromHeader;
    }
}
