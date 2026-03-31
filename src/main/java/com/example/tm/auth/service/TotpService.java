package com.example.tm.auth.service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contains business logic for totp service.
 */
@Service
public class TotpService {

    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] BASE32_LOOKUP = new int[128];
    private static final int SECRET_BYTES = 20;
    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final String HMAC_ALGO = "HmacSHA1";

    private final SecureRandom random = new SecureRandom();

    static {
        for (int i = 0; i < BASE32_LOOKUP.length; i++) {
            BASE32_LOOKUP[i] = -1;
        }
        for (int i = 0; i < BASE32_ALPHABET.length; i++) {
            BASE32_LOOKUP[BASE32_ALPHABET[i]] = i;
        }
    }

    /** Handles generate secret. */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Handles verify code. */
    public boolean verifyCode(String base32Secret, String code, int window) {
        if (base32Secret == null || code == null) {
            return false;
        }
        String normalizedCode = code.trim();
        if (!normalizedCode.matches("\\d{6}")) {
            return false;
        }
        long timeStep = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        for (int i = -window; i <= window; i++) {
            String expected = generateCode(base32Secret, timeStep + i);
            if (expected.equals(normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns digits. */
    public int getDigits() {
        return DIGITS;
    }

    /** Returns period seconds. */
    public int getPeriodSeconds() {
        return PERIOD_SECONDS;
    }

    /** Handles generate code. */
    private String generateCode(String base32Secret, long timeStep) {
        byte[] key = base32Decode(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.US, "%0" + DIGITS + "d", otp);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate MFA code");
        }
    }

    /** Handles base32 encode. */
    private String base32Encode(byte[] bytes) {
        StringBuilder output = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                output.append(BASE32_ALPHABET[index]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            output.append(BASE32_ALPHABET[index]);
        }
        return output.toString();
    }

    /** Handles base32 decode. */
    private byte[] base32Decode(String value) {
        if (value == null) {
            return new byte[0];
        }
        String normalized = value.trim().replace("=", "").replace(" ", "").toUpperCase(Locale.US);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= BASE32_LOOKUP.length || BASE32_LOOKUP[c] == -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MFA secret");
            }
            buffer = (buffer << 5) | BASE32_LOOKUP[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
