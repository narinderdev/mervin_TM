package com.example.tm.auth.service;

import com.example.tm.auth.dto.MfaSetupResponseDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.shared.EmailService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contains business logic for mfa service.
 */
@Service
@RequiredArgsConstructor
public class MfaService {

    private final TmUserRepository tmUserRepository;
    private final TotpService totpService;
    private final MfaCryptoService cryptoService;
    private final MfaRateLimitService rateLimitService;
    private final EmailService emailService;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.mfa.app-name:TM}")
    private String appName;

    @Value("${app.mfa.qr-size:240}")
    private int qrSize;

    @Value("${app.mfa.email-otp-expiry-minutes:5}")
    private int emailOtpExpiryMinutes;

    /** Handles setup. */
    public MfaSetupResponseDto setup(String email) {
        TmUser user = requireUser(email);
        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is already enabled");
        }
        if (!user.isMfaEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is not verified for MFA");
        }

        String secret = totpService.generateSecret();
        user.setMfaSecretTemp(cryptoService.encrypt(secret));
        tmUserRepository.save(user);

        String otpAuthUrl = buildOtpAuthUrl(appName, secret, user.getEmail());
        String qrCodeImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(generateQrPng(otpAuthUrl));
        return new MfaSetupResponseDto(secret, qrCodeImage);
    }

    /** Handles verify setup. */
    public void verifySetup(String email, String code) {
        TmUser user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("setup", user.getId()));

        String encryptedSecret = user.getMfaSecretTemp();
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA setup not initialized");
        }
        String secret = cryptoService.decrypt(encryptedSecret);
        if (!totpService.verifyCode(secret, code, 1)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        user.setMfaSecret(encryptedSecret);
        user.setMfaSecretTemp(null);
        user.setMfaEnabled(true);
        tmUserRepository.save(user);
    }

    /** Handles disable. */
    public void disable(String email, String code) {
        TmUser user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("disable", user.getId()));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled");
        }
        String secret = cryptoService.decrypt(user.getMfaSecret());
        if (!totpService.verifyCode(secret, code, 1)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaSecretTemp(null);
        tmUserRepository.save(user);
    }

    /** Handles send email otp. */
    public void sendEmailOtp(String email) {
        TmUser user = requireUser(email);

        Instant now = Instant.now();
        String existingOtp = user.getMfaEmailOtp();
        Instant existingExpiry = user.getMfaEmailOtpExpiresAt();
        if (existingOtp != null && !existingOtp.isBlank()
                && existingExpiry != null
                && !now.isAfter(existingExpiry)) {
            return;
        }

        rateLimitService.checkOrThrow(rateLimitKey("email_send", user.getId()));

        String otp = generateOtp();
        user.setMfaEmailOtp(otp);
        user.setMfaEmailOtpExpiresAt(now.plus(Duration.ofMinutes(emailOtpExpiryMinutes)));
        user.setMfaEmailVerified(false);
        tmUserRepository.save(user);

        String html = """
                <p>Your email verification code is:</p>
                <h2>%s</h2>
                <p>This code expires in %d minutes.</p>
                """.formatted(otp, emailOtpExpiryMinutes);
        emailService.sendHtml(user.getEmail(), "Your email verification code", html);
    }

    /** Handles verify email otp. */
    public void verifyEmailOtp(String email, String code) {
        TmUser user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("email_verify", user.getId()));

        String otp = user.getMfaEmailOtp();
        Instant expiresAt = user.getMfaEmailOtpExpiresAt();
        if (otp == null || otp.isBlank() || expiresAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email OTP not requested");
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email OTP expired");
        }
        if (!otp.equals(code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email OTP");
        }

        user.setMfaEmailVerified(true);
        user.setMfaEmailOtp(null);
        user.setMfaEmailOtpExpiresAt(null);
        tmUserRepository.save(user);
    }

    /** Handles verify active code. */
    public boolean verifyActiveCode(TmUser user, String code) {
        if (user == null || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }
        String secret = cryptoService.decrypt(user.getMfaSecret());
        return totpService.verifyCode(secret, code, 1);
    }

    /** Handles check login rate limit. */
    public void checkLoginRateLimit(Long userId) {
        rateLimitService.checkOrThrow(rateLimitKey("login", userId));
    }

    /** Handles require user. */
    private TmUser requireUser(String email) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return tmUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** Handles build otp auth url. */
    private String buildOtpAuthUrl(String issuerName, String secret, String email) {
        String label = URLEncoder.encode(issuerName + ":" + email, StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(issuerName, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=" + totpService.getDigits()
                + "&period=" + totpService.getPeriodSeconds();
    }

    /** Handles generate qr png. */
    private byte[] generateQrPng(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code");
        }
    }

    /** Handles rate limit key. */
    private String rateLimitKey(String action, Long userId) {
        return "mfa:" + action + ":" + userId;
    }

    /** Handles generate otp. */
    private String generateOtp() {
        int value = random.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
