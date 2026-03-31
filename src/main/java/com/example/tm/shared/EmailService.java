package com.example.tm.shared;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for email service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${app.mail.enforce-credentials:true}")
    private boolean enforceMailCredentials;

    private String fromEmail;

    /** Handles init. */
    @PostConstruct
    public void init() {
        String username = environment.getProperty("spring.mail.username");
        String password = environment.getProperty("spring.mail.password");

        log.info("Mail username present: {}", username != null && !username.isBlank());
        log.info("Mail password present: {}", password != null && !password.isBlank());

        if ((username == null || username.isBlank()) || (password == null || password.isBlank())) {
            if (enforceMailCredentials) {
                throw new IllegalStateException("spring.mail.username/password are NOT configured");
            }
            log.warn("Mail credentials missing; email features (OTP/invite) will fail until configured.");
        } else {
            this.fromEmail = username;
            log.info("Emails will be sent from {}", fromEmail);
        }
    }

    /** Sends html. */
    public void sendHtml(String to, String subject, String html) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Mail credentials not configured (spring.mail.username/password).");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
