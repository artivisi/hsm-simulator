package com.artivisi.hsm.simulator.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for EmailService.
 *
 * This test requires actual SMTP credentials to run.
 * Set the following environment variables to enable this test:
 * - EMAIL_TEST_ENABLED=true
 * - SMTP_HOST (e.g., smtp.gmail.com)
 * - SMTP_PORT (e.g., 587)
 * - SMTP_USERNAME (your email)
 * - SMTP_PASSWORD (your app password)
 * - EMAIL_FROM (sender email)
 * - EMAIL_TEST_RECIPIENT (where to send test emails)
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.docker.compose.enabled=true",
    "hsm.email.enabled=${EMAIL_TEST_ENABLED:true}"
})
@Slf4j
class EmailServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private EmailService emailService;

    @Value("${EMAIL_TEST_RECIPIENT:artivisi@yopmail.com}")
    private String testRecipient;

    @Test
    void shouldCheckIfEmailIsEnabled() {
        log.info("Email enabled: {}", emailService.isEmailEnabled());
        log.info("Test recipient: {}", testRecipient);

        // This test always passes, just logs the configuration
        assertNotNull(emailService);
    }

    @Test
    void shouldSendSimpleHtmlEmail() {
        log.info("Sending test email to: {}", testRecipient);

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            padding: 20px;
                            background-color: #f4f4f4;
                        }
                        .container {
                            background: white;
                            padding: 30px;
                            border-radius: 10px;
                            max-width: 600px;
                            margin: 0 auto;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 20px;
                            border-radius: 8px;
                            text-align: center;
                        }
                        .content {
                            margin-top: 20px;
                            line-height: 1.6;
                        }
                        .footer {
                            margin-top: 30px;
                            text-align: center;
                            color: #666;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🧪 Email Service Test</h1>
                        </div>
                        <div class="content">
                            <p>This is a test email from the HSM Simulator application.</p>
                            <p>If you received this email, the email service is working correctly!</p>
                            <ul>
                                <li>HTML formatting: ✅</li>
                                <li>Email delivery: ✅</li>
                                <li>SMTP configuration: ✅</li>
                            </ul>
                        </div>
                        <div class="footer">
                            <p>ArtiVisi Intermedia &copy; 2025</p>
                        </div>
                    </div>
                </body>
                </html>
                """;

        assertDoesNotThrow(() -> {
            emailService.sendHtmlEmail(
                testRecipient,
                "HSM Simulator - Email Service Test",
                htmlContent
            );
        });

        log.info("Test email sent successfully!");
    }

    @Test
    void shouldSendEmailWithAttachment() {
        log.info("Sending test email with attachment to: {}", testRecipient);

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            padding: 20px;
                            background-color: #f4f4f4;
                        }
                        .container {
                            background: white;
                            padding: 30px;
                            border-radius: 10px;
                            max-width: 600px;
                            margin: 0 auto;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 20px;
                            border-radius: 8px;
                            text-align: center;
                        }
                        .content {
                            margin-top: 20px;
                            line-height: 1.6;
                        }
                        .info-box {
                            background: #e3f2fd;
                            border-left: 4px solid #2196f3;
                            padding: 15px;
                            margin: 15px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📎 Attachment Test</h1>
                        </div>
                        <div class="content">
                            <p>This email contains a test attachment.</p>
                            <div class="info-box">
                                <strong>Attachment:</strong> test-file.txt<br>
                                <strong>Size:</strong> ~500 bytes<br>
                                <strong>Type:</strong> Plain text
                            </div>
                            <p>Please check if the attachment is present and can be downloaded.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;

        String attachmentContent = """
                ═══════════════════════════════════════════════════════
                         HSM SIMULATOR - TEST ATTACHMENT
                ═══════════════════════════════════════════════════════

                This is a test attachment file.

                It simulates how key shares would be attached to emails
                in the actual application.

                TEST INFORMATION
                ───────────────────────────────────────────────────────
                Test ID:        TEST-001
                Test Type:      Email Attachment
                Timestamp:      2025-10-23
                Application:    HSM Simulator

                ═══════════════════════════════════════════════════════
                """;

        byte[] attachmentBytes = attachmentContent.getBytes();

        assertDoesNotThrow(() -> {
            emailService.sendHtmlEmailWithAttachment(
                testRecipient,
                "HSM Simulator - Attachment Test",
                htmlContent,
                "test-file.txt",
                attachmentBytes
            );
        });

        log.info("Test email with attachment sent successfully!");
    }

    @Test
    void shouldSimulateKeyShareEmail() {
        log.info("Sending simulated key share email to: {}", testRecipient);

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px;
                            border-radius: 10px 10px 0 0;
                            text-align: center;
                        }
                        .content {
                            background: #f8f9fa;
                            padding: 30px;
                            border-radius: 0 0 10px 10px;
                        }
                        .info-box {
                            background: white;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 20px 0;
                            border-left: 4px solid #667eea;
                        }
                        .warning-box {
                            background: #fff3cd;
                            border: 1px solid #ffc107;
                            padding: 15px;
                            border-radius: 8px;
                            margin: 20px 0;
                        }
                        .footer {
                            text-align: center;
                            color: #6c757d;
                            font-size: 12px;
                            margin-top: 30px;
                        }
                        .label {
                            font-weight: bold;
                            color: #667eea;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>🔐 HSM Key Share (TEST)</h1>
                        <p>Secure Key Ceremony Distribution</p>
                    </div>

                    <div class="content">
                        <h2>Dear Test User,</h2>

                        <p>This is a <strong>TEST EMAIL</strong> simulating key share distribution.</p>

                        <div class="info-box">
                            <p><span class="label">Share ID:</span> TEST-SHARE-001</p>
                            <p><span class="label">Threshold:</span> 2 of 3 shares required for key recovery</p>
                            <p><span class="label">Your Role:</span> Custodian of encrypted key share</p>
                        </div>

                        <p>In a real scenario, your encrypted key share would be attached to this email. Please:</p>
                        <ul>
                            <li>Download and store the attached file in a secure location</li>
                            <li>Keep multiple backups in different secure locations</li>
                            <li>DO NOT share this file with unauthorized personnel</li>
                            <li>Keep the file encrypted and password-protected if possible</li>
                        </ul>

                        <div class="warning-box">
                            <strong>⚠️ Security Notice</strong>
                            <p style="margin: 10px 0 0 0;">Any 2 shares can be combined to reconstruct the master key. Protect this share as you would protect the master key itself.</p>
                        </div>
                    </div>

                    <div class="footer">
                        <p>This is a TEST email from HSM Simulator</p>
                        <p>ArtiVisi Intermedia &copy; 2025</p>
                    </div>
                </body>
                </html>
                """;

        String shareContent = """
                ═══════════════════════════════════════════════════════════════
                           HSM KEY CEREMONY - ENCRYPTED KEY SHARE (TEST)
                ═══════════════════════════════════════════════════════════════

                CEREMONY INFORMATION
                ─────────────────────────────────────────────────────────────
                Ceremony Name:     Test Ceremony
                Ceremony ID:       CER-TEST-001
                Completed:         2025-10-23 07:30:00
                Algorithm:         AES-256
                Key Size:          256 bits

                MASTER KEY INFORMATION
                ─────────────────────────────────────────────────────────────
                Master Key ID:     MK-TEST-001
                Fingerprint:       TEST:1234:5678:ABCD
                Status:            ACTIVE

                CUSTODIAN INFORMATION
                ─────────────────────────────────────────────────────────────
                Custodian:         Custodian A
                Name:              Test User
                Email:             test@example.com

                SHARE INFORMATION
                ─────────────────────────────────────────────────────────────
                Share ID:          TEST-SHARE-001
                Share Index:       1 of 3
                Threshold:         2 shares required for recovery
                Polynomial Degree: 1
                Generated:         2025-10-23 07:30:00

                VERIFICATION
                ─────────────────────────────────────────────────────────────
                Verification Hash: a1b2c3d4e5f67890abcdef1234567890

                ENCRYPTED SHARE DATA
                ─────────────────────────────────────────────────────────────
                Format: Base64-encoded AES-256-GCM encrypted data
                The encrypted data includes:
                  - Shamir share index and value
                  - Prime modulus for reconstruction
                  - Threshold information

                BEGIN ENCRYPTED SHARE
                VGhpcyBpcyBhIFRFU1QgZW5jcnlwdGVkIHNoYXJlIGRhdGEuIEluIGEgcmVhbCBz
                Y2VuYXJpbywgdGhpcyB3b3VsZCBiZSB0aGUgYWN0dWFsIGVuY3J5cHRlZCBTaGFt
                aXIgc2VjcmV0IHNoYXJpbmcgZGF0YS4gVGhpcyBpcyBmb3IgVEVTVElORyBwdXJw
                b3NlcyBvbmx5IQ==
                END ENCRYPTED SHARE

                SECURITY NOTICE
                ─────────────────────────────────────────────────────────────
                • Store this share in a secure location
                • Do NOT share with unauthorized personnel
                • Any 2 shares can reconstruct the master key
                • Required for HSM recovery operations
                • Contact security team if compromised

                ═══════════════════════════════════════════════════════════════
                        Generated by HSM Simulator - ArtiVisi Intermedia
                        2025-10-23 07:30:00
                ═══════════════════════════════════════════════════════════════
                """;

        byte[] shareBytes = shareContent.getBytes();

        assertDoesNotThrow(() -> {
            emailService.sendHtmlEmailWithAttachment(
                testRecipient,
                "Your HSM Key Share - Test Ceremony",
                htmlContent,
                "key-share-TEST-SHARE-001.txt",
                shareBytes
            );
        });

        log.info("Simulated key share email sent successfully!");
    }
}
