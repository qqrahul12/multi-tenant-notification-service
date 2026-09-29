package com.notificationservice.channel;

import com.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Template Method Pattern — defines the algorithm skeleton for channel dispatch.
 *
 * Invariant steps (in base):  preDispatch → [subclass doSend] → postDispatch / onFailure
 * Variant step (in subclass): doSend(request)
 *
 * Subclasses implement only the provider-specific send logic.
 * Logging, error normalisation, and provider response handling live here.
 */
@Slf4j
public abstract class AbstractChannelDispatcher implements ChannelDispatcher {

    @Override
    public final void dispatch(NotificationRequest request) throws Exception {
        preDispatch(request);
        try {
            String providerResponse = doSend(request);
            postDispatch(request, providerResponse);
        } catch (TransientChannelException e) {
            log.warn("[{}] Transient failure for notificationId={}: {}",
                    getSupportedChannel(), request.getId(), e.getMessage());
            throw e;  // let RetryAwareChannelDispatcher (Decorator) handle retry scheduling
        } catch (Exception e) {
            log.error("[{}] Unexpected failure for notificationId={}: {}",
                    getSupportedChannel(), request.getId(), e.getMessage(), e);
            throw new TransientChannelException("Unexpected dispatch error", e);
        }
    }

    /**
     * Variant step — subclasses implement actual sending logic.
     * @return provider response string (for audit logging)
     */
    protected abstract String doSend(NotificationRequest request) throws Exception;

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
