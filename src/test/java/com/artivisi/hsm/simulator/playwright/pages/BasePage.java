package com.artivisi.hsm.simulator.playwright.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

public abstract class BasePage {
    protected final Page page;

    public BasePage(Page page) {
        this.page = page;
    }

    // Layout Elements
    public Locator appContainer() {
        return page.locator("#app-container");
    }

    public Locator mainHeader() {
        return page.locator("#main-header");
    }

    public Locator appTitle() {
        return page.locator("#app-title");
    }

    public Locator headerLogo() {
        return page.locator("#header-logo");
    }

    public Locator welcomeMessage() {
        return page.locator("#welcome-message");
    }

    public Locator settingsIcon() {
        return page.locator("#settings-icon");
    }

    public Locator mainSidebar() {
        return page.locator("#main-sidebar");
    }

    public Locator mainContent() {
        return page.locator("#main-content");
    }

    public Locator pageContent() {
        return page.locator("#page-content");
    }

    public Locator mainFooter() {
        return page.locator("#main-footer");
    }

    // Footer Elements
    public Locator footerLeft() {
        return page.locator("#footer-left");
    }

    public Locator copyright() {
        return page.locator("#copyright");
    }

    public Locator artivisiCredit() {
        return page.locator("#artivisi-credit");
    }

    public Locator artivisiLink() {
        return page.locator("#artivisi-link");
    }

    public Locator artivisiLogo() {
        return page.locator("#artivisi-logo");
    }

    public Locator artivisiText() {
        return page.locator("#artivisi-text");
    }

    public Locator footerRight() {
        return page.locator("#footer-right");
    }

    public Locator gitInfo() {
        return page.locator("#git-info");
    }

    public Locator gitCommit() {
        return page.locator("#git-commit");
    }

    public Locator gitBranch() {
        return page.locator("#git-branch");
    }

    public Locator versionInfo() {
        return page.locator("#version-info");
    }

    public Locator appVersion() {
        return page.locator("#app-version");
    }

    public Locator springVersion() {
        return page.locator("#spring-version");
    }

    // Navigation Elements
    public Locator sidebarKeyManagement() {
        return page.locator("#sidebar-key-management");
    }

    public Locator sidebarEncryption() {
        return page.locator("#sidebar-encryption");
    }

    public Locator sidebarSignature() {
        return page.locator("#sidebar-signature");
    }

    public Locator sidebarCertificates() {
        return page.locator("#sidebar-certificates");
    }

    public Locator sidebarTransactionLog() {
        return page.locator("#sidebar-transaction-log");
    }

    public Locator sidebarStatistics() {
        return page.locator("#sidebar-statistics");
    }

    public Locator sidebarSettings() {
        return page.locator("#sidebar-settings");
    }

    public Locator sidebarAbout() {
        return page.locator("#sidebar-about");
    }

    // Common Methods
    public String getTitle() {
        return page.title();
    }

    public String getUrl() {
        return page.url();
    }

    public void navigateTo(String url) {
        page.navigate(url);
    }

    public void refresh() {
        page.reload();
    }

    public boolean isVisible(Locator locator) {
        return locator.isVisible();
    }

    public String getText(Locator locator) {
        return locator.textContent();
    }

    public String getAttribute(Locator locator, String attribute) {
        return locator.getAttribute(attribute);
    }

    public String getGitCommitHref() {
        return getAttribute(gitCommit(), "href");
    }
}