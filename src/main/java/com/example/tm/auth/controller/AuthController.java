package com.example.tm.auth.controller;

import com.example.tm.auth.dto.LoginRequestDto;
import com.example.tm.auth.dto.LoginResponseDto;
import com.example.tm.auth.dto.SignupRequestDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.service.TmAuthService;
import com.example.tm.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TmAuthService tmAuthService;

    @PostMapping
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto data = tmAuthService.login(request);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Login Successfully", data)
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSummaryDto>> signup(@Valid @RequestBody SignupRequestDto request) {
        UserSummaryDto data = tmAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.successResponse(HttpStatus.CREATED.value(), "User created successfully", data)
        );
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getLoggedInUsers() {
        List<UserSummaryDto> data = tmAuthService.getLoggedInUsers();
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Active users fetched successfully", data)
        );
    }
}
