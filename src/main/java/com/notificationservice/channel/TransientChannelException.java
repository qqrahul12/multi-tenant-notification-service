package com.notificationservice.channel;

/**
 * Transient failure — eligible for retry with exponential backoff.
 * Examples: network timeout, provider rate limit, temporary outage.
 */
public class TransientChannelException extends RuntimeException {
    public TransientChannelException(String message) { super(message); }
    public TransientChannelException(String message, Throwable cause) { super(message, cause); }
}
