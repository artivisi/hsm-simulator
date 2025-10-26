package com.artivisi.hsm.simulator.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Map;

/**
 * Service for rendering Mustache templates
 */
@Service
@Slf4j
public class TemplateService {

    private final MustacheFactory mustacheFactory;

    public TemplateService() {
        this.mustacheFactory = new DefaultMustacheFactory("templates/email");
    }

    /**
     * Renders a Mustache template with the given data
     *
     * @param templateName Name of the template file (without .mustache extension)
     * @param data Data to pass to the template
     * @return Rendered HTML string
     */
    public String render(String templateName, Map<String, Object> data) {
        try {
            Mustache mustache = mustacheFactory.compile(templateName + ".mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, data).flush();
            return writer.toString();
        } catch (Exception e) {
            log.error("Failed to render template: {}", templateName, e);
            throw new TemplateRenderException("Failed to render template: " + templateName, e);
        }
    }

    /**
     * Custom exception for template rendering failures
     */
    public static class TemplateRenderException extends RuntimeException {
        public TemplateRenderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
