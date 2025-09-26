package com.artivisi.hsm.simulator.playwright.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

public class HomePage extends BasePage {

    public HomePage(Page page) {
        super(page);
    }

    // Welcome Section Elements
    public Locator welcomeSection() {
        return page.locator("#welcome-section");
    }

    public Locator welcomeTitle() {
        return page.locator("#welcome-title");
    }

    public Locator welcomeSubtitle() {
        return page.locator("#welcome-subtitle");
    }

    // Quick Actions Elements
    public Locator quickActions() {
        return page.locator("#quick-actions");
    }

    public Locator quickActionsTitle() {
        return page.locator("#quick-actions-title");
    }

    public Locator btnGenerateKey() {
        return page.locator("#btn-generate-key");
    }

    public Locator btnImportKey() {
        return page.locator("#btn-import-key");
    }

    public Locator btnExportKey() {
        return page.locator("#btn-export-key");
    }

    public Locator btnSettings() {
        return page.locator("#btn-settings");
    }

    // Stats Cards Elements
    public Locator statsCards() {
        return page.locator("#stats-cards");
    }

    public Locator statsTotalKeys() {
        return page.locator("#stats-total-keys");
    }

    public Locator statsActiveOperations() {
        return page.locator("#stats-active-operations");
    }

    public Locator statsCertificates() {
        return page.locator("#stats-certificates");
    }

    public Locator statsSuccessRate() {
        return page.locator("#stats-success-rate");
    }

    // Feature Cards Elements
    public Locator getFeatureCard(String featureName) {
        return page.locator("#feature-" + featureName.toLowerCase().replace(" ", "-"));
    }

    // Page Specific Methods
    public void clickGenerateKey() {
        btnGenerateKey().click();
    }

    public void clickImportKey() {
        btnImportKey().click();
    }

    public void clickExportKey() {
        btnExportKey().click();
    }

    public void clickSettings() {
        btnSettings().click();
    }

    public String getWelcomeTitle() {
        return getText(welcomeTitle());
    }

    public String getWelcomeSubtitle() {
        return getText(welcomeSubtitle());
    }

    public String getQuickActionsTitle() {
        return getText(quickActionsTitle());
    }

    public boolean areQuickActionsButtonsVisible() {
        return isVisible(btnGenerateKey()) &&
               isVisible(btnImportKey()) &&
               isVisible(btnExportKey()) &&
               isVisible(btnSettings());
    }

    public boolean areStatsCardsVisible() {
        return isVisible(statsTotalKeys()) &&
               isVisible(statsActiveOperations()) &&
               isVisible(statsCertificates()) &&
               isVisible(statsSuccessRate());
    }

    // Validation Methods
    public boolean isHomePageLoaded() {
        return isVisible(welcomeSection()) &&
               isVisible(quickActions()) &&
               isVisible(statsCards()) &&
               getWelcomeTitle().contains("HSM Simulator");
    }

    public boolean isLayoutComplete() {
        return isVisible(appContainer()) &&
               isVisible(mainHeader()) &&
               isVisible(mainSidebar()) &&
               isVisible(mainContent()) &&
               isVisible(mainFooter());
    }

    public boolean isFooterInfoComplete() {
        return isVisible(copyright()) &&
               isVisible(artivisiLogo()) &&
               isVisible(gitCommit()) &&
               isVisible(appVersion()) &&
               isVisible(springVersion());
    }

    public String getArtivisiLinkUrl() {
        return getAttribute(artivisiLink(), "href");
    }

    public String getGitCommitId() {
        return getText(gitCommit());
    }

    public String getGitBranch() {
        return getText(gitBranch());
    }

    public String getAppVersion() {
        return getText(appVersion());
    }

    public String getSpringVersion() {
        return getText(springVersion());
    }
}