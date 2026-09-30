package com.notificationservice.channel;

import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.domain.NotificationRequest;
import com.notificationservice.repository.NotificationChannelConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractChannelDispatcher implements ChannelDispatcher {

    @Autowired
    private NotificationChannelConfigRepository channelConfigRepository;

    @Override
    public final void dispatch(NotificationRequest request) throws Exception {
        preDispatch(request);
        
        NotificationChannelConfig config = channelConfigRepository
                .findByTenantIdAndChannel(request.getTenantId(), getSupportedChannel())
                .filter(NotificationChannelConfig::isEnabled)
                .orElseThrow(() -> new IllegalStateException(
                        "No active channel configuration found for tenant=" + request.getTenantId() 
                        + " channel=" + getSupportedChannel()));

        try {
            String providerResponse = doSend(request, config);
            postDispatch(request, providerResponse);
            
        } catch (org.springframework.web.client.RestClientException e) {
            log.warn("[{}] Network/Transient failure for notificationId={}: {}",
                    getSupportedChannel(), request.getId(), e.getMessage());
            throw new TransientChannelException("Provider network failure: " + e.getMessage(), e);
        } catch (TransientChannelException e) {
            log.warn("[{}] Transient failure for notificationId={}: {}",
                    getSupportedChannel(), request.getId(), e.getMessage());
            throw e; 
        } catch (Exception e) {
            log.error("[{}] Unexpected failure for notificationId={}: {}",
                    getSupportedChannel(), request.getId(), e.getMessage(), e);
            throw new TransientChannelException("Unexpected dispatch error: " + e.getMessage(), e);
        }
    }

    protected abstract String doSend(NotificationRequest request, NotificationChannelConfig config) throws Exception;

    private void preDispatch(NotificationRequest request) {
        log.info("[{}] Dispatching notificationId={} to recipient={}",
                getSupportedChannel(), request.getId(),
                maskRecipient(request.getRecipientAddress()));
    }

    private void postDispatch(NotificationRequest request, String providerResponse) {
        log.info("[{}] Successfully dispatched notificationId={} providerResponse={}",
                getSupportedChannel(), request.getId(), providerResponse);
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 4) return "***";
        return recipient.substring(0, 3) + "***";
    }
}
