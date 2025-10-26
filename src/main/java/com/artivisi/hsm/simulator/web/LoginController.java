package com.artivisi.hsm.simulator.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
            log.warn("Failed login attempt");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
            log.info("User logged out");
        }

        return "auth/login";
    }
}
