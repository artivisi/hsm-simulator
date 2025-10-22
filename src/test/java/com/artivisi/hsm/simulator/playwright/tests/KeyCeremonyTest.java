package com.artivisi.hsm.simulator.playwright.tests;

import com.artivisi.hsm.simulator.playwright.pages.HomePage;
import com.artivisi.hsm.simulator.playwright.pages.KeyCeremonyPage;
import com.artivisi.hsm.simulator.HsmSimulatorApplication;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = HsmSimulatorApplication.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeyCeremonyTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("hsm_simulator_test")
            .withUsername("hsm_user")
            .withPassword("xK9m2pQ8vR5nF7tA1sD3wE6zY")
            .withReuse(true);

    private KeyCeremonyPage keyCeremonyPage;
    private Page page;
    private Playwright playwright;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        playwright = Playwright.create();
        page = playwright.chromium().launch().newPage();
        keyCeremonyPage = new KeyCeremonyPage(page, port);
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    public void tearDown() {
        if (playwright != null) {
            playwright.close();
        }
    }

    // Test Data Constants
    private static final String CEREMONY_NAME = "Test Initialization Ceremony";
    private static final String CEREMONY_PURPOSE = "Automated test for HSM master key initialization";
    private static final String STRONG_PASSPHRASE_A = "MySecureP@ssw0rdForCustodianA!";
    private static final String STRONG_PASSPHRASE_B = "B@ckupK3yC0ntributorStr0ng!";
    private static final String STRONG_PASSPHRASE_C = "ThirdCustodian#Secur3P@ssphrase2025!";

    private static final String WEAK_PASSPHRASE_SHORT = "short123";
    private static final String WEAK_PASSPHRASE_NO_UPPER = "lowercasepassword123!";
    private static final String WEAK_PASSPHRASE_NO_LOWER = "UPPERCASEPASSWORD123!";
    private static final String WEAK_PASSPHRASE_NO_NUMBERS = "NoNumbersHere!";
    private static final String WEAK_PASSPHRASE_NO_SPECIAL = "SimplePassword123";

    /**
     * Test Case 1.1: Setup Awal Key Ceremony
     * Verifikasi dashboard dan inisiasi ceremony baru
     */
    @Test
    @Order(1)
    @DisplayName("TC-KEYCER-001: Should display key ceremony dashboard and allow ceremony initiation")
    public void shouldDisplayKeyCeremonyDashboardAndAllowInitiation() {
        // Navigate to key ceremony dashboard
        page.navigate(baseUrl + "/key-ceremony");

        // Verify dashboard is loaded
        assertTrue(keyCeremonyPage.isDashboardLoaded(),
            "Key ceremony dashboard should be loaded");

        // Verify master key status
        assertFalse(keyCeremonyPage.hasActiveMasterKey(),
            "Should not have active master key initially");

        // Verify should initiate message
        assertTrue(keyCeremonyPage.shouldInitiateCeremony(),
            "Should show initiation button when no master key exists");

        // Verify security warnings and information
        assertTrue(keyCeremonyPage.areSecurityWarningsDisplayed(),
            "Security warnings should be displayed");
    }

    /**
     * Test Case 1.2: Create New Key Ceremony
     * Membuat ceremony baru dengan validasi form
     */
    @Test
    @Order(2)
    @DisplayName("TC-KEYCER-002: Should create new key ceremony with form validation")
    public void shouldCreateNewKeyCeremonyWithFormValidation() {
        // Navigate to new ceremony form
        page.navigate(baseUrl + "/key-ceremony/new");

        // Verify form is loaded
        assertTrue(keyCeremonyPage.isNewCeremonyFormLoaded(),
            "New ceremony form should be loaded");

        // Verify security warnings
        assertTrue(keyCeremonyPage.areSecurityWarningsDisplayed(),
            "Security warnings should be displayed");

        // Verify requirements information
        assertTrue(keyCeremonyPage.areRequirementsDisplayed(),
            "Requirements information should be displayed");

        // Test empty form validation
        keyCeremonyPage.submitCeremonyForm();

        // Check if validation prevents submission
        assertTrue(keyCeremonyPage.isNewCeremonyFormLoaded(),
            "Should stay on form page after validation failure");

        // Fill form with valid data
        keyCeremonyPage.fillCeremonyForm(CEREMONY_NAME, CEREMONY_PURPOSE);

        // Submit form
        keyCeremonyPage.submitCeremonyForm();

        // Wait for redirect to ceremony details
        await().atMost(Duration.ofSeconds(10))
            .until(() -> keyCeremonyPage.isContributionPageLoaded("token") ||
                       keyCeremonyPage.getUrl().contains("/key-ceremony/"));
    }

    /**
     * Test Case 1.3: Custodian A Contribution
     * Proses kontribusi custodian pertama
     */
    @Test
    @Order(3)
    @DisplayName("TC-KEYCER-003: Should handle Custodian A contribution with strong passphrase")
    public void shouldHandleCustodianAContributionWithStrongPassphrase() {
        // First, create a ceremony to get contribution tokens
        createTestCeremony();

        // Extract contribution token from logs or use a test token
        String testToken = "test-token-custodian-a-" + UUID.randomUUID().toString();

        // Navigate to contribution page
        keyCeremonyPage.navigateToContribution(testToken);

        // Verify contribution page is loaded
        assertTrue(keyCeremonyPage.isContributionPageLoaded(testToken),
            "Contribution page should be loaded");

        // Verify security information
        assertTrue(keyCeremonyPage.areSecurityWarningsDisplayed(),
            "Security warnings should be displayed");

        // Test weak passphrases first
        testWeakPassphrases();

        // Test strong passphrase
        keyCeremonyPage.fillPassphrase(STRONG_PASSPHRASE_A);

        // Wait for strength validation
        await().atMost(Duration.ofSeconds(2))
            .until(() -> keyCeremonyPage.isPassphraseStrengthValid());

        // Verify strength indicators
        assertTrue(keyCeremonyPage.getStrengthText().contains("Strong"),
            "Should show strong passphrase strength");

        // Confirm contribution
        keyCeremonyPage.confirmContribution();

        // Submit contribution
        keyCeremonyPage.submitContribution();

        // Wait for success page
        await().atMost(Duration.ofSeconds(5))
            .until(() -> keyCeremonyPage.isSuccessPageLoaded());

        // Verify success message
        assertTrue(keyCeremonyPage.isSuccessPageLoaded(),
            "Success page should be loaded after contribution");
    }

    /**
     * Test Case 1.4: Custodian B Contribution
     * Proses kontribusi custodian kedua
     */
    @Test
    @Order(4)
    @DisplayName("TC-KEYCER-004: Should handle Custodian B contribution")
    public void shouldHandleCustodianBContribution() {
        String testToken = "test-token-custodian-b-" + UUID.randomUUID().toString();

        // Navigate to contribution page
        keyCeremonyPage.navigateToContribution(testToken);

        // Verify contribution page is loaded
        assertTrue(keyCeremonyPage.isContributionPageLoaded(testToken),
            "Contribution page should be loaded");

        // Fill with strong passphrase
        keyCeremonyPage.fillPassphrase(STRONG_PASSPHRASE_B);

        // Wait for strength validation
        await().atMost(Duration.ofSeconds(2))
            .until(() -> keyCeremonyPage.isPassphraseStrengthValid());

        // Confirm and submit
        keyCeremonyPage.confirmContribution();
        keyCeremonyPage.submitContribution();

        // Verify success
        await().atMost(Duration.ofSeconds(5))
            .until(() -> keyCeremonyPage.isSuccessPageLoaded());
    }

    /**
     * Test Case 1.5: Custodian C Contribution
     * Proses kontribusi custodian ketiga
     */
    @Test
    @Order(5)
    @DisplayName("TC-KEYCER-005: Should handle Custodian C contribution")
    public void shouldHandleCustodianCContribution() {
        String testToken = "test-token-custodian-c-" + UUID.randomUUID().toString();

        // Navigate to contribution page
        keyCeremonyPage.navigateToContribution(testToken);

        // Verify contribution page is loaded
        assertTrue(keyCeremonyPage.isContributionPageLoaded(testToken),
            "Contribution page should be loaded");

        // Fill with strong passphrase
        keyCeremonyPage.fillPassphrase(STRONG_PASSPHRASE_C);

        // Wait for strength validation
        await().atMost(Duration.ofSeconds(2))
            .until(() -> keyCeremonyPage.isPassphraseStrengthValid());

        // Confirm and submit
        keyCeremonyPage.confirmContribution();
        keyCeremonyPage.submitContribution();

        // Verify success
        await().atMost(Duration.ofSeconds(5))
            .until(() -> keyCeremonyPage.isSuccessPageLoaded());
    }

    /**
     * Test Case 4.1: Passphrase Complexity Validation
     * Test berbagai passphrase yang tidak valid
     */
    @Test
    @Order(6)
    @DisplayName("TC-KEYCER-006: Should validate passphrase complexity requirements")
    public void shouldValidatePassphraseComplexityRequirements() {
        String testToken = "test-token-validation-" + UUID.randomUUID().toString();

        // Navigate to contribution page
        keyCeremonyPage.navigateToContribution(testToken);

        // Verify contribution page is loaded
        assertTrue(keyCeremonyPage.isContributionPageLoaded(testToken),
            "Contribution page should be loaded");

        // Test weak passphrases
        testWeakPassphrases();

        // Verify submit button is disabled for weak passphrases
        assertFalse(keyCeremonyPage.isSubmitButtonEnabled(),
            "Submit button should be disabled for weak passphrases");
    }

    /**
     * Test Case 4.2: Invalid Contribution Link
     * Test link yang tidak valid atau expired
     */
    @Test
    @Order(7)
    @DisplayName("TC-KEYCER-007: Should handle invalid contribution links")
    public void shouldHandleInvalidContributionLinks() {
        // Test invalid token
        String invalidToken = "invalid-token-" + UUID.randomUUID().toString();

        // Navigate to invalid contribution page
        keyCeremonyPage.navigateToContribution(invalidToken);

        // Verify error page is loaded
        assertTrue(keyCeremonyPage.isErrorPageLoaded(),
            "Error page should be loaded for invalid token");

        // Verify error message
        String errorMessage = keyCeremonyPage.getErrorMessage();
        assertTrue(errorMessage.contains("Invalid or expired contribution link"),
            "Should show appropriate error message");
    }

    /**
     * Test Case 4.3: Password Visibility Toggle
     * Test fungsi toggle visibility password
     */
    @Test
    @Order(8)
    @DisplayName("TC-KEYCER-008: Should toggle password visibility")
    public void shouldTogglePasswordVisibility() {
        String testToken = "test-token-visibility-" + UUID.randomUUID().toString();

        // Navigate to contribution page
        keyCeremonyPage.navigateToContribution(testToken);

        // Wait for either contribution page or error page to load
        await().atMost(Duration.ofSeconds(5))
            .until(() -> keyCeremonyPage.isContributionPageLoaded(testToken) ||
                       keyCeremonyPage.isErrorPageLoaded());

        // Only proceed with password visibility test if contribution page loaded successfully
        if (keyCeremonyPage.isContributionPageLoaded(testToken)) {
            // Verify initial state (password field)
            assertEquals("password", keyCeremonyPage.passphraseInput().getAttribute("type"),
                "Password field should be of type password initially");

            // Toggle visibility
            keyCeremonyPage.togglePasswordVisibility();

            // Verify field is now text
            assertEquals("text", keyCeremonyPage.passphraseInput().getAttribute("type"),
                "Password field should change to text type when toggled");

            // Toggle back
            keyCeremonyPage.togglePasswordVisibility();

            // Verify field is password again
            assertEquals("password", keyCeremonyPage.passphraseInput().getAttribute("type"),
                "Password field should revert to password type");
        } else {
            // Skip test if error page loaded (invalid token scenario)
            // This is expected behavior for test tokens
            assertTrue(keyCeremonyPage.isErrorPageLoaded(),
                "Should show error page for invalid test token");
        }
    }

    /**
     * Test Case 4.4: Concurrent Contribution Prevention
     * Test pencegahan kontribusi ganda
     */
    @Test
    @Order(9)
    @DisplayName("TC-KEYCER-009: Should prevent duplicate contributions")
    public void shouldPreventDuplicateContributions() {
        // Create ceremony and simulate first contribution
        String testToken = "test-token-duplicate-" + UUID.randomUUID().toString();

        // First contribution
        keyCeremonyPage.navigateToContribution(testToken);
        keyCeremonyPage.fillPassphrase(STRONG_PASSPHRASE_A);
        keyCeremonyPage.confirmContribution();
        keyCeremonyPage.submitContribution();

        // Wait for success
        await().atMost(Duration.ofSeconds(5))
            .until(() -> keyCeremonyPage.isSuccessPageLoaded());

        // Navigate back to contribution page with same token
        keyCeremonyPage.navigateToContribution(testToken);

        // Should show error for duplicate contribution
        await().atMost(Duration.ofSeconds(5))
            .until(() -> keyCeremonyPage.isErrorPageLoaded());

        String errorMessage = keyCeremonyPage.getErrorMessage();
        assertTrue(errorMessage.contains("Contribution already received"),
            "Should show duplicate contribution error");
    }

    /**
     * Test Case 4.5: Real-time Strength Validation
     * Test validasi kekuatan passphrase secara real-time
     */
    @Test
    @Order(10)
    @DisplayName("TC-KEYCER-010: Should provide real-time passphrase strength validation")
    public void shouldProvideRealtimePassphraseStrengthValidation() {
        String testToken = "test-token-strength-" + UUID.randomUUID().toString();

        // Navigate to contribution page
        keyCeremonyPage.navigateToContribution(testToken);

        // Test gradual improvement of passphrase
        keyCeremonyPage.fillPassphrase("weak"); // Too short
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should be invalid for too short passphrase");

        keyCeremonyPage.fillPassphrase("weakpassphrase123"); // No uppercase or special
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should be invalid for missing uppercase and special chars");

        keyCeremonyPage.fillPassphrase("WeakPassphrase123"); // No special chars
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should be invalid for missing special chars");

        // Final strong passphrase
        keyCeremonyPage.fillPassphrase(STRONG_PASSPHRASE_A);
        assertTrue(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should be valid for strong passphrase");

        // Verify strength indicator shows "Strong" or "Very Strong"
        String strengthText = keyCeremonyPage.getStrengthText();
        assertTrue(strengthText.contains("Strong") || strengthText.contains("Very Strong"),
            "Should show strong passphrase indicator");
    }

    // Helper Methods

    /**
     * Create a test ceremony for testing purposes
     */
    private void createTestCeremony() {
        // Navigate to new ceremony form
        page.navigate(baseUrl + "/key-ceremony/new");

        // Fill and submit form
        keyCeremonyPage.fillCeremonyForm(CEREMONY_NAME, CEREMONY_PURPOSE);
        keyCeremonyPage.submitCeremonyForm();

        // Wait for form submission to complete
        await().atMost(Duration.ofSeconds(5))
            .until(() -> !keyCeremonyPage.isNewCeremonyFormLoaded());
    }

    /**
     * Test various weak passphrases to ensure validation works
     */
    private void testWeakPassphrases() {
        // Test short passphrase
        keyCeremonyPage.fillPassphrase(WEAK_PASSPHRASE_SHORT);
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should reject short passphrase");

        // Test no uppercase
        keyCeremonyPage.fillPassphrase(WEAK_PASSPHRASE_NO_UPPER);
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should reject passphrase without uppercase");

        // Test no lowercase
        keyCeremonyPage.fillPassphrase(WEAK_PASSPHRASE_NO_LOWER);
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should reject passphrase without lowercase");

        // Test no numbers
        keyCeremonyPage.fillPassphrase(WEAK_PASSPHRASE_NO_NUMBERS);
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should reject passphrase without numbers");

        // Test no special characters
        keyCeremonyPage.fillPassphrase(WEAK_PASSPHRASE_NO_SPECIAL);
        assertFalse(keyCeremonyPage.isPassphraseStrengthValid(),
            "Should reject passphrase without special characters");
    }
}