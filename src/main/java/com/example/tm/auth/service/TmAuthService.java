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

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        EamUser eamUser = loadActiveEamUser(normalizedEmail);
        List<EamCompany> companies = loadActiveCompanies(eamUser);

        TmUser user = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> syncFromEam(eamUser, normalizedEmail, request.getPassword()));

        validatePasswordAndStatus(request.getPassword(), user);

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

    private void validatePasswordAndStatus(String rawPassword, TmUser user) {
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    /**
     * If the user exists in EAM but not yet in TM, create/sync a TM user using the
     * same hashed password. This lets EAM users log into TM without resetting passwords.
     */
    private TmUser syncFromEam(EamUser eamUser, String normalizedEmail, String rawPassword) {
        String eamPasswordHash = eamUser.getPassword();
        if (eamPasswordHash == null || !passwordEncoder.matches(rawPassword, eamPasswordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        TmUser user = new TmUser();
        user.setFirstName(eamUser.getFirstName());
        user.setLastName(eamUser.getLastName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(eamPasswordHash); // already bcrypt-hashed in EAM
        user.setRole(resolveRole(eamUser).orElse("Technician"));
        user.setActive(true);

        return tmUserRepository.save(user);
    }

    private EamUser loadActiveEamUser(String normalizedEmail) {
        EamUser eamUser = eamUserRepository.findByEmailAndDeletedFalse(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (eamUser.getStatus() != EamUserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }
        return eamUser;
    }

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

    private void rejectActiveInvite(String normalizedEmail) {
        if (inviteRepository.existsByEmailAndAcceptedFalse(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An invite is already pending for this email");
        }
    }
}
