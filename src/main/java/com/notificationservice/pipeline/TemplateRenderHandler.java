package com.notificationservice.pipeline;

import com.notificationservice.domain.NotificationTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;

/**
 * Handler 5 — Renders the template by substituting {{variable}} placeholders.
 *
 * Also validates that all placeholders in the template are satisfied
 * by the provided variables map.
 */
@Component
@Slf4j
public class TemplateRenderHandler implements NotificationHandler {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    @Override
    public void handle(NotificationContext context, NotificationHandlerChain chain) {
        NotificationTemplate template = context.getTemplate();
        Map<String, String> variables = context.getVariables() != null
                ? context.getVariables() : Map.of();

        // Validate all required placeholders are provided
        validatePlaceholders(template.getBodyTemplate(), variables, "body");
        if (template.getSubject() != null) {
            validatePlaceholders(template.getSubject(), variables, "subject");
        }

        // Render
        String renderedBody = render(template.getBodyTemplate(), variables);
        String renderedSubject = template.getSubject() != null
                ? render(template.getSubject(), variables) : null;

        context.setRenderedBody(renderedBody);
        context.setRenderedSubject(renderedSubject);

        log.debug("Template rendered for request on channel={}", context.getChannel());
        chain.next(context);
    }

    @Override
    public int getOrder() { return 5; }

    public String render(String template, Map<String, String> variables) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "{{" + key + "}}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void validatePlaceholders(String template, Map<String, String> variables, String field) {
        Set<String> missing = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!variables.containsKey(key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing template variables in " + field + ": " + missing);
        }
    }
}
