package com.notificationservice.pipeline;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.domain.TemplateStatus;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TemplateRenderHandlerTest {

    private final TemplateRenderHandler handler = new TemplateRenderHandler();

    @Test
    void shouldRenderTemplateVariablesCorrectly() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setSubject("Hello {{name}}");
        template.setBodyTemplate("Your order {{orderId}} is confirmed.");

        NotificationContext context = NotificationContext.builder()
                .template(template)
                .variables(Map.of("name", "Alice", "orderId", "12345"))
                .channel(Channel.EMAIL)
                .build();

        NotificationHandlerChain chain = new NotificationHandlerChain(java.util.List.of(handler));

        // Act
        chain.next(context);

        // Assert
        assertEquals("Hello Alice", context.getRenderedSubject());
        assertEquals("Your order 12345 is confirmed.", context.getRenderedBody());
    }

    @Test
    void shouldThrowExceptionWhenVariableIsMissing() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setBodyTemplate("Missing {{requiredVar}}");

        NotificationContext context = NotificationContext.builder()
                .template(template)
                .variables(Map.of()) // Empty variables
                .channel(Channel.EMAIL)
                .build();

        NotificationHandlerChain chain = new NotificationHandlerChain(java.util.List.of(handler));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            chain.next(context);
        });
        assertTrue(ex.getMessage().contains("Missing template variables"));
        assertTrue(ex.getMessage().contains("requiredVar"));
    }
}
