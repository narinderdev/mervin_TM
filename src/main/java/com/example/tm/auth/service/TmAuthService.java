package com.example.tm.auth.service;

import com.example.tm.auth.dto.LoginRequestDto;
import com.example.tm.auth.dto.LoginResponseDto;
import com.example.tm.auth.dto.SignupRequestDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.integration.eam.EamCompany;
import com.example.tm.auth.integration.eam.EamUser;
import com.example.tm.auth.integration.eam.EamUserRole;
import com.example.tm.auth.integration.eam.EamUserStatus;
import com.example.tm.auth.integration.eam.EamUserCompany;
import com.example.tm.auth.integration.eam.EamUserCompanyRepository;
import com.example.tm.auth.integration.eam.EamUserRepository;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.auth.repository.TmUserInviteRepository;
import com.example.tm.auth.security.TmJwtService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contains business logic for tm auth service.
 */
@Service
@RequiredArgsConstructor
public class TmAuthService {

    private static final String ADMIN_ROLE_NAME = "Admin";

    private final TmUserRepository tmUserRepository;
    private final EamUserRepository eamUserRepository;
    private final EamUserCompanyRepository eamUserCompanyRepository;
    private final TmUserInviteRepository inviteRepository;
    private final TmJwtService tmJwtService;
    private final PasswordEncoder passwordEncoder;

    // Handles signup.
    @Transactional
    public UserSummaryDto signup(SignupRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (tmUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        rejectActiveInvite(normalizedEmail);

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

    // Handles login.
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        EamUser eamUser = loadActiveEamUser(normalizedEmail);
        List<EamCompany> companies = loadActiveCompanies(eamUser);
        validateEamPassword(request.getPassword(), eamUser.getPassword());

        TmUser user = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> resyncFromEam(existing, eamUser, normalizedEmail))
                .orElseGet(() -> createFromEam(eamUser, normalizedEmail));

        validateUserStatus(user);

        String token = tmJwtService.generateAccessToken(user);
        List<LoginResponseDto.CompanyDto> responseCompanies = companies.stream()
                .map(this::toCompanyDto)
                .toList();
        boolean isAdmin = isAdmin(eamUser);

        return LoginResponseDto.builder()
                .token(token)
                .user(toUserSummary(user))
                .companies(responseCompanies)
                .isCompanySetup(isAdmin ? !responseCompanies.isEmpty() : null)
                .mfaRequired(false)
                .build();
    }

    // Returns logged in users.
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getLoggedInUsers() {
        return tmUserRepository.findByActiveTrue()
                .stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    // Converts data to user summary.
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

    // Normalizes email.
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    // Validates status.
    private void validateUserStatus(TmUser user) {
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }
    }

    // Validates EAM password hash.
    private void validateEamPassword(String rawPassword, String eamPasswordHash) {
        if (eamPasswordHash == null || !passwordEncoder.matches(rawPassword, eamPasswordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    /**
     * If the user exists in EAM but not yet in TM, create a TM user from EAM.
     */
    private TmUser createFromEam(EamUser eamUser, String normalizedEmail) {
        TmUser user = new TmUser();
        user.setFirstName(eamUser.getFirstName());
        user.setLastName(eamUser.getLastName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(eamUser.getPassword()); // already bcrypt-hashed in EAM
        user.setRole(resolveRole(eamUser).orElse("Technician"));
        user.setActive(true);

        return tmUserRepository.save(user);
    }

    // Re-syncs existing TM user fields from EAM.
    private TmUser resyncFromEam(TmUser existing, EamUser eamUser, String normalizedEmail) {
        String eamRole = resolveRole(eamUser).orElse("Technician");
        boolean changed = false;

        if (!Objects.equals(existing.getFirstName(), eamUser.getFirstName())) {
            existing.setFirstName(eamUser.getFirstName());
            changed = true;
        }
        if (!Objects.equals(existing.getLastName(), eamUser.getLastName())) {
            existing.setLastName(eamUser.getLastName());
            changed = true;
        }
        if (!Objects.equals(existing.getEmail(), normalizedEmail)) {
            existing.setEmail(normalizedEmail);
            changed = true;
        }
        if (!Objects.equals(existing.getPasswordHash(), eamUser.getPassword())) {
            existing.setPasswordHash(eamUser.getPassword());
            changed = true;
        }
        if (!Objects.equals(existing.getRole(), eamRole)) {
            existing.setRole(eamRole);
            changed = true;
        }

        return changed ? tmUserRepository.save(existing) : existing;
    }

    // Loads active eam user.
    private EamUser loadActiveEamUser(String normalizedEmail) {
        EamUser eamUser = eamUserRepository.findByEmailAndDeletedFalse(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (eamUser.getStatus() != EamUserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }
        return eamUser;
    }

    // Loads active companies.
    private List<EamCompany> loadActiveCompanies(EamUser eamUser) {
        List<EamCompany> companies = eamUserCompanyRepository.findByUser_IdAndCompany_ActiveTrue(eamUser.getId())
                .stream()
                .map(EamUserCompany::getCompany)
                .filter(Objects::nonNull)
                .toList();

        if (companies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active company found for this user");
        }

        return companies;
    }

    // Converts data to company dto.
    private LoginResponseDto.CompanyDto toCompanyDto(EamCompany company) {
        return LoginResponseDto.CompanyDto.builder()
                .id(company.getId())
                .companyLegalName(company.getCompanyLegalName())
                .companyTradeName(company.getCompanyTradeName())
                .companyNumber(company.getCompanyNumber())
                .address(company.getAddress())
                .city(company.getCity())
                .country(company.getCountry())
                .postalCode(company.getPostalCode())
                .active(company.getActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    // Resolves role.
    private Optional<String> resolveRole(EamUser eamUser) {
        return eamUser.getUserRoles() == null
                ? Optional.empty()
                : eamUser.getUserRoles()
                        .stream()
                        .map(EamUserRole::getRole)
                        .filter(r -> r != null && r.getName() != null)
                        .map(r -> r.getName().trim())
                        .filter(name -> !name.isEmpty())
                        .findFirst();
    }

    // Checks whether admin.
    private boolean isAdmin(EamUser eamUser) {
        return eamUser.getUserRoles() != null
                && eamUser.getUserRoles()
                        .stream()
                        .map(EamUserRole::getRole)
                        .filter(Objects::nonNull)
                        .map(r -> r.getName())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .anyMatch(roleName -> ADMIN_ROLE_NAME.equalsIgnoreCase(roleName));
    }

    // Rejects active invite.
    private void rejectActiveInvite(String normalizedEmail) {
        if (inviteRepository.existsByEmailAndAcceptedFalseAndExpiresAtAfter(normalizedEmail, java.time.Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An invite is already pending for this email");
        }
    }
}
