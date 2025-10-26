package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.service.OfflineRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for offline key recovery (no database required)
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/hsm/recovery")
public class KeyRecoveryController {

    private final OfflineRecoveryService offlineRecoveryService;

    // ===== Offline Recovery (no database required) =====

    /**
     * Default route redirects to offline recovery
     */
    @GetMapping
    public String defaultRoute() {
        return "redirect:/hsm/recovery/offline";
    }

    /**
     * Shows offline recovery page
     */
    @GetMapping("/offline")
    public String showOfflineRecovery() {
        log.info("Showing offline recovery page");
        return "recovery/offline";
    }

    /**
     * Parses uploaded share files
     */
    @PostMapping("/offline/parse")
    @ResponseBody
    public ResponseEntity<?> parseShareFiles(@RequestParam("files") List<MultipartFile> files) {
        log.info("Parsing {} share files", files.size());

        try {
            List<OfflineRecoveryService.ParsedShare> parsedShares = new ArrayList<>();

            for (MultipartFile file : files) {
                OfflineRecoveryService.ParsedShare share = offlineRecoveryService.parseShareFile(file);
                parsedShares.add(share);
            }

            int threshold = parsedShares.isEmpty() ? 0 : parsedShares.get(0).getThreshold();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "parsedShares", parsedShares.stream().map(share -> Map.of(
                            "shareId", share.getShareId(),
                            "shareIndex", share.getShareIndex(),
                            "threshold", share.getThreshold(),
                            "fileName", share.getFileName(),
                            "ceremonyName", share.getCeremonyName() != null ? share.getCeremonyName() : "Unknown",
                            "ceremonyId", share.getCeremonyId() != null ? share.getCeremonyId() : "Unknown",
                            "masterKeyFingerprint", share.getMasterKeyFingerprint() != null ? share.getMasterKeyFingerprint() : "N/A"
                    )).collect(Collectors.toList()),
                    "sharesUploaded", parsedShares.size(),
                    "sharesRequired", threshold,
                    "canReconstruct", parsedShares.size() >= threshold && threshold > 0,
                    "message", parsedShares.size() >= threshold && threshold > 0
                            ? "Threshold met. Ready to reconstruct."
                            : "Need " + (threshold - parsedShares.size()) + " more share(s)"
            ));

        } catch (Exception e) {
            log.error("Error parsing share files", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Reconstructs key from uploaded files with passphrases
     */
    @PostMapping("/offline/reconstruct")
    @ResponseBody
    public ResponseEntity<?> reconstructFromFiles(@RequestParam("files") List<MultipartFile> files,
                                                   @RequestParam("passphrases") List<String> passphrases) {
        log.info("Reconstructing key from {} share files with passphrases", files.size());

        try {
            if (files.size() != passphrases.size()) {
                throw new IllegalArgumentException("Number of files must match number of passphrases");
            }

            List<OfflineRecoveryService.ShareWithPassphrase> sharesWithPassphrases = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                OfflineRecoveryService.ParsedShare share = offlineRecoveryService.parseShareFile(files.get(i));
                sharesWithPassphrases.add(OfflineRecoveryService.ShareWithPassphrase.builder()
                        .share(share)
                        .passphrase(passphrases.get(i))
                        .build());
            }

            OfflineRecoveryService.OfflineRecoveryResult result =
                    offlineRecoveryService.reconstructFromFiles(sharesWithPassphrases);

            return ResponseEntity.ok(Map.of(
                    "success", result.isSuccess(),
                    "verified", result.isVerified(),
                    "canVerify", result.isCanVerify(),
                    "sharesUsed", result.getSharesUsed(),
                    "threshold", result.getThreshold(),
                    "reconstructedFingerprint", result.getReconstructedFingerprint(),
                    "originalFingerprint", result.getOriginalFingerprint() != null ? result.getOriginalFingerprint() : "N/A",
                    "ceremonyName", result.getCeremonyName() != null ? result.getCeremonyName() : "Unknown",
                    "ceremonyId", result.getCeremonyId() != null ? result.getCeremonyId() : "Unknown",
                    "message", result.getMessage()
            ));

        } catch (Exception e) {
            log.error("Error reconstructing from files", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
