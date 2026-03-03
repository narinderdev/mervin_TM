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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TechnicianTeamController {

    private final EamLookupService eamLookupService;

    @PostMapping("/technician-teams")
    public ResponseEntity<ApiResponse<?>> createTechnicianTeam(
            @Valid @RequestBody TechnicianTeamCreateRequest requestBody,
            HttpServletRequest request) {
        log.info("EAM POST /technician-teams cid={}", correlationId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Technician team created successfully",
                eamLookupService.createTechnicianTeam(requestBody)));
    }

    @GetMapping("/technician-teams/{id}")
    public ResponseEntity<ApiResponse<?>> getTechnicianTeamById(
            @PathVariable("id") Long teamId,
            HttpServletRequest request) {
        log.info("EAM GET /technician-teams/{} cid={}", teamId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician team fetched successfully",
                eamLookupService.getTechnicianTeamById(teamId)));
    }

    @PatchMapping("/technician-teams/{id}")
    public ResponseEntity<ApiResponse<?>> patchTechnicianTeam(
            @PathVariable("id") Long teamId,
            @RequestBody TechnicianTeamPatchRequest requestBody,
            HttpServletRequest request) {
        log.info("EAM PATCH /technician-teams/{} cid={}", teamId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician team updated successfully",
                eamLookupService.patchTechnicianTeam(teamId, requestBody)));
    }

    @DeleteMapping("/technician-teams/{id}")
    public ResponseEntity<ApiResponse<?>> deleteTechnicianTeam(
            @PathVariable("id") Long teamId,
            HttpServletRequest request) {
        log.info("EAM DELETE /technician-teams/{} cid={}", teamId, correlationId(request));
        eamLookupService.deleteTechnicianTeam(teamId);
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Technician team deleted successfully",
                null));
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

    private String correlationId(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(HeaderConstants.CORRELATION_ID_HEADER);
        if (fromRequest instanceof String value && !value.isBlank()) {
            return value;
        }
        String fromHeader = request.getHeader(HeaderConstants.CORRELATION_ID_HEADER);
        return fromHeader == null ? "" : fromHeader;
    }
}
