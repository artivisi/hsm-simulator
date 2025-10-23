package com.artivisi.hsm.simulator.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending emails with HTML and attachments support.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${hsm.email.from}")
    private String fromEmail;

    @Value("${hsm.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:not-configured}")
    private String smtpHost;

    @Value("${spring.mail.port:0}")
    private int smtpPort;

    @Value("${spring.mail.username:not-configured}")
    private String smtpUsername;

    @PostConstruct
    public void init() {
        log.info("===============================================");
        log.info("Email Service Configuration:");
        log.info("  Enabled: {}", emailEnabled);
        log.info("  From: {}", fromEmail);
        log.info("  SMTP Host: {}", smtpHost);
        log.info("  SMTP Port: {}", smtpPort);
        log.info("  SMTP Username: {}", smtpUsername);
        log.info("===============================================");
    }

    /**
     * Sends an HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.warn("Email sending is disabled. Simulating email send:");
            log.info("  To: {}", to);
            log.info("  Subject: {}", subject);
            log.info("  Content: {}", htmlContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to create email message", e);
            throw new EmailSendException("Failed to create email message: " + e.getMessage(), e);
        } catch (MailException e) {
            log.error("Failed to send email", e);
            throw new EmailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Sends an HTML email with plain text attachment
     */
    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent,
                                            String attachmentName, byte[] attachmentContent) {
        log.debug("sendHtmlEmailWithAttachment called - enabled: {}, to: {}", emailEnabled, to);

        if (!emailEnabled) {
            log.warn("Email sending is disabled. Simulating email send:");
            log.info("  To: {}", to);
            log.info("  Subject: {}", subject);
            log.info("  Attachment: {} ({} bytes)", attachmentName, attachmentContent.length);
            return;
        }

        log.info("Attempting to send email to {} with subject: {}", to, subject);

        try {
            log.debug("Creating MimeMessage...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            log.debug("Setting email fields - from: {}, to: {}", fromEmail, to);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addAttachment(attachmentName, () -> new ByteArrayInputStream(attachmentContent), "text/plain");

            log.debug("Sending email via JavaMailSender...");
            mailSender.send(message);
            log.info("Email with attachment sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to create email message with attachment to {}: {}", to, e.getMessage(), e);
            throw new EmailSendException("Failed to create email message: " + e.getMessage(), e);
        } catch (MailException e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage(), e);
            throw new EmailSendException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            throw new EmailSendException("Unexpected error sending email: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if email sending is enabled
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    /**
     * Custom exception for email sending failures
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
