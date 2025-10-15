package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.*;
import com.artivisi.hsm.simulator.repository.CeremonyCustodianRepository;
import com.artivisi.hsm.simulator.service.KeyCeremonyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/key-ceremony")
@RequiredArgsConstructor
@Slf4j
public class KeyCeremonyController {

    private final KeyCeremonyService keyCeremonyService;
    private final CeremonyCustodianRepository ceremonyCustodianRepository;

    /**
     * Show key ceremony dashboard
     */
    @GetMapping
    public String dashboard(Model model) {
        try {
            // Check if master key exists
            boolean hasActiveMasterKey = keyCeremonyService.hasActiveMasterKey();
            MasterKey activeMasterKey = keyCeremonyService.getActiveMasterKey();

            // Check if should initiate ceremony
            boolean shouldInitiate = keyCeremonyService.shouldInitiateKeyCeremony();

            // Get active ceremonies
            List<KeyCeremony> activeCeremonies = keyCeremonyService.getActiveCeremonies();

            model.addAttribute("hasActiveMasterKey", hasActiveMasterKey);
            model.addAttribute("activeMasterKey", activeMasterKey);
            model.addAttribute("shouldInitiate", shouldInitiate);
            model.addAttribute("activeCeremonies", activeCeremonies);

            return "key-ceremony/dashboard";
        } catch (Exception e) {
            log.error("Error loading key ceremony dashboard", e);
            model.addAttribute("error", "Failed to load dashboard: " + e.getMessage());
            return "key-ceremony/dashboard";
        }
    }

    /**
     * Show form to start new key ceremony
     */
    @GetMapping("/new")
    public String showNewCeremonyForm(Model model) {
        // Check if already has master key
        if (keyCeremonyService.hasActiveMasterKey()) {
            return "redirect:/key-ceremony";
        }

        model.addAttribute("ceremonyForm", new CeremonyForm());
        return "key-ceremony/new";
    }

    /**
     * Process new key ceremony form
     */
    @PostMapping("/new")
    public String processNewCeremony(@ModelAttribute CeremonyForm form, RedirectAttributes redirectAttributes) {
        try {
            // Validate form
            if (form.getCeremonyName() == null || form.getCeremonyName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Ceremony name is required");
                return "redirect:/key-ceremony/new";
            }

            if (form.getPurpose() == null || form.getPurpose().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Purpose is required");
                return "redirect:/key-ceremony/new";
            }

            // Initiate ceremony
            KeyCeremony ceremony = keyCeremonyService.initiateKeyCeremony(
                form.getCeremonyName(),
                form.getPurpose(),
                "system"
            );

            redirectAttributes.addFlashAttribute("success",
                "Key ceremony initiated successfully! Ceremony ID: " + ceremony.getCeremonyId());

            return "redirect:/key-ceremony/" + ceremony.getCeremonyId();

        } catch (Exception e) {
            log.error("Error initiating key ceremony", e);
            redirectAttributes.addFlashAttribute("error", "Failed to initiate ceremony: " + e.getMessage());
            return "redirect:/key-ceremony/new";
        }
    }

    /**
     * Show ceremony details
     */
    @GetMapping("/{ceremonyId}")
    public String showCeremonyDetails(@PathVariable String ceremonyId, Model model) {
        try {
            KeyCeremony ceremony = keyCeremonyService.getCeremonyWithDetails(ceremonyId);
            List<CeremonyCustodian> custodians = keyCeremonyService.getCeremonyCustodians(ceremony.getId());

            // Calculate progress
            long contributionCount = keyCeremonyService.getContributionCount(ceremony.getId());
            boolean canComplete = keyCeremonyService.canCompleteCeremony(ceremony.getId());
            int progressPercentage = (int) ((contributionCount / 3.0) * 100);

            model.addAttribute("ceremony", ceremony);
            model.addAttribute("custodians", custodians);
            model.addAttribute("contributionCount", contributionCount);
            model.addAttribute("canComplete", canComplete);
            model.addAttribute("progressPercentage", progressPercentage);

            return "key-ceremony/details";
        } catch (Exception e) {
            log.error("Error loading ceremony details for {}", ceremonyId, e);
            model.addAttribute("error", "Ceremony not found or error loading details");
            return "key-ceremony/details";
        }
    }

    /**
     * Show contribution page for custodian
     */
    @GetMapping("/contribute/{token}")
    public String showContributionPage(@PathVariable String token, Model model) {
        try {
            // Find ceremony custodian by token
            CeremonyCustodian ceremonyCustodian = ceremonyCustodianRepository.findByContributionToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid contribution token"));

            // Check if contribution already submitted
            if (ceremonyCustodian.getContributionStatus() == CeremonyCustodian.ContributionStatus.CONTRIBUTED) {
                model.addAttribute("error", "Contribution already received for this token");
                return "key-ceremony/contribute-error";
            }

            // Check if ceremony is still active
            KeyCeremony ceremony = ceremonyCustodian.getKeyCeremony();
            if (ceremony.getStatus() == KeyCeremony.CeremonyStatus.COMPLETED ||
                ceremony.getStatus() == KeyCeremony.CeremonyStatus.CANCELLED) {
                model.addAttribute("error", "Ceremony is no longer accepting contributions");
                return "key-ceremony/contribute-error";
            }

            // Check if deadline has passed
            if (ceremony.getContributionDeadline() != null &&
                ceremony.getContributionDeadline().isBefore(java.time.LocalDateTime.now())) {
                model.addAttribute("error", "Contribution deadline has passed");
                return "key-ceremony/contribute-error";
            }

            model.addAttribute("token", token);
            model.addAttribute("ceremonyCustodian", ceremonyCustodian);
            model.addAttribute("contributionForm", new ContributionForm());

            log.info("Custodian {} ({}) accessing contribution page for ceremony {}",
                    ceremonyCustodian.getKeyCustodian().getFullName(),
                    ceremonyCustodian.getKeyCustodian().getEmail(),
                    ceremony.getCeremonyId());

            return "key-ceremony/contribute";
        } catch (Exception e) {
            log.error("Error loading contribution page for token {}", token, e);
            model.addAttribute("error", "Invalid or expired contribution link");
            return "key-ceremony/contribute-error";
        }
    }

    /**
     * Process custodian contribution
     */
    @PostMapping("/contribute/{token}")
    public String processContribution(@PathVariable String token,
                                    @ModelAttribute ContributionForm form,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Find ceremony custodian by token
            CeremonyCustodian ceremonyCustodian = ceremonyCustodianRepository.findByContributionToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid contribution token"));

            // Validate passphrase
            if (form.getPassphrase() == null || form.getPassphrase().length() < 16) {
                redirectAttributes.addFlashAttribute("error",
                    "Passphrase must be at least 16 characters long");
                return "redirect:/key-ceremony/contribute/" + token;
            }

            // Additional validation
            KeyCeremony ceremony = ceremonyCustodian.getKeyCeremony();
            if (ceremonyCustodian.getContributionStatus() == CeremonyCustodian.ContributionStatus.CONTRIBUTED) {
                redirectAttributes.addFlashAttribute("error", "Contribution already received");
                return "redirect:/key-ceremony/contribute/" + token;
            }

            // Log contribution details
            log.info("CONTRIBUTION RECEIVED:");
            log.info("Ceremony ID: {}", ceremony.getCeremonyId());
            log.info("Custodian: {} ({})",
                    ceremonyCustodian.getKeyCustodian().getFullName(),
                    ceremonyCustodian.getKeyCustodian().getEmail());
            log.info("Custodian Label: {}", ceremonyCustodian.getCustodianLabel());
            log.info("Passphrase Length: {} characters", form.getPassphrase().length());
            log.info("Passphrase Hash: {}", calculateHash(form.getPassphrase()));
            log.info("Contribution Time: {}", LocalDateTime.now());
            log.info("Contribution Token: {}", token);
            log.info("--------------------------------------------------------");

            // Update ceremony custodian status (simplified - in real implementation, would store in database)
            ceremonyCustodian.setContributionStatus(CeremonyCustodian.ContributionStatus.CONTRIBUTED);
            ceremonyCustodian.setContributedAt(LocalDateTime.now());
            ceremonyCustodianRepository.save(ceremonyCustodian);

            // Check if all contributions received and update ceremony status
            long contributionCount = ceremonyCustodianRepository.countContributionsByCeremony(ceremony.getId());
            if (contributionCount >= ceremony.getThreshold()) {
                ceremony.setStatus(KeyCeremony.CeremonyStatus.GENERATING_KEY);
                keyCeremonyService.updateCeremonyStatus(ceremony);
                log.info("Ceremony {} threshold reached, ready for master key generation", ceremony.getCeremonyId());
            }

            redirectAttributes.addFlashAttribute("success",
                "Thank you! Your contribution has been recorded successfully.");

            return "redirect:/key-ceremony/contribute-success";

        } catch (Exception e) {
            log.error("Error processing contribution for token {}", token, e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to process contribution: " + e.getMessage());
            return "redirect:/key-ceremony/contribute/" + token;
        }
    }

    /**
     * Calculate SHA-256 hash for logging purposes
     */
    private String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "Hash calculation failed";
        }
    }

    /**
     * Show contribution success page
     */
    @GetMapping("/contribute-success")
    public String showContributionSuccess() {
        return "key-ceremony/contribute-success";
    }

    /**
     * API endpoint to check master key status
     */
    @GetMapping("/api/master-key-status")
    @ResponseBody
    public Map<String, Object> getMasterKeyStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean hasActiveMasterKey = keyCeremonyService.hasActiveMasterKey();
            MasterKey activeMasterKey = keyCeremonyService.getActiveMasterKey();
            boolean shouldInitiate = keyCeremonyService.shouldInitiateKeyCeremony();

            response.put("hasActiveMasterKey", hasActiveMasterKey);
            response.put("shouldInitiate", shouldInitiate);

            if (activeMasterKey != null) {
                response.put("masterKeyId", activeMasterKey.getMasterKeyId());
                response.put("algorithm", activeMasterKey.getAlgorithm());
                response.put("keySize", activeMasterKey.getKeySize());
                response.put("generatedAt", activeMasterKey.getGeneratedAt());
                response.put("fingerprint", activeMasterKey.getKeyFingerprint());
            }

            response.put("status", "success");
        } catch (Exception e) {
            log.error("Error checking master key status", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // Form classes
    public static class CeremonyForm {
        private String ceremonyName;
        private String purpose;

        public String getCeremonyName() { return ceremonyName; }
        public void setCeremonyName(String ceremonyName) { this.ceremonyName = ceremonyName; }

        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }

    public static class ContributionForm {
        private String passphrase;

        public String getPassphrase() { return passphrase; }
        public void setPassphrase(String passphrase) { this.passphrase = passphrase; }
    }
}