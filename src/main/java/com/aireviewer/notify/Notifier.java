package com.aireviewer.notify;

/**
 * Simple abstraction for sending admin notifications when the review pipeline fails.
 */
public interface Notifier {
    /**
     * Sends a notification to the administrator.
     *
     * @param subject short subject line
     * @param body full message body
     */
    void notifyAdmin(String subject, String body);
}
