package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.Bank;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.entity.Terminal;
import com.artivisi.hsm.simulator.repository.BankRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.repository.TerminalRepository;
import com.artivisi.hsm.simulator.service.KeyOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/keys")
public class KeyOperationController {

    private final KeyOperationService keyOperationService;
    private final BankRepository bankRepository;
    private final TerminalRepository terminalRepository;
    private final MasterKeyRepository masterKeyRepository;

    /**
     * Show key generation form
     */
    @GetMapping("/generate")
    public String showGenerateForm(Model model) {
        model.addAttribute("banks", bankRepository.findAll());
        model.addAttribute("terminals", terminalRepository.findAll());
        model.addAttribute("tmks", masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                .stream().filter(k -> "TMK".equals(k.getKeyType())).toList());
        model.addAttribute("zmks", masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                .stream().filter(k -> "ZMK".equals(k.getKeyType())).toList());

        return "keys/generate";
    }

    /**
     * Generate TMK
     */
    @PostMapping("/api/generate/tmk")
    @ResponseBody
    public ResponseEntity<?> generateTMK(
            @RequestParam UUID bankId,
            @RequestParam(defaultValue = "256") Integer keySize,
            @RequestParam(required = false) String description,
            Principal principal
    ) {
        log.info("User {} requesting TMK generation for bank {}", principal.getName(), bankId);

        try {
            MasterKey tmk = keyOperationService.generateTMK(bankId, keySize, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TMK generated successfully",
                    "keyId", tmk.getMasterKeyId(),
                    "fingerprint", tmk.getKeyFingerprint()
            ));
        } catch (Exception e) {
            log.error("Error generating TMK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate TPK
     */
    @PostMapping("/api/generate/tpk")
    @ResponseBody
    public ResponseEntity<?> generateTPK(
            @RequestParam UUID tmkId,
            @RequestParam UUID terminalId,
            @RequestParam(required = false) String description,
            Principal principal
    ) {
        log.info("User {} requesting TPK generation from TMK {}", principal.getName(), tmkId);

        try {
            MasterKey tpk = keyOperationService.generateTPK(tmkId, terminalId, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TPK generated successfully",
                    "keyId", tpk.getMasterKeyId(),
                    "fingerprint", tpk.getKeyFingerprint()
            ));
        } catch (Exception e) {
            log.error("Error generating TPK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate TSK
     */
    @PostMapping("/api/generate/tsk")
    @ResponseBody
    public ResponseEntity<?> generateTSK(
            @RequestParam UUID tmkId,
            @RequestParam UUID terminalId,
            @RequestParam(required = false) String description,
            Principal principal
    ) {
        log.info("User {} requesting TSK generation from TMK {}", principal.getName(), tmkId);

        try {
            MasterKey tsk = keyOperationService.generateTSK(tmkId, terminalId, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TSK generated successfully",
                    "keyId", tsk.getMasterKeyId(),
                    "fingerprint", tsk.getKeyFingerprint()
            ));
        } catch (Exception e) {
            log.error("Error generating TSK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate ZMK
     */
    @PostMapping("/api/generate/zmk")
    @ResponseBody
    public ResponseEntity<?> generateZMK(
            @RequestParam UUID bankId,
            @RequestParam(defaultValue = "256") Integer keySize,
            @RequestParam(required = false) String description,
            Principal principal
    ) {
        log.info("User {} requesting ZMK generation for bank {}", principal.getName(), bankId);

        try {
            MasterKey zmk = keyOperationService.generateZMK(bankId, keySize, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ZMK generated successfully",
                    "keyId", zmk.getMasterKeyId(),
                    "fingerprint", zmk.getKeyFingerprint()
            ));
        } catch (Exception e) {
            log.error("Error generating ZMK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate ZPK
     */
    @PostMapping("/api/generate/zpk")
    @ResponseBody
    public ResponseEntity<?> generateZPK(
            @RequestParam UUID zmkId,
            @RequestParam String zoneIdentifier,
            @RequestParam(required = false) String description,
            Principal principal
    ) {
        log.info("User {} requesting ZPK generation from ZMK {}", principal.getName(), zmkId);

        try {
            MasterKey zpk = keyOperationService.generateZPK(zmkId, zoneIdentifier, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ZPK generated successfully",
                    "keyId", zpk.getMasterKeyId(),
                    "fingerprint", zpk.getKeyFingerprint()
            ));
        } catch (Exception e) {
            log.error("Error generating ZPK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate ZSK
     */
    @PostMapping("/api/generate/zsk")
    @ResponseBody
    public ResponseEntity<?> generateZSK(
            @RequestParam UUID zmkId,
            @RequestParam String zoneIdentifier,
            @RequestParam(required = false) String description,
            Principal principal
    ) {
        log.info("User {} requesting ZSK generation from ZMK {}", principal.getName(), zmkId);

        try {
            MasterKey zsk = keyOperationService.generateZSK(zmkId, zoneIdentifier, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ZSK generated successfully",
                    "keyId", zsk.getMasterKeyId(),
                    "fingerprint", zsk.getKeyFingerprint()
            ));
        } catch (Exception e) {
            log.error("Error generating ZSK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Revoke a key (server-side POST)
     */
    @PostMapping("/{keyId}/revoke")
    public String revokeKey(
            @PathVariable UUID keyId,
            @RequestParam String reason,
            Principal principal
    ) {
        log.info("User {} requesting revocation of key {}", principal.getName(), keyId);

        try {
            keyOperationService.revokeKey(keyId, reason, principal.getName());
            return "redirect:/keys?success=Key+revoked+successfully";
        } catch (Exception e) {
            log.error("Error revoking key", e);
            return "redirect:/keys?error=" + e.getMessage();
        }
    }

    /**
     * Revoke a key (API endpoint for programmatic access)
     */
    @PostMapping("/api/revoke/{keyId}")
    @ResponseBody
    public ResponseEntity<?> revokeKeyApi(
            @PathVariable UUID keyId,
            @RequestParam String reason,
            Principal principal
    ) {
        log.info("User {} requesting revocation of key {}", principal.getName(), keyId);

        try {
            keyOperationService.revokeKey(keyId, reason, principal.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Key revoked successfully"
            ));
        } catch (Exception e) {
            log.error("Error revoking key", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
