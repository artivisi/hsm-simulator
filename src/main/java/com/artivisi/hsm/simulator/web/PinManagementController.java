package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.GeneratedPin;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.GeneratedPinRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.service.PinGenerationService;
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
@RequestMapping("/pins")
public class PinManagementController {

    private final PinGenerationService pinGenerationService;
    private final GeneratedPinRepository generatedPinRepository;
    private final MasterKeyRepository masterKeyRepository;

    /**
     * Show PIN list page
     */
    @GetMapping
    public String listPins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());
        Page<GeneratedPin> pinsPage = generatedPinRepository.findAll(pageable);

        long totalPins = generatedPinRepository.count();
        long activePins = generatedPinRepository.countByStatus(GeneratedPin.PinStatus.ACTIVE);
        long blockedPins = generatedPinRepository.countByStatus(GeneratedPin.PinStatus.BLOCKED);

        model.addAttribute("pinsPage", pinsPage);
        model.addAttribute("totalPins", totalPins);
        model.addAttribute("activePins", activePins);
        model.addAttribute("blockedPins", blockedPins);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "pins/list";
    }

    /**
     * Show PIN generation form
     */
    @GetMapping("/generate")
    public String showGenerateForm(Model model) {
        // Get LMK keys for PIN storage encryption (primary)
        // TPK and ZPK can also be used for transmission scenarios
        List<MasterKey> pinKeys = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                .stream()
                .filter(k -> k.getKeyType() == KeyType.LMK ||
                            k.getKeyType() == KeyType.TPK ||
                            k.getKeyType() == KeyType.ZPK)
                .toList();

        model.addAttribute("pinKeys", pinKeys);
        return "pins/generate";
    }

    /**
     * Generate a new PIN
     */
    @PostMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<?> generatePin(
            @RequestParam UUID keyId,
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "4") Integer pinLength,
            @RequestParam(defaultValue = "ISO-0") String pinFormat,
            Principal principal
    ) {
        log.info("User {} generating PIN for account {}", principal.getName(), accountNumber);

        try {
            // Validate input
            if (pinLength < 4 || pinLength > 12) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "PIN length must be between 4 and 12 digits"
                ));
            }

            if (generatedPinRepository.existsByAccountNumber(accountNumber)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "PIN already exists for this account. Please verify or reset first."
                ));
            }

            GeneratedPin generatedPin = pinGenerationService.generatePin(
                    keyId, accountNumber, pinLength, pinFormat);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "PIN generated successfully",
                    "accountNumber", generatedPin.getAccountNumber(),
                    "clearPin", generatedPin.getClearPin(),
                    "pvv", generatedPin.getPinVerificationValue(),
                    "encryptedPinBlock", generatedPin.getEncryptedPinBlock()
            ));
        } catch (Exception e) {
            log.error("Error generating PIN", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Verify a PIN
     */
    @PostMapping("/api/verify")
    @ResponseBody
    public ResponseEntity<?> verifyPin(
            @RequestParam String accountNumber,
            @RequestParam String pin,
            Principal principal
    ) {
        log.info("User {} verifying PIN for account {}", principal.getName(), accountNumber);

        try {
            boolean isValid = pinGenerationService.verifyPin(accountNumber, pin);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", isValid,
                    "message", isValid ? "PIN verified successfully" : "Invalid PIN"
            ));
        } catch (Exception e) {
            log.error("Error verifying PIN", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Translate PIN from one key to another
     */
    @PostMapping("/api/translate")
    @ResponseBody
    public ResponseEntity<?> translatePin(
            @RequestParam UUID sourcePinId,
            @RequestParam UUID targetKeyId,
            Principal principal
    ) {
        log.info("User {} translating PIN from {} to key {}",
                 principal.getName(), sourcePinId, targetKeyId);

        try {
            String translatedPinBlock = pinGenerationService.translatePin(sourcePinId, targetKeyId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "PIN translated successfully",
                    "encryptedPinBlock", translatedPinBlock
            ));
        } catch (Exception e) {
            log.error("Error translating PIN", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete a PIN
     */
    @PostMapping("/{pinId}/delete")
    public String deletePin(
            @PathVariable UUID pinId,
            Principal principal
    ) {
        log.info("User {} deleting PIN {}", principal.getName(), pinId);

        try {
            GeneratedPin pin = generatedPinRepository.findById(pinId)
                    .orElseThrow(() -> new IllegalArgumentException("PIN not found: " + pinId));

            generatedPinRepository.delete(pin);
            log.info("PIN deleted successfully for account {}", pin.getAccountNumber());

            return "redirect:/pins?success=PIN+deleted+successfully";
        } catch (Exception e) {
            log.error("Error deleting PIN", e);
            return "redirect:/pins?error=" + e.getMessage();
        }
    }
}
