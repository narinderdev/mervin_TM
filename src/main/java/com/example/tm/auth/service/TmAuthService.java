package com.example.tm.auth.service;

import com.example.tm.auth.dto.LoginRequestDto;
import com.example.tm.auth.dto.LoginResponseDto;
import com.example.tm.auth.dto.ChangePasswordDto;
import com.example.tm.auth.dto.ForgotPasswordDto;
import com.example.tm.auth.dto.MfaLoginDto;
import com.example.tm.auth.dto.SignupRequestDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.integration.eam.EamCompany;
import com.example.tm.auth.integration.eam.EamCompanyRepository;
import com.example.tm.auth.integration.eam.EamUser;
import com.example.tm.auth.integration.eam.EamUserRole;
import com.example.tm.auth.integration.eam.EamUserStatus;
import com.example.tm.auth.integration.eam.EamUserCompany;
import com.example.tm.auth.integration.eam.EamUserCompanyRepository;
import com.example.tm.auth.integration.eam.EamUserRepository;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.auth.repository.TmUserInviteRepository;
import com.example.tm.auth.security.TmJwtService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contains business logic for tm auth service.
 */
@Service
public class TmAuthService {

    private static final String ADMIN_ROLE_NAME = "Admin";
    private static final String TECHNICIAN_ROLE_NAME = "Technician";

    @Value("${app.auth.login.max-failed-attempts:5}")
    private int maxFailedAttempts = 5;

    @Value("${app.auth.login.block-seconds:900}")
    private int blockSeconds = 900;

    @Value("${app.mfa.token-expiration-ms:300000}")
    private long mfaTokenExpirationMs = 300000L;

    private final TmUserRepository tmUserRepository;
    private final EamCompanyRepository eamCompanyRepository;
    private final EamUserRepository eamUserRepository;
    private final EamUserCompanyRepository eamUserCompanyRepository;
    private final TmUserInviteRepository inviteRepository;
    private final TmJwtService tmJwtService;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;
    @Qualifier("tmJdbcTemplate")
    private final JdbcTemplate tmJdbcTemplate;
    private final ConcurrentMap<String, LoginAttemptState> loginAttemptByEmail = new ConcurrentHashMap<>();

    /** Creates a new instance of tm auth service. */
    public TmAuthService(
            TmUserRepository tmUserRepository,
            EamCompanyRepository eamCompanyRepository,
            EamUserRepository eamUserRepository,
            EamUserCompanyRepository eamUserCompanyRepository,
            TmUserInviteRepository inviteRepository,
            TmJwtService tmJwtService,
            PasswordEncoder passwordEncoder,
            MfaService mfaService,
            @Qualifier("tmJdbcTemplate") JdbcTemplate tmJdbcTemplate) {
        this.tmUserRepository = tmUserRepository;
        this.eamCompanyRepository = eamCompanyRepository;
        this.eamUserRepository = eamUserRepository;
        this.eamUserCompanyRepository = eamUserCompanyRepository;
        this.inviteRepository = inviteRepository;
        this.tmJwtService = tmJwtService;
        this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.tmJdbcTemplate = tmJdbcTemplate;
    }

    /** Handles signup. */
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

    /** Handles login. */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        enforceLoginRateLimit(normalizedEmail);
        try {
            Optional<TmUser> existingUser = tmUserRepository.findByEmailIgnoreCase(normalizedEmail);
            Optional<EamUser> eamUserOpt = findActiveEamUser(normalizedEmail);

            TmUser user;
            List<EamCompany> companies;
            EamUser eamUser;

            if (eamUserOpt.isPresent()) {
                eamUser = eamUserOpt.get();
                companies = resolveLoginCompanies(eamUser, normalizedEmail);
                validateLoginPassword(request.getPassword(), existingUser.orElse(null), eamUser);
                user = existingUser
                        .map(existing -> resyncFromEam(existing, eamUser, normalizedEmail))
                        .orElseGet(() -> createFromEam(eamUser, normalizedEmail));
            } else {
                eamUser = null;
                companies = loadActiveCompaniesForTmTechnician(normalizedEmail);
                user = existingUser.orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
                validateTmPassword(request.getPassword(), user);
            }

            validateUserStatus(user);
            clearFailedLogin(normalizedEmail);

            if (isTechnician(user) && !user.isMfaEnabled()) {
                mfaService.sendEmailOtp(user.getEmail());
            }

            if (user.isMfaEnabled()) {
                String mfaToken = buildMfaToken(user, request.getDeviceToken(), request.getDevicePlatform());
                return buildLoginResponse(user, eamUser, companies, null, true, mfaToken);
            }

            String token = tmJwtService.generateAccessToken(user);
            return buildLoginResponse(user, eamUser, companies, token, false, null);
        } catch (ResponseStatusException ex) {
            if (HttpStatus.UNAUTHORIZED.equals(ex.getStatusCode())) {
                registerFailedLogin(normalizedEmail);
            }
            throw ex;
        }
    }

    /** Handles login with mfa. */
    @Transactional
    public LoginResponseDto loginWithMfa(MfaLoginDto request) {
        Claims claims = parseMfaClaims(request.getMfaToken());
        String normalizedEmail = normalizeEmail(claims.getSubject());
        Long userId = extractUserId(claims);

        if (normalizedEmail == null || normalizedEmail.isBlank() || userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA token");
        }

        TmUser user = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!Objects.equals(user.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA token");
        }

        validateUserStatus(user);
        if (!user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled");
        }

        mfaService.checkLoginRateLimit(user.getId());
        boolean codeValid = mfaService.verifyActiveCode(user, request.getCode());
        if (!codeValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        EamUser eamUser = loadActiveEamUser(normalizedEmail);
        List<EamCompany> companies = resolveLoginCompanies(eamUser, normalizedEmail);
        String token = tmJwtService.generateAccessToken(user);
        return buildLoginResponse(user, eamUser, companies, token, false, null);
    }

    /** Changes user password. */
    @Transactional
    public void changePassword(ChangePasswordDto dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        TmUser user = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentPasswordHash = user.getPasswordHash();
        if (currentPasswordHash == null || currentPasswordHash.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is not set for this user");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), currentPasswordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.setPasswordHash(encodedPassword);
        tmUserRepository.save(user);
        syncPasswordToActiveEamUser(normalizedEmail, encodedPassword);
    }

    /** Resets user password from forgot-password flow. */
    @Transactional
    public void forgotPassword(ForgotPasswordDto dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        TmUser user = tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not active");
        }
        if (user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()
                && passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different from current password");
        }

        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.setPasswordHash(encodedPassword);
        tmUserRepository.save(user);
        syncPasswordToActiveEamUser(normalizedEmail, encodedPassword);
    }

    /** Returns logged in users. */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getLoggedInUsers() {
        return tmUserRepository.findByActiveTrue()
                .stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    /** Converts data to user summary. */
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

    /** Normalizes email. */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Validates status. */
    private void validateUserStatus(TmUser user) {
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }
    }

    /** Validates login password and heals TM/EAM mismatch for invited technicians. */
    private void validateLoginPassword(String rawPassword, TmUser tmUser, EamUser eamUser) {
        String eamPasswordHash = eamUser.getPassword();
        if (eamPasswordHash != null && passwordEncoder.matches(rawPassword, eamPasswordHash)) {
            return;
        }

        String tmPasswordHash = tmUser == null ? null : tmUser.getPasswordHash();
        if (isTechnician(tmUser) && tmPasswordHash != null && passwordEncoder.matches(rawPassword, tmPasswordHash)) {
            eamUser.setPassword(tmPasswordHash);
            eamUserRepository.save(eamUser);
            return;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    /** Validates TM-only password. */
    private void validateTmPassword(String rawPassword, TmUser tmUser) {
        String tmPasswordHash = tmUser == null ? null : tmUser.getPasswordHash();
        if (tmPasswordHash == null || !passwordEncoder.matches(rawPassword, tmPasswordHash)) {
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

    /** Re-syncs existing TM user fields from EAM. */
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

    /** Loads active eam user. */
    private EamUser loadActiveEamUser(String normalizedEmail) {
        return findActiveEamUser(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    }

    /** Finds active eam user if present. */
    private Optional<EamUser> findActiveEamUser(String normalizedEmail) {
        Optional<EamUser> eamUserOpt = eamUserRepository.findByEmailAndDeletedFalse(normalizedEmail);
        if (eamUserOpt.isEmpty()) {
            return Optional.empty();
        }
        EamUser eamUser = eamUserOpt.get();
        if (eamUser.getStatus() != EamUserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
        }
        return Optional.of(eamUser);
    }

    /** Resolves companies for login (EAM mapping first, TM mapping fallback). */
    private List<EamCompany> resolveLoginCompanies(EamUser eamUser, String normalizedEmail) {
        List<EamCompany> companies = loadActiveCompaniesFromEam(eamUser);
        if (companies.isEmpty()) {
            companies = loadActiveCompaniesForTmTechnician(normalizedEmail);
        }
        if (companies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active company found for this user");
        }
        return companies;
    }

    /** Loads active companies from EAM user-company mapping. */
    private List<EamCompany> loadActiveCompaniesFromEam(EamUser eamUser) {
        return eamUserCompanyRepository.findByUser_IdAndCompany_ActiveTrue(eamUser.getId())
                .stream()
                .map(EamUserCompany::getCompany)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Loads active companies for TM-only technician login. */
    private List<EamCompany> loadActiveCompaniesForTmTechnician(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return List.of();
        }
        List<Long> companyIds = tmJdbcTemplate.queryForList(
                """
                        SELECT DISTINCT company_id
                        FROM technicians
                        WHERE is_deleted = 0
                          AND company_id IS NOT NULL
                          AND LOWER(email) = LOWER(?)
                        """,
                Long.class,
                normalizedEmail);
        if (companyIds.isEmpty()) {
            return List.of();
        }
        return eamCompanyRepository.findByIdInAndActiveTrue(companyIds)
                .stream()
                .sorted(java.util.Comparator.comparing(EamCompany::getId))
                .toList();
    }

    /** Converts data to company dto. */
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

    /** Builds login response. */
    private LoginResponseDto buildLoginResponse(
            TmUser user,
            EamUser eamUser,
            List<EamCompany> companies,
            String token,
            boolean mfaRequired,
            String mfaToken) {
        List<LoginResponseDto.CompanyDto> responseCompanies = companies.stream()
                .map(this::toCompanyDto)
                .toList();
        boolean isAdmin = eamUser != null && isAdmin(eamUser);

        return LoginResponseDto.builder()
                .token(token)
                .user(toUserSummary(user))
                .companies(responseCompanies)
                .isCompanySetup(isAdmin ? !responseCompanies.isEmpty() : null)
                .mfaRequired(mfaRequired)
                .mfaToken(mfaToken)
                .build();
    }

    /** Builds mfa token. */
    private String buildMfaToken(TmUser user, String deviceToken, String devicePlatform) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "mfa_pending");
        if (deviceToken != null && !deviceToken.trim().isEmpty()) {
            claims.put("deviceToken", deviceToken.trim());
        }
        if (devicePlatform != null && !devicePlatform.trim().isEmpty()) {
            claims.put("devicePlatform", devicePlatform.trim());
        }
        return tmJwtService.generateToken(user.getEmail(), claims, mfaTokenExpirationMs);
    }

    /** Handles parse mfa claims. */
    private Claims parseMfaClaims(String token) {
        try {
            Claims claims = tmJwtService.parseClaims(token);
            String type = claims.get("type", String.class);
            if (!"mfa_pending".equals(type)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA token");
            }
            return claims;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired MFA token");
        } catch (ResponseStatusException ex) {
            throw ex;
        }
    }

    /** Extracts user id from claims. */
    private Long extractUserId(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        Object altValue = claims.get("user_id");
        if (altValue instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /** Resolves role. */
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

    /** Checks whether admin. */
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

    /** Checks whether technician. */
    private boolean isTechnician(TmUser user) {
        return user != null
                && user.getRole() != null
                && TECHNICIAN_ROLE_NAME.equalsIgnoreCase(user.getRole().trim());
    }

    /** Rejects active invite. */
    private void rejectActiveInvite(String normalizedEmail) {
        if (inviteRepository.existsByEmailAndAcceptedFalseAndExpiresAtAfter(normalizedEmail, java.time.Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An invite is already pending for this email");
        }
    }

    /** Enforces login rate limit. */
    private void enforceLoginRateLimit(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        LoginAttemptState state = loginAttemptByEmail.get(normalizedEmail);
        if (state == null) {
            return;
        }
        if (state.blockedUntil != null && state.blockedUntil.isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again later."
            );
        }
        if (state.blockedUntil != null && !state.blockedUntil.isAfter(Instant.now())) {
            loginAttemptByEmail.remove(normalizedEmail);
        }
    }

    /** Registers failed login. */
    private void registerFailedLogin(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        loginAttemptByEmail.compute(normalizedEmail, (key, existing) -> {
            LoginAttemptState current = existing == null ? new LoginAttemptState(0, null) : existing;
            int failures = current.failedAttempts + 1;
            Instant blockedUntil = failures >= maxFailedAttempts
                    ? Instant.now().plusSeconds(blockSeconds)
                    : null;
            return new LoginAttemptState(failures, blockedUntil);
        });
    }

    /** Clears failed login state. */
    private void clearFailedLogin(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        loginAttemptByEmail.remove(normalizedEmail);
    }

    /** Syncs password to active EAM user when mapped by email. */
    private void syncPasswordToActiveEamUser(String normalizedEmail, String encodedPassword) {
        eamUserRepository.findByEmailAndDeletedFalse(normalizedEmail).ifPresent(eamUser -> {
            if (eamUser.getStatus() != EamUserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
            }
            eamUser.setPassword(encodedPassword);
            eamUserRepository.save(eamUser);
        });
    }

    /** Stores login attempt state. */
    private static final class LoginAttemptState {
        private final int failedAttempts;
        private final Instant blockedUntil;

        private LoginAttemptState(int failedAttempts, Instant blockedUntil) {
            this.failedAttempts = failedAttempts;
            this.blockedUntil = blockedUntil;
        }
    }
}
