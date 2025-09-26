package com.artivisi.hsm.simulator.playwright.tests;

import com.artivisi.hsm.simulator.playwright.pages.HomePage;
import com.artivisi.hsm.simulator.HsmSimulatorApplication;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = HsmSimulatorApplication.class)
@Testcontainers
public class HomePageTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("hsm_simulator_test")
            .withUsername("hsm_user")
            .withPassword("xK9m2pQ8vR5nF7tA1sD3wE6zY")
            .withReuse(true);

    private HomePage homePage;
    private Page page;
    private Playwright playwright;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        playwright = Playwright.create();
        page = playwright.chromium().launch().newPage();
        homePage = new HomePage(page);
        baseUrl = "http://localhost:" + port;
        homePage.navigateTo(baseUrl);
    }

    @AfterEach
    public void tearDown() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @DisplayName("Should load homepage with correct title")
    public void shouldLoadHomepageWithCorrectTitle() {
        assertEquals("HSM Simulator - Dashboard", homePage.getTitle());
        assertTrue(homePage.getUrl().contains("localhost"));
    }

    @Test
    @DisplayName("Should display welcome section with correct content")
    public void shouldDisplayWelcomeSection() {
        assertTrue(homePage.isVisible(homePage.welcomeSection()));

        String welcomeTitle = homePage.getWelcomeTitle();
        assertEquals("Welcome to HSM Simulator", welcomeTitle);

        String welcomeSubtitle = homePage.getWelcomeSubtitle();
        assertTrue(welcomeSubtitle.contains("Hardware Security Module simulation platform"));
    }

    @Test
    @DisplayName("Should display quick actions section")
    public void shouldDisplayQuickActionsSection() {
        assertTrue(homePage.isVisible(homePage.quickActions()));

        String actionsTitle = homePage.getQuickActionsTitle();
        assertEquals("Quick Actions", actionsTitle);

        assertTrue(homePage.areQuickActionsButtonsVisible());
    }

    @Test
    @DisplayName("Should display stats cards section")
    public void shouldDisplayStatsCards() {
        assertTrue(homePage.isVisible(homePage.statsCards()));
        assertTrue(homePage.areStatsCardsVisible());
    }

    @Test
    @DisplayName("Should have complete layout structure")
    public void shouldHaveCompleteLayoutStructure() {
        assertTrue(homePage.isLayoutComplete());

        // Verify main layout components
        assertTrue(homePage.isVisible(homePage.appContainer()));
        assertTrue(homePage.isVisible(homePage.mainHeader()));
        assertTrue(homePage.isVisible(homePage.mainSidebar()));
        assertTrue(homePage.isVisible(homePage.mainContent()));
        assertTrue(homePage.isVisible(homePage.mainFooter()));
    }

    @Test
    @DisplayName("Should have header with correct elements")
    public void shouldHaveHeaderWithCorrectElements() {
        assertTrue(homePage.isVisible(homePage.headerLogo()));
        assertTrue(homePage.isVisible(homePage.appTitle()));
        assertTrue(homePage.isVisible(homePage.welcomeMessage()));
        assertTrue(homePage.isVisible(homePage.settingsIcon()));

        assertEquals("HSM Simulator", homePage.getText(homePage.appTitle()));
        assertEquals("Welcome to HSM Simulator", homePage.getText(homePage.welcomeMessage()));
    }

    @Test
    @DisplayName("Should have sidebar with navigation links")
    public void shouldHaveSidebarWithNavigationLinks() {
        assertTrue(homePage.isVisible(homePage.mainSidebar()));

        // Verify feature navigation links
        assertTrue(homePage.isVisible(homePage.sidebarKeyManagement()));
        assertTrue(homePage.isVisible(homePage.sidebarEncryption()));
        assertTrue(homePage.isVisible(homePage.sidebarSignature()));
        assertTrue(homePage.isVisible(homePage.sidebarCertificates()));
        assertTrue(homePage.isVisible(homePage.sidebarTransactionLog()));
        assertTrue(homePage.isVisible(homePage.sidebarStatistics()));

        // Verify configuration navigation links
        assertTrue(homePage.isVisible(homePage.sidebarSettings()));
        assertTrue(homePage.isVisible(homePage.sidebarAbout()));
    }

    @Test
    @DisplayName("Should have footer with copyright and Artivisi credit")
    public void shouldHaveFooterWithCopyrightAndArtivisiCredit() {
        assertTrue(homePage.isVisible(homePage.footerLeft()));
        assertTrue(homePage.isVisible(homePage.copyright()));
        assertTrue(homePage.isVisible(homePage.artivisiCredit()));
        assertTrue(homePage.isVisible(homePage.artivisiLogo()));
        assertTrue(homePage.isVisible(homePage.artivisiText()));

        // Verify Artivisi link
        String artivisiUrl = homePage.getArtivisiLinkUrl();
        assertEquals("https://artivisi.com", artivisiUrl);

        // Verify copyright text
        String copyrightText = homePage.getText(homePage.copyright());
        assertTrue(copyrightText.contains("2025 HSM Simulator"));
        assertTrue(copyrightText.contains("All rights reserved"));

        // Verify Artivisi text
        String artivisiText = homePage.getText(homePage.artivisiText());
        assertEquals("Developed by Artivisi", artivisiText);
    }

    @Test
    @DisplayName("Should have footer with version and git information")
    public void shouldHaveFooterWithVersionAndGitInformation() {
        assertTrue(homePage.isVisible(homePage.footerRight()));
        assertTrue(homePage.isVisible(homePage.gitInfo()));
        assertTrue(homePage.isVisible(homePage.gitCommit()));
        assertTrue(homePage.isVisible(homePage.gitBranch()));
        assertTrue(homePage.isVisible(homePage.versionInfo()));
        assertTrue(homePage.isVisible(homePage.appVersion()));
        assertTrue(homePage.isVisible(homePage.springVersion()));

        // Verify version information
        String appVersion = homePage.getAppVersion();
        assertTrue(appVersion.contains("0.0.1-SNAPSHOT"));

        String springVersion = homePage.getSpringVersion();
        assertTrue(springVersion.contains("Spring Boot"));
        assertTrue(springVersion.contains("3.5.6"));

        // Verify git information should not be "unknown"
        String gitCommit = homePage.getGitCommitId();
        assertNotNull(gitCommit);
        assertFalse(gitCommit.trim().isEmpty());
        assertNotEquals("unknown", gitCommit.toLowerCase());

        String gitBranch = homePage.getGitBranch();
        assertNotNull(gitBranch);
        assertFalse(gitBranch.trim().isEmpty());

        // Verify GitHub link functionality
        String gitCommitHref = homePage.getGitCommitHref();
        assertNotNull(gitCommitHref);
        assertTrue(gitCommitHref.contains("github.com"));
        assertTrue(gitCommitHref.contains("/commit/" + gitCommit));
    }

    @Test
    @DisplayName("Should have complete footer information")
    public void shouldHaveCompleteFooterInformation() {
        assertTrue(homePage.isFooterInfoComplete());
    }

    @Test
    @DisplayName("Should verify homepage is fully loaded")
    public void shouldVerifyHomepageIsFullyLoaded() {
        assertTrue(homePage.isHomePageLoaded());
    }

    @Test
    @DisplayName("Should have working navigation links")
    public void shouldHaveWorkingNavigationLinks() {
        // Test that navigation links exist and are clickable
        assertTrue(homePage.sidebarKeyManagement().isEnabled());
        assertTrue(homePage.sidebarEncryption().isEnabled());
        assertTrue(homePage.sidebarSettings().isEnabled());
        assertTrue(homePage.sidebarAbout().isEnabled());
    }

    @Test
    @DisplayName("Should have working quick action buttons")
    public void shouldHaveWorkingQuickActionButtons() {
        // Test that quick action buttons exist and are clickable
        assertTrue(homePage.btnGenerateKey().isEnabled());
        assertTrue(homePage.btnImportKey().isEnabled());
        assertTrue(homePage.btnExportKey().isEnabled());
        assertTrue(homePage.btnSettings().isEnabled());
    }

  }