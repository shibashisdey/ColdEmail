package com.shibashis.coldmailer.v1.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class TemplateRenderer {

    private final TemplateEngine templateEngine;

    @Autowired
    public TemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renders an HTML email body using Thymeleaf.
     *
     * @param templateBody The raw HTML template string containing Thymeleaf syntax like [[${variable}]].
     * @param variables A map of variables to be made available to the template.
     * @return The rendered HTML as a String.
     */
    public String render(String templateBody, Map<String, Object> variables) {
        if (templateBody == null) {
            return "";
        }
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateBody, context);
    }
}
