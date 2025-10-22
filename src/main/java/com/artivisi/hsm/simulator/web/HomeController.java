package com.artivisi.hsm.simulator.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@PropertySource(value = "classpath:git.properties", ignoreResourceNotFound = true)
public class HomeController {

    @Value("${git.commit.id.abbrev:unknown}")
    private String gitCommitId;

    @Value("${git.branch:unknown}")
    private String gitBranch;

    @Value("${git.commit.time:unknown}")
    private String gitCommitTime;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("gitCommitId", gitCommitId);
        model.addAttribute("gitBranch", gitBranch);
        model.addAttribute("gitCommitTime", gitCommitTime);

        log.info("Home page accessed - Git: {} on branch {} at {}", gitCommitId, gitBranch, gitCommitTime);

        return "index";
    }
}
