package com.example.tm.team.controller;

import com.example.tm.team.dto.TmTeamCreateRequest;
import com.example.tm.team.dto.TmTeamDetailsResponse;
import com.example.tm.team.service.TmTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TmTeamController {

    private final TmTeamService teamService;

    @PostMapping
    public ResponseEntity<TmTeamDetailsResponse> create(@Valid @RequestBody TmTeamCreateRequest request) {
        TmTeamDetailsResponse response = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
