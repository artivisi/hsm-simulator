package com.artivisi.hsm.simulator.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

@Controller
@ControllerAdvice
public class GitInfoController {

    @Autowired
    private Environment environment;

    private String gitCommitId;
    private String gitBranch;
    private String gitCommitTime;
    private String githubUrl;

    @PostConstruct
    public void init() {
        try {
            Properties gitProperties = new Properties();
            gitProperties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));

            gitCommitId = gitProperties.getProperty("git.commit.id.abbrev", "unknown");
            gitBranch = gitProperties.getProperty("git.branch", "unknown");
            gitCommitTime = gitProperties.getProperty("git.commit.time", "unknown");

            // GitHub URL - points to the Artivisi repository
            githubUrl = "https://github.com/artivisi/hsm-simulator";
        } catch (Exception e) {
            gitCommitId = "unknown";
            gitBranch = "unknown";
            gitCommitTime = "unknown";
            githubUrl = "https://github.com/artivisi/hsm-simulator";
        }
    }

    @ModelAttribute
    public void addGitInfo(Model model) {
        model.addAttribute("gitCommitId", gitCommitId);
        model.addAttribute("gitBranch", gitBranch);
        model.addAttribute("gitCommitTime", gitCommitTime);
        model.addAttribute("githubUrl", githubUrl);
    }
}