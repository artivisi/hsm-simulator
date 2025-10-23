package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.PassphraseContribution;
import com.artivisi.hsm.simulator.service.CeremonyService;
import com.artivisi.hsm.simulator.service.PassphraseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller for custodian passphrase contributions (token-based, no authentication required).
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class ContributionController {

    private final CeremonyService ceremonyService;
    private final PassphraseService passphraseService;

    /**
     * Shows the contribution form for a custodian (token-based access)
     */
    @GetMapping("/hsm/contribute/{token}")
    public String showContributionForm(@PathVariable String token, Model model) {
        log.info("Showing contribution form for token: {}", token.substring(0, 8) + "...");

        try {
            CeremonyService.ContributionTokenInfo tokenInfo = ceremonyService.verifyContributionToken(token);

            if (!tokenInfo.isValid()) {
                model.addAttribute("error", tokenInfo.getErrorMessage());
                return "contribution/error";
            }

            model.addAttribute("token", token);
            model.addAttribute("ceremonyName", tokenInfo.getCeremonyName());
            model.addAttribute("ceremonyPurpose", tokenInfo.getCeremonyPurpose());
            model.addAttribute("custodianName", tokenInfo.getCustodianName());
            model.addAttribute("custodianLabel", tokenInfo.getCustodianLabel());
            model.addAttribute("deadline", tokenInfo.getDeadline());
            model.addAttribute("minLength", 12);
            model.addAttribute("recommendedLength", 20);

            return "contribution/form";

        } catch (IllegalArgumentException e) {
            log.error("Invalid contribution token", e);
            model.addAttribute("error", "Invalid or expired contribution token");
            return "contribution/error";
        }
    }

    /**
     * Verifies a contribution token (API endpoint)
     */
    @GetMapping("/api/contributions/verify/{token}")
    @ResponseBody
    public ResponseEntity<?> verifyToken(@PathVariable String token) {
        try {
            CeremonyService.ContributionTokenInfo tokenInfo = ceremonyService.verifyContributionToken(token);

            if (!tokenInfo.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "error", tokenInfo.getErrorMessage()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "ceremonyName", tokenInfo.getCeremonyName(),
                    "custodianName", tokenInfo.getCustodianName(),
                    "custodianLabel", tokenInfo.getCustodianLabel()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "Invalid token"
            ));
        }
    }

    /**
     * Validates passphrase strength (API endpoint for real-time feedback)
     */
    @PostMapping("/api/contributions/validate-passphrase")
    @ResponseBody
    public ResponseEntity<?> validatePassphrase(@RequestBody PassphraseValidationRequest request) {
        try {
            PassphraseService.PassphraseValidationResult result =
                    passphraseService.validatePassphrase(request.getPassphrase());

            return ResponseEntity.ok(Map.of(
                    "valid", result.isValid(),
                    "length", result.getLength(),
                    "hasUppercase", result.isHasUppercase(),
                    "hasLowercase", result.isHasLowercase(),
                    "hasDigit", result.isHasDigit(),
                    "hasSpecial", result.isHasSpecial(),
                    "entropyScore", result.getEntropyScore() != null ? result.getEntropyScore() : BigDecimal.ZERO,
                    "strength", result.getStrength() != null ? result.getStrength().name() : "WEAK",
                    "meetsRecommendedLength", result.isMeetsRecommendedLength(),
                    "errorMessage", result.getErrorMessage()
            ));

        } catch (Exception e) {
            log.error("Error validating passphrase", e);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "errorMessage", "Validation error"
            ));
        }
    }

    /**
     * Submits a custodian's passphrase contribution (API endpoint)
     */
    @PostMapping("/api/contributions/{token}")
    @ResponseBody
    public ResponseEntity<?> submitContribution(
            @PathVariable String token,
            @RequestBody ContributionSubmissionRequest request,
            HttpServletRequest httpRequest) {

        log.info("Receiving contribution for token: {}", token.substring(0, 8) + "...");

        try {
            // Validate passphrases match
            if (!request.getPassphrase().equals(request.getPassphraseConfirm())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Passphrases do not match"
                ));
            }

            // Build service request
            CeremonyService.ContributionSubmissionRequest serviceRequest =
                    CeremonyService.ContributionSubmissionRequest.builder()
                            .passphrase(request.getPassphrase())
                            .ipAddress(getClientIpAddress(httpRequest))
                            .userAgent(httpRequest.getHeader("User-Agent"))
                            .build();

            PassphraseContribution contribution = ceremonyService.submitContribution(token, serviceRequest);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "contributionId", contribution.getContributionId(),
                    "strength", contribution.getPassphraseStrength().name(),
                    "entropyScore", contribution.getPassphraseEntropyScore(),
                    "contributedAt", contribution.getContributedAt().toString(),
                    "message", "Contribution submitted successfully"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Validation error submitting contribution", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            log.error("State error submitting contribution", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error submitting contribution", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred"
            ));
        }
    }

    /**
     * Shows the contribution success page
     */
    @GetMapping("/hsm/contribute/success")
    public String showSuccessPage(
            @RequestParam String contributionId,
            @RequestParam String strength,
            @RequestParam String entropyScore,
            @RequestParam String custodianName,
            Model model) {

        log.info("Showing success page for contribution: {}", contributionId);

        model.addAttribute("contributionId", contributionId);
        model.addAttribute("strength", strength);
        model.addAttribute("entropyScore", entropyScore);
        model.addAttribute("custodianName", custodianName);

        return "contribution/success";
    }

    // ===== Helper Methods =====

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    // ===== Request DTOs =====

    @lombok.Data
    public static class PassphraseValidationRequest {
        private String passphrase;
    }

    @lombok.Data
    public static class ContributionSubmissionRequest {
        private String passphrase;
        private String passphraseConfirm;
        private boolean confirmed;
    }
}
