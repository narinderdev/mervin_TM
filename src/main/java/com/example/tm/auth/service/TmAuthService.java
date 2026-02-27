package com.example.tm.auth.service;

import com.example.tm.auth.dto.LoginRequestDto;
import com.example.tm.auth.dto.LoginResponseDto;
import com.example.tm.auth.dto.SignupRequestDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.auth.security.TmJwtService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TmAuthService {

    private final TmUserRepository tmUserRepository;
    private final TmJwtService tmJwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserSummaryDto signup(SignupRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (tmUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        boolean firstUser = tmUserRepository.count() == 0;

        TmUser user = new TmUser();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(firstUser ? "Admin" : "Technician");
        user.setActive(true);

        TmUser saved = tmUserRepository.save(user);
        return toUserSummary(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        TmUser user = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = tmJwtService.generateAccessToken(user);
        return LoginResponseDto.builder()
                .token(token)
                .user(toUserSummary(user))
                .mfaRequired(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getLoggedInUsers() {
        return tmUserRepository.findByActiveTrue()
                .stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    private UserSummaryDto toUserSummary(TmUser user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
