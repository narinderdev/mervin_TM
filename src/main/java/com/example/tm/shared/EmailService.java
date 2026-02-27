package com.example.tm.shared;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    private String fromEmail;

    @PostConstruct
    public void init() {
        this.fromEmail = environment.getProperty("spring.mail.username");
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username is not set; emails will fail until configured.");
        } else {
            log.info("Emails will be sent from {}", fromEmail);
        }
    }

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
            log.info("Invite email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
