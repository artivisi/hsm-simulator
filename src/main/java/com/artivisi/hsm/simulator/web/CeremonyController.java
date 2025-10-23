package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.KeyCeremony;
import com.artivisi.hsm.simulator.entity.KeyCustodian;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.KeyCustodianRepository;
import com.artivisi.hsm.simulator.service.CeremonyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for HSM ceremony management (admin operations).
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class CeremonyController {

    private final CeremonyService ceremonyService;
    private final KeyCustodianRepository custodianRepository;

    /**
     * Shows the ceremony initiation form
     */
    @GetMapping("/hsm/initialize")
    public String showInitializationForm(Model model) {
        log.info("Showing ceremony initialization form");

        // Get all active custodians
        List<KeyCustodian> custodians = custodianRepository.findByStatus(KeyCustodian.CustodianStatus.ACTIVE);

        model.addAttribute("custodians", custodians);
        model.addAttribute("defaultThreshold", 2);
        model.addAttribute("defaultNumberOfCustodians", 3);
        model.addAttribute("defaultAlgorithm", "AES-256");
        model.addAttribute("defaultKeySize", 256);

        return "ceremony/initialize";
    }

    /**
     * Creates a new key ceremony (API endpoint)
     */
    @PostMapping("/api/ceremonies")
    @ResponseBody
    public ResponseEntity<?> createCeremony(@RequestBody CeremonyCreationRequest request) {
        log.info("Creating new ceremony: {}", request.getCeremonyName());

        try {
            // Build service request
            CeremonyService.CeremonyCreationRequest serviceRequest = CeremonyService.CeremonyCreationRequest.builder()
                    .ceremonyName(request.getCeremonyName())
                    .purpose(request.getPurpose())
                    .ceremonyType(KeyCeremony.CeremonyType.valueOf(request.getCeremonyType()))
                    .custodianIds(request.getCustodianIds())
                    .numberOfCustodians(request.getNumberOfCustodians())
                    .threshold(request.getThreshold())
                    .algorithm(request.getAlgorithm())
                    .keySize(request.getKeySize())
                    .contributionDeadline(request.getContributionDeadline())
                    .createdBy("admin") // TODO: Get from authentication
                    .build();

            KeyCeremony ceremony = ceremonyService.createCeremony(serviceRequest);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "ceremonyId", ceremony.getId().toString(),
                    "message", "Ceremony created successfully"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Validation error creating ceremony", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating ceremony", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred"
            ));
        }
    }

    /**
     * Shows the ceremony dashboard with progress tracking
     */
    @GetMapping("/hsm/ceremony/{ceremonyId}")
    public String showCeremonyDashboard(@PathVariable UUID ceremonyId, Model model) {
        log.info("Showing ceremony dashboard: {}", ceremonyId);

        try {
            CeremonyService.CeremonyStatusResponse status = ceremonyService.getCeremonyStatus(ceremonyId);

            model.addAttribute("ceremony", status.getCeremony());
            model.addAttribute("contributedCount", status.getContributedCount());
            model.addAttribute("requiredCount", status.getRequiredCount());
            model.addAttribute("totalCustodians", status.getTotalCustodians());
            model.addAttribute("thresholdMet", status.isThresholdMet());
            model.addAttribute("custodianStatuses", status.getCustodianStatuses());
            model.addAttribute("progressPercentage",
                    (int) ((status.getContributedCount() * 100.0) / status.getTotalCustodians()));

            return "ceremony/dashboard";

        } catch (IllegalArgumentException e) {
            log.error("Ceremony not found: {}", ceremonyId);
            model.addAttribute("error", "Ceremony not found");
            return "error/404";
        }
    }

    /**
     * Generates the master key (API endpoint)
     */
    @PostMapping("/api/ceremonies/{ceremonyId}/generate")
    @ResponseBody
    public ResponseEntity<?> generateMasterKey(@PathVariable UUID ceremonyId) {
        log.info("Generating master key for ceremony: {}", ceremonyId);

        try {
            MasterKey masterKey = ceremonyService.generateMasterKey(ceremonyId, "admin");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "masterKeyId", masterKey.getMasterKeyId(),
                    "fingerprint", masterKey.getKeyFingerprint(),
                    "message", "Master key generated successfully"
            ));

        } catch (IllegalStateException e) {
            log.error("Invalid state for key generation", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error generating master key", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to generate master key"
            ));
        }
    }

    /**
     * Gets ceremony status (API endpoint for AJAX polling)
     */
    @GetMapping("/api/ceremonies/{ceremonyId}/status")
    @ResponseBody
    public ResponseEntity<?> getCeremonyStatus(@PathVariable UUID ceremonyId) {
        try {
            CeremonyService.CeremonyStatusResponse status = ceremonyService.getCeremonyStatus(ceremonyId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", status.getCeremony().getStatus().name(),
                    "contributedCount", status.getContributedCount(),
                    "requiredCount", status.getRequiredCount(),
                    "thresholdMet", status.isThresholdMet(),
                    "custodianStatuses", status.getCustodianStatuses()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Ceremony not found"
            ));
        }
    }

    // ===== Request DTOs =====

    @lombok.Data
    public static class CeremonyCreationRequest {
        private String ceremonyName;
        private String purpose;
        private String ceremonyType;
        private List<UUID> custodianIds;
        private Integer numberOfCustodians;
        private Integer threshold;
        private String algorithm;
        private Integer keySize;
        private LocalDateTime contributionDeadline;
    }
}
