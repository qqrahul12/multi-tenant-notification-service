package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.domain.NotificationRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class EmailChannelDispatcher extends AbstractChannelDispatcher {

    @Override
    protected String doSend(NotificationRequest request, NotificationChannelConfig config) throws Exception {
        Map<String, String> cfg = config.getConfig();
        
        if (!cfg.containsKey("smtpHost") || !cfg.containsKey("smtpUsername") || !cfg.containsKey("smtpPassword")) {
            throw new IllegalArgumentException("Missing required SMTP configuration (smtpHost, smtpUsername, smtpPassword)");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(cfg.get("smtpHost"));
        mailSender.setPort(Integer.parseInt(cfg.getOrDefault("smtpPort", "587")));
        mailSender.setUsername(cfg.get("smtpUsername"));
        mailSender.setPassword(cfg.get("smtpPassword"));
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(cfg.getOrDefault("fromAddress", "noreply@tenant.com"));
        helper.setTo(request.getRecipientAddress());
        helper.setSubject(request.getRenderedSubject());
        helper.setText(request.getRenderedBody(), true);

        mailSender.send(message);

        return message.getMessageID() != null ? message.getMessageID() : "SMTP_DELIVERED";
    }

    @Override
    public Channel getSupportedChannel() { return Channel.EMAIL; }
}
