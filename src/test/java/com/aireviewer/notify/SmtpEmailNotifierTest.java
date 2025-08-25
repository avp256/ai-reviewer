package com.aireviewer.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpEmailNotifierTest {

    private JavaMailSender mailSender;
    private SmtpEmailNotifier notifier;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        notifier = new SmtpEmailNotifier(mailSender);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void notifyAdmin_SendsEmailWithSubjectBodyAndTimestamp() {
        // Arrange
        setField(notifier, "adminEmail", "admin@example.com");
        setField(notifier, "fromEmail", "noreply@example.com");

        String subject = "Pipeline Failure";
        String body = "ReviewProcessor failed for MR !123";

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        notifier.notifyAdmin(subject, body);

        // Assert
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertArrayEquals(new String[]{"admin@example.com"}, msg.getTo());
        assertEquals("noreply@example.com", msg.getFrom());
        assertEquals(subject, msg.getSubject());
        assertNotNull(msg.getText());
        assertTrue(msg.getText().startsWith(body));
        assertTrue(msg.getText().contains("Timestamp: "), "Body should contain appended timestamp");
    }

    @Test
    void notifyAdmin_SkipsWhenAdminEmailMissing() {
        // Arrange: adminEmail blank
        setField(notifier, "adminEmail", " ");
        setField(notifier, "fromEmail", "noreply@example.com");

        // Act
        notifier.notifyAdmin("Subj", "Body");

        // Assert: should not call send at all
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyAdmin_DoesNotThrowOnSendFailure() {
        // Arrange
        setField(notifier, "adminEmail", "admin@example.com");
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert: no exception should bubble up
        assertDoesNotThrow(() -> notifier.notifyAdmin("Subj", "Body"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
