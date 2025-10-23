# Email Testing Guide

This document explains how to test the email sending functionality in the HSM Simulator application.

## Overview

The application uses Spring Mail to send encrypted key shares via email with HTML formatting and attachments. Email sending is **disabled by default** for security and can be enabled via configuration.

## Configuration

### Environment Variables

To enable and configure email sending, set the following environment variables:

```bash
# Enable email sending (default: false)
export EMAIL_ENABLED=true

# SMTP Configuration
export SMTP_HOST=smtp.gmail.com          # Your SMTP server
export SMTP_PORT=587                      # SMTP port (587 for TLS)
export SMTP_USERNAME=your-email@gmail.com # SMTP username
export SMTP_PASSWORD=your-app-password    # SMTP password or app password
export EMAIL_FROM=noreply@artivisi.com   # Sender email address
```

### Using Gmail

If using Gmail, you need to create an **App Password**:

1. Go to Google Account settings
2. Security → 2-Step Verification
3. App passwords → Generate new app password
4. Use the generated password for `SMTP_PASSWORD`

**Note:** Never use your actual Gmail password in applications!

### Using Other Email Providers

Configure SMTP settings for your provider:

- **Brevo (formerly Sendinblue):**
  - Host: `smtp-relay.brevo.com`
  - Port: `587`
  - Username: Your Brevo SMTP login (from Settings → SMTP & API)
  - Password: Your SMTP key (not your account password!)
  - **IMPORTANT**: Sender email must be verified in Brevo
    1. Go to https://app.brevo.com/settings/senders
    2. Add and verify your sender email
    3. Use that verified email as `EMAIL_FROM`

- **Outlook/Office365:**
  - Host: `smtp.office365.com`
  - Port: `587`

- **Yahoo:**
  - Host: `smtp.mail.yahoo.com`
  - Port: `587`

- **SendGrid:**
  - Host: `smtp.sendgrid.net`
  - Port: `587`
  - Username: `apikey`
  - Password: Your SendGrid API key

## Running Integration Tests

### Prerequisites

1. Configure SMTP environment variables (see above)
2. Set test recipient email:
   ```bash
   export EMAIL_TEST_RECIPIENT=your-test-email@example.com
   ```

### Run All Email Tests

```bash
# Enable email testing
export EMAIL_TEST_ENABLED=true

# Run the integration tests
mvn test -Dtest=EmailServiceIntegrationTest
```

### Run Specific Test

```bash
# Test simple HTML email
mvn test -Dtest=EmailServiceIntegrationTest#shouldSendSimpleHtmlEmail

# Test email with attachment
mvn test -Dtest=EmailServiceIntegrationTest#shouldSendEmailWithAttachment

# Test simulated key share email
mvn test -Dtest=EmailServiceIntegrationTest#shouldSimulateKeyShareEmail
```

### Tests Included

1. **shouldCheckIfEmailIsEnabled()**
   - Verifies email service configuration
   - Always runs (doesn't require EMAIL_TEST_ENABLED)
   - Logs current configuration

2. **shouldSendSimpleHtmlEmail()**
   - Sends basic HTML email
   - Tests SMTP connectivity
   - Requires EMAIL_TEST_ENABLED=true

3. **shouldSendEmailWithAttachment()**
   - Sends HTML email with text attachment
   - Tests attachment functionality
   - Requires EMAIL_TEST_ENABLED=true

4. **shouldSimulateKeyShareEmail()**
   - Sends realistic key share email
   - Tests actual production email format
   - Requires EMAIL_TEST_ENABLED=true

## Testing in Development

### Option 1: Use Real Email Service

Set actual SMTP credentials and test with real email delivery.

```bash
export EMAIL_ENABLED=true
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=your-email@gmail.com
export SMTP_PASSWORD=your-app-password
export EMAIL_FROM=noreply@example.com
export EMAIL_TEST_ENABLED=true
export EMAIL_TEST_RECIPIENT=your-test-email@gmail.com

mvn test -Dtest=EmailServiceIntegrationTest
```

### Option 2: Use Local SMTP Server (Simulated)

Keep `EMAIL_ENABLED=false` to use the simulated email mode:

```bash
export EMAIL_ENABLED=false
mvn spring-boot:run
```

In this mode, emails are logged to the console instead of being sent:

```
WARN: Email sending is disabled. Simulating email send:
INFO:   To: alice@example.com
INFO:   Subject: Your HSM Key Share - Test Ceremony
INFO:   Attachment: key-share-KS-001.txt (2048 bytes)
```

## Production Configuration

### Using application.properties

Create `application-prod.properties` for production:

```properties
# Email Configuration
spring.mail.host=${SMTP_HOST}
spring.mail.port=${SMTP_PORT}
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

hsm.email.from=${EMAIL_FROM}
hsm.email.enabled=true
```

Run with production profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Using Docker/Kubernetes

Pass environment variables via container configuration:

```yaml
# docker-compose.yml
services:
  hsm-simulator:
    environment:
      - EMAIL_ENABLED=true
      - SMTP_HOST=smtp.gmail.com
      - SMTP_PORT=587
      - SMTP_USERNAME=${SMTP_USERNAME}
      - SMTP_PASSWORD=${SMTP_PASSWORD}
      - EMAIL_FROM=noreply@artivisi.com
```

## Troubleshooting

### Email not sending

1. Check `EMAIL_ENABLED` is set to `true`
2. Verify SMTP credentials are correct
3. Check application logs for errors
4. Ensure firewall allows SMTP port (587)

### Authentication failures

- Gmail: Use App Password, not regular password
- Enable "Less secure app access" if required (not recommended)
- Check 2FA settings

### Timeout errors

- Increase timeout in application.properties:
  ```properties
  spring.mail.properties.mail.smtp.connectiontimeout=10000
  spring.mail.properties.mail.smtp.timeout=10000
  ```

### Emails going to spam

- Configure SPF/DKIM records for your domain
- Use verified sender email address
- Use reputable SMTP service (SendGrid, AWS SES, etc.)

### Sender validation errors (Brevo)

**Error**: "Sender is not valid" or "Validate your sender"

**Solution**:
1. Log into Brevo dashboard: https://app.brevo.com
2. Go to **Settings → Senders**
3. Click **Add a new sender**
4. Enter your email address (must be one you own)
5. Verify via confirmation email
6. Update `EMAIL_FROM` to match the verified sender
7. Restart the application

**Example**:
```bash
export EMAIL_FROM=verified@yourdomain.com  # Must be verified in Brevo
export SMTP_HOST=smtp-relay.brevo.com
export SMTP_USERNAME=your-smtp-login@smtp-brevo.com
export SMTP_PASSWORD=your-smtp-key
export EMAIL_ENABLED=true
```

## Security Considerations

1. **Never commit SMTP credentials** to version control
2. Use environment variables or secret management
3. Use **App Passwords** instead of actual passwords
4. Enable TLS/SSL for SMTP connections
5. Rotate credentials regularly
6. Use dedicated email service account
7. Monitor email sending logs
8. Implement rate limiting in production

## Email Templates

The application sends professionally formatted HTML emails with:

- Gradient header with ceremony branding
- Custodian information
- Share details (ID, threshold, role)
- Security warnings
- Encrypted share file as attachment
- Footer with organization info

See `ShareDistributionService.buildShareEmailHtml()` for template implementation.
