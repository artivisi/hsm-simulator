package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.GeneratedMac;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.GeneratedMacRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.service.MacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/macs")
public class MacController {

    private final MacService macService;
    private final GeneratedMacRepository generatedMacRepository;
    private final MasterKeyRepository masterKeyRepository;

    /**
     * Show MAC list page
     */
    @GetMapping
    public String listMacs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());
        Page<GeneratedMac> macsPage = generatedMacRepository.findAll(pageable);

        long totalMacs = generatedMacRepository.count();
        long activeMacs = generatedMacRepository.countByStatus(GeneratedMac.MacStatus.ACTIVE);
        long expiredMacs = generatedMacRepository.countByStatus(GeneratedMac.MacStatus.EXPIRED);

        model.addAttribute("macsPage", macsPage);
        model.addAttribute("totalMacs", totalMacs);
        model.addAttribute("activeMacs", activeMacs);
        model.addAttribute("expiredMacs", expiredMacs);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "macs/list";
    }

    /**
     * Show MAC generation form
     */
    @GetMapping("/generate")
    public String showGenerateForm(Model model) {
        // Get TSK and ZSK keys for MAC generation
        List<MasterKey> macKeys = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                .stream()
                .filter(k -> k.getKeyType() == KeyType.TSK || k.getKeyType() == KeyType.ZSK)
                .toList();

        model.addAttribute("macKeys", macKeys);
        return "macs/generate";
    }

    /**
     * Generate a new MAC
     */
    @PostMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<?> generateMac(
            @RequestParam UUID keyId,
            @RequestParam String message,
            @RequestParam(defaultValue = "AES-CMAC") String algorithm,
            Principal principal
    ) {
        log.info("User {} generating MAC for message length {}", principal.getName(), message.length());

        try {
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Message cannot be empty"
                ));
            }

            GeneratedMac generatedMac = macService.generateMac(keyId, message, algorithm);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MAC generated successfully",
                    "macValue", generatedMac.getMacValue(),
                    "algorithm", generatedMac.getMacAlgorithm(),
                    "messageLength", generatedMac.getMessageLength()
            ));
        } catch (Exception e) {
            log.error("Error generating MAC", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Show MAC verification form
     */
    @GetMapping("/verify")
    public String showVerifyForm(Model model) {
        // Get TSK and ZSK keys for MAC verification
        List<MasterKey> macKeys = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                .stream()
                .filter(k -> k.getKeyType() == KeyType.TSK || k.getKeyType() == KeyType.ZSK)
                .toList();

        model.addAttribute("macKeys", macKeys);
        return "macs/verify";
    }

    /**
     * Verify a MAC
     */
    @PostMapping("/api/verify")
    @ResponseBody
    public ResponseEntity<?> verifyMac(
            @RequestParam String message,
            @RequestParam String mac,
            @RequestParam UUID keyId,
            @RequestParam(defaultValue = "AES-CMAC") String algorithm,
            Principal principal
    ) {
        log.info("User {} verifying MAC for message length {}", principal.getName(), message.length());

        try {
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Message cannot be empty"
                ));
            }

            if (mac == null || mac.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "MAC value cannot be empty"
                ));
            }

            boolean isValid = macService.verifyMac(message, mac, keyId, algorithm);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", isValid,
                    "message", isValid ? "MAC is valid" : "MAC is invalid"
            ));
        } catch (Exception e) {
            log.error("Error verifying MAC", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete a MAC
     */
    @PostMapping("/{macId}/delete")
    public String deleteMac(
            @PathVariable UUID macId,
            Principal principal
    ) {
        log.info("User {} deleting MAC {}", principal.getName(), macId);

        try {
            GeneratedMac mac = generatedMacRepository.findById(macId)
                    .orElseThrow(() -> new IllegalArgumentException("MAC not found: " + macId));

            generatedMacRepository.delete(mac);
            log.info("MAC deleted successfully");

            return "redirect:/macs?success=MAC+deleted+successfully";
        } catch (Exception e) {
            log.error("Error deleting MAC", e);
            return "redirect:/macs?error=" + e.getMessage();
        }
    }
}
