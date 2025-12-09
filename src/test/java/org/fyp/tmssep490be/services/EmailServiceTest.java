package org.fyp.tmssep490be.services;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.fyp.tmssep490be.config.EmailConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private EmailConfig emailConfig;

    @Spy
    @InjectMocks
    private EmailService emailService;

    // ==========================================================
    // A. sendEmailAsync
    // ==========================================================

    @Test
    void sendEmailAsync_success() {
        MimeMessage mime = new MimeMessage((Session) null);

        lenient().when(mailSender.createMimeMessage()).thenReturn(mime);
        lenient().when(emailConfig.getFromName()).thenReturn("TMS System");
        lenient().when(emailConfig.getFromEmail()).thenReturn("noreply@tms.com");

        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendEmailAsync("user@x.com", "Hello", "<h1>Test</h1>")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAsync_mailSenderIsNull_skipSending() {
        EmailService svc = new EmailService(); // mailSender = null
        assertDoesNotThrow(() ->
                svc.sendEmailAsync("user@x.com", "Hello", "HTML")
        );
    }

    @Test
    void sendEmailAsync_mailSenderThrowsException_butIsCaught() {
        MimeMessage mime = new MimeMessage((Session) null);

        lenient().when(mailSender.createMimeMessage()).thenReturn(mime);
        lenient().when(emailConfig.getFromName()).thenReturn("TMS");
        lenient().when(emailConfig.getFromEmail()).thenReturn("no@tms.com");

        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendEmailAsync("user@x.com", "Hello", "HTML")
        );
    }

    @Test
    void sendEmailAsync_invalidEmail_stillHandled() {
        // Setup minimal stub — không tạo stubbing dư thừa
        MimeMessage mime = new MimeMessage((Session) null);

        // chỉ stub createMimeMessage vì luôn được gọi
        when(mailSender.createMimeMessage()).thenReturn(mime);

        // KHÔNG stub mailSender.send(...) để tránh unnecessary stubbing
        // Vì với email rỗng, code không bao giờ gọi đến send()

        assertDoesNotThrow(() ->
                emailService.sendEmailAsync("", "Hello", "HTML")
        );
    }


    // ==========================================================
    // B. sendEmailWithTemplateAsync
    // ==========================================================

    @Test
    void sendEmailWithTemplateAsync_success() {
        when(templateEngine.process(eq("emails/template"), any(Context.class)))
                .thenReturn("<html>OK</html>");

        doNothing().when(emailService).sendEmailAsync(anyString(), anyString(), anyString());

        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        assertDoesNotThrow(() ->
                emailService.sendEmailWithTemplateAsync("user@x.com", "Subject", "emails/template", data)
        );

        verify(templateEngine, times(1))
                .process(eq("emails/template"), any(Context.class));

        verify(emailService, times(1))
                .sendEmailAsync(eq("user@x.com"), eq("Subject"), anyString());
    }

    @Test
    void sendEmailWithTemplateAsync_templateError_isCaught() {
        when(templateEngine.process(eq("emails/template"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        assertDoesNotThrow(() ->
                emailService.sendEmailWithTemplateAsync("user@x.com", "Subject", "emails/template", data)
        );

        verify(emailService, never())
                .sendEmailAsync(anyString(), anyString(), anyString());
    }

    // ==========================================================
    // C. sendNewStudentCredentialsAsync
    // ==========================================================

    @Test
    void sendNewStudentCredentialsAsync_success() {
        when(templateEngine.process(eq("emails/new-student-credentials"), any(Context.class)))
                .thenReturn("<html>Student</html>");

        doNothing().when(emailService).sendEmailAsync(anyString(), anyString(), anyString());

        assertDoesNotThrow(() ->
                emailService.sendNewStudentCredentialsAsync(
                        "student@x.com",
                        "John Doe",
                        "ST001",
                        "student@x.com",
                        "123456",
                        "Hanoi"
                )
        );

        verify(templateEngine, times(1))
                .process(eq("emails/new-student-credentials"), any(Context.class));

        verify(emailService, times(1))
                .sendEmailAsync(eq("student@x.com"), anyString(), anyString());
    }

    @Test
    void sendNewStudentCredentialsAsync_templateError_isCaught() {
        when(templateEngine.process(eq("emails/new-student-credentials"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        assertDoesNotThrow(() ->
                emailService.sendNewStudentCredentialsAsync(
                        "student@x.com", "John Doe", "ST001",
                        "student@x.com", "123456", "Hanoi"
                )
        );

        verify(emailService, never())
                .sendEmailAsync(anyString(), anyString(), anyString());
    }

    // ==========================================================
    // D. sendClassEnrollmentNotificationAsync
    // ==========================================================

    @Test
    void sendClassEnrollmentNotificationAsync_success() {
        when(templateEngine.process(eq("emails/class-enrollment"), any(Context.class)))
                .thenReturn("<html>Enroll</html>");

        doNothing().when(emailService).sendEmailAsync(anyString(), anyString(), anyString());

        assertDoesNotThrow(() ->
                emailService.sendClassEnrollmentNotificationAsync(
                        "student@x.com", "John Doe", "Math A", "Hanoi Center"
                )
        );

        verify(templateEngine, times(1))
                .process(eq("emails/class-enrollment"), any(Context.class));

        verify(emailService, times(1))
                .sendEmailAsync(eq("student@x.com"), anyString(), anyString());
    }

    @Test
    void sendClassEnrollmentNotificationAsync_templateError_isCaught() {
        when(templateEngine.process(eq("emails/class-enrollment"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        assertDoesNotThrow(() ->
                emailService.sendClassEnrollmentNotificationAsync(
                        "student@x.com", "John Doe", "Math A", "Hanoi Center"
                )
        );

        verify(emailService, never())
                .sendEmailAsync(anyString(), anyString(), anyString());
    }
}
