package com.artivisi.hsm.simulator.playwright.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

public class KeyCeremonyPage extends BasePage {
    private int port;

    public KeyCeremonyPage(Page page) {
        super(page);
        this.port = 8080; // Default port
    }

    public KeyCeremonyPage(Page page, int port) {
        super(page);
        this.port = port;
    }

    // Dashboard Elements
    public Locator dashboardTitle() {
        return page.locator("#dashboard-title");
    }

    public Locator masterKeyStatusCard() {
        return page.locator("#master-key-status-card");
    }

      public Locator hasActiveMasterKeyStatus() {
        return page.locator("#active-master-key-status");
    }

    public Locator noActiveMasterKeyStatus() {
        return page.locator("#no-active-master-key-status");
    }

    public Locator startNewCeremonyButton() {
        return page.locator("#start-new-ceremony-button");
    }

    public Locator shouldInitiateMessage() {
        return page.locator("#should-initiate-message");
    }

    public Locator activeCeremoniesSection() {
        return page.locator("#active-ceremonies-section");
    }

    public Locator noActiveCeremoniesMessage() {
        return page.locator("#no-active-ceremonies-message");
    }

    // New Ceremony Form Elements
    public Locator ceremonyNameInput() {
        return page.locator("#ceremonyName");
    }

    public Locator purposeInput() {
        return page.locator("#purpose");
    }

    public Locator startCeremonyButton() {
        return page.locator("#start-ceremony-button");
    }

    public Locator cancelButton() {
        return page.locator("#cancel-button");
    }

    // Ceremony Details Elements
    public Locator ceremonyIdHeader() {
        return page.locator("h1:has-text('Ceremony Details')");
    }

    public Locator ceremonyIdDisplay() {
        return page.locator("span:has-text('CER-')");
    }

    public Locator ceremonyStatusBadge() {
        return page.locator(".px-2.py-1.rounded-full.text-xs.font-medium");
    }

    public Locator contributionProgressBar() {
        return page.locator(".w-full.bg-gray-200.rounded-full.h-3 .bg-blue-600");
    }

    public Locator progressPercentage() {
        return page.locator(".text-2xl.font-bold.text-orange-600");
    }

    public Locator generateMasterKeyButton() {
        return page.locator("button:has-text('Generate Master Key')");
    }

    public Locator refreshStatusButton() {
        return page.locator("button:has-text('Refresh Status')");
    }

    // Contribution Form Elements
    public Locator contributionTitle() {
        return page.locator("#contribution-title");
    }

    public Locator custodianInfo() {
        return page.locator(".text-blue-900:has-text('Contribution Information')");
    }

    public Locator passphraseInput() {
        return page.locator("#passphrase");
    }

    public Locator passwordToggleButton() {
        return page.locator("#password-toggle-button");
    }

    public Locator strengthBar() {
        return page.locator("#strengthBar");
    }

    public Locator strengthText() {
        return page.locator("#strengthText");
    }

    public Locator submitContributionButton() {
        return page.locator("#submit-contribution-button");
    }

    public Locator confirmationCheckbox() {
        return page.locator("#confirmation-checkbox");
    }

    // Success/Error Page Elements
    public Locator successMessage() {
        return page.locator(".text-green-600:has-text('Contribution Submitted Successfully!')");
    }

    public Locator errorTitle() {
        return page.locator(".text-red-600:has-text('Contribution Link Error')");
    }

    public Locator errorMessage() {
        return page.locator(".text-gray-600:has-text('Invalid or expired contribution link')");
    }

    // Navigation Methods
    public void navigateToKeyCeremony() {
        page.navigate("http://localhost:" + getPort() + "/key-ceremony");
    }

    public void navigateToNewCeremony() {
        page.navigate("http://localhost:" + getPort() + "/key-ceremony/new");
    }

    public void navigateToContribution(String token) {
        page.navigate("http://localhost:" + getPort() + "/key-ceremony/contribute/" + token);
    }

    // Action Methods
    public void clickStartNewCeremony() {
        startNewCeremonyButton().click();
    }

    public void fillCeremonyForm(String name, String purpose) {
        ceremonyNameInput().fill(name);
        purposeInput().fill(purpose);
    }

    public void submitCeremonyForm() {
        startCeremonyButton().click();
    }

    public void fillPassphrase(String passphrase) {
        passphraseInput().fill(passphrase);
    }

    public void togglePasswordVisibility() {
        passwordToggleButton().click();
    }

    public void confirmContribution() {
        confirmationCheckbox().check();
    }

    public void submitContribution() {
        submitContributionButton().click();
    }

    public void clickGenerateMasterKey() {
        generateMasterKeyButton().click();
    }

    public void clickRefreshStatus() {
        refreshStatusButton().click();
    }

    public void clickBackToDashboard() {
        page.locator("a:has-text('Return to Key Ceremony Dashboard')").click();
    }

    // Validation Methods
    public boolean isDashboardLoaded() {
        return isVisible(dashboardTitle()) &&
               isVisible(masterKeyStatusCard());
    }

    public boolean hasActiveMasterKey() {
        return isVisible(hasActiveMasterKeyStatus());
    }

    public boolean shouldInitiateCeremony() {
        return isVisible(startNewCeremonyButton()) &&
               isVisible(shouldInitiateMessage());
    }

    public boolean isNewCeremonyFormLoaded() {
        return isVisible(ceremonyNameInput()) &&
               isVisible(purposeInput()) &&
               isVisible(startCeremonyButton());
    }

    public boolean isContributionPageLoaded(String token) {
        return isVisible(contributionTitle()) &&
               isVisible(passphraseInput()) &&
               page.url().contains(token);
    }

    public boolean isSuccessPageLoaded() {
        return isVisible(successMessage());
    }

    public boolean isErrorPageLoaded() {
        return isVisible(errorTitle());
    }

    public boolean isPassphraseStrengthValid() {
        return strengthBar().isVisible() &&
               strengthText().textContent().contains("Strong");
    }

    public boolean isSubmitButtonEnabled() {
        return !submitContributionButton().isDisabled();
    }

    public boolean areSecurityWarningsDisplayed() {
        return page.locator(".bg-yellow-50.border.border-yellow-200").isVisible();
    }

    public boolean areRequirementsDisplayed() {
        return page.locator(".text-gray-600:has-text('Minimum 16 characters')").isVisible() &&
               page.locator(".text-gray-600:has-text('Must contain uppercase letters')").isVisible();
    }

    // Text Extraction Methods
    public String getCeremonyId() {
        return getText(ceremonyIdDisplay());
    }

    public String getCeremonyStatus() {
        return getText(ceremonyStatusBadge());
    }

    public String getProgressPercentage() {
        return getText(progressPercentage());
    }

    public String getStrengthText() {
        return getText(strengthText());
    }

    public String getErrorMessage() {
        return getText(errorMessage());
    }

    // Helper method to get port
    private int getPort() {
        return this.port;
    }

    // Set port method
    public void setPort(int port) {
        this.port = port;
    }
}