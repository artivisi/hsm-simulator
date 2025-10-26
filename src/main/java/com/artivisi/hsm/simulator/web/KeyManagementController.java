package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/keys")
public class KeyManagementController {

    private final MasterKeyRepository masterKeyRepository;

    @GetMapping
    public String listKeys(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "generatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model
    ) {
        log.info("Listing master keys - page: {}, size: {}, sortBy: {}, direction: {}",
                 page, size, sortBy, direction);

        // Create pageable with sorting
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get paginated results
        Page<MasterKey> masterKeysPage = masterKeyRepository.findAll(pageable);

        // Calculate statistics using repository count queries
        long totalKeys = masterKeyRepository.count();
        long activeKeys = masterKeyRepository.countByStatus(MasterKey.KeyStatus.ACTIVE);
        long recoveredKeys = masterKeyRepository.countByGenerationMethod("RECOVERED");
        long revokedKeys = masterKeyRepository.countByStatus(MasterKey.KeyStatus.REVOKED);

        // Calculate pagination display values
        long showingFrom = masterKeysPage.getNumber() * masterKeysPage.getSize() + 1;
        long showingTo = Math.min((masterKeysPage.getNumber() + 1) * masterKeysPage.getSize(),
                                   masterKeysPage.getTotalElements());

        model.addAttribute("masterKeysPage", masterKeysPage);
        model.addAttribute("totalKeys", totalKeys);
        model.addAttribute("activeKeys", activeKeys);
        model.addAttribute("recoveredKeys", recoveredKeys);
        model.addAttribute("revokedKeys", revokedKeys);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("showingFrom", showingFrom);
        model.addAttribute("showingTo", showingTo);

        return "keys/list";
    }
}
