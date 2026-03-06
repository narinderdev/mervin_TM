package com.example.tm.timesheet.controller;

import com.example.tm.auth.security.TmJwtService;
import com.example.tm.timesheet.dto.TimesheetRecentEntryResponseDto;
import com.example.tm.timesheet.dto.TimesheetRequestDto;
import com.example.tm.timesheet.dto.TimesheetResponseDto;
import com.example.tm.timesheet.service.TimesheetService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final TmJwtService tmJwtService;

    @PostMapping
    public ResponseEntity<TimesheetResponseDto> create(
            @Valid @RequestBody TimesheetRequestDto requestDto,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheetService.create(requestDto, actorRole));
    }

    @GetMapping
    public List<TimesheetResponseDto> getAll() {
        return timesheetService.getAll();
    }

    @GetMapping("/{id}")
    public TimesheetResponseDto getById(@PathVariable Long id) {
        return timesheetService.getById(id);
    }

    @PutMapping("/{id}")
    public TimesheetResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody TimesheetRequestDto requestDto,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return timesheetService.update(id, requestDto, actorRole);
    }

    @GetMapping("/technicians/{technicianId}")
    public List<TimesheetResponseDto> getByTechnician(@PathVariable("technicianId") Long technicianId) {
        return timesheetService.getByTechnician(technicianId);
    }

    @GetMapping("/technicians/{technicianId}/recent-entry")
    public TimesheetRecentEntryResponseDto getRecentEntryByTechnician(@PathVariable("technicianId") Long technicianId) {
        return timesheetService.getRecentEntryByTechnician(technicianId);
    }

    @PostMapping("/{id}/approve")
    public TimesheetResponseDto approve(@PathVariable Long id) {
        return timesheetService.approve(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        timesheetService.delete(id, actorRole);
        return ResponseEntity.noContent().build();
    }

    private String extractRoleFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        try {
            return tmJwtService.extractPrimaryRole(token);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }
}
