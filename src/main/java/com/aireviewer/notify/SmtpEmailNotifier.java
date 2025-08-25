package com.aireviewer.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * SMTP-based notifier that sends emails using Spring's JavaMailSender.
 */
@Service
public class SmtpEmailNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailNotifier.class);

    private final JavaMailSender mailSender;

    @Value("${notify.admin.email:}")
    private String adminEmail;

    @Value("${notify.from.email:}")
    private String fromEmail;

    public SmtpEmailNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void notifyAdmin(String subject, String body) {
        try {
            if (adminEmail == null || adminEmail.isBlank()) {
                log.warn("Admin email is not configured (notify.admin.email). Skipping email notification.");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setSubject(subject);
            String time = OffsetDateTime.now().toString();
            message.setText(body + "\n\nTimestamp: " + time);
            mailSender.send(message);
            log.info("Sent admin notification to {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send admin notification email: {}", e.getMessage(), e);
        }
    }
}
