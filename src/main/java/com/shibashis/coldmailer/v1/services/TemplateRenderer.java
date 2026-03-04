package com.shibashis.coldmailer.v1.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateRenderer {

    private static final Pattern MUSTACHE_VAR_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

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
        String normalizedTemplate = convertMustacheToThymeleaf(templateBody);
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(normalizedTemplate, context);
    }

    private String convertMustacheToThymeleaf(String templateBody) {
        Matcher matcher = MUSTACHE_VAR_PATTERN.matcher(templateBody);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String originalName = matcher.group(1);
            String normalizedName = toCamelCase(originalName);
            matcher.appendReplacement(out, "[[\\${" + normalizedName + "}]]");
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String toCamelCase(String name) {
        String[] parts = name.split("_");
        if (parts.length == 0) {
            return name;
        }
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            result.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return result.toString();
    }
}
