package org.fyp.tmssep490be.services;

import jakarta.mail.internet.MimeMessage;
import org.fyp.tmssep490be.config.EmailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailConfig emailConfig;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(emailConfig.getFromEmail()).thenReturn("no-reply@fpt.edu.vn");
        when(emailConfig.getFromName()).thenReturn("FPT Education");
    }

    // ================================
    // TEST sendEmailAsync()
    // ================================
    @Test
    void sendEmailAsync_success() throws Exception {
        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        emailService.sendEmailAsync(
                "user@x.com",
                "Subject",
                "<h1>Hello</h1>"
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAsync_mailSenderNull_skipSending() {
        // simulate null mail sender
        emailService = new EmailService();
        emailService.sendEmailAsync("x@gmail.com", "Subject", "Body");

        // no exception thrown
        assertTrue(true);
    }

    // ================================
    // TEST sendEmailWithTemplateAsync()
    // ================================
    @Test
    void sendEmailWithTemplateAsync_success() {
        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        when(templateEngine.process(eq("emails/class-enrollment"), any(Context.class)))
                .thenReturn("<html>OK</html>");

        Map<String, Object> data = new HashMap<>();
        data.put("studentName", "Nghia");

        emailService.sendEmailWithTemplateAsync(
                "student@fpt.edu.vn",
                "Subject",
                "emails/class-enrollment",
                data
        );

        verify(templateEngine, times(1)).process(eq("emails/class-enrollment"), any(Context.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailWithTemplateAsync_templateError() {
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("template error"));

        Map<String, Object> data = new HashMap<>();

        assertDoesNotThrow(() ->
                emailService.sendEmailWithTemplateAsync("a@a.com", "Subject", "wrong-template", data)
        );
    }

    // ================================
    // TEST sendNewStudentCredentialsAsync()
    // ================================
    @Test
    void sendNewStudentCredentialsAsync_success() {
        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        when(templateEngine.process(eq("emails/new-student-credentials"), any(Context.class)))
                .thenReturn("<html>OK</html>");

        emailService.sendNewStudentCredentialsAsync(
                "student@fpt.edu.vn",
                "Student Name",
                "ST123",
                "email@fpt.edu.vn",
                "123456",
                "Hanoi Branch"
        );

        verify(templateEngine, times(1)).process(eq("emails/new-student-credentials"), any(Context.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ================================
    // TEST sendClassEnrollmentNotificationAsync()
    // ================================
    @Test
    void sendClassEnrollmentNotificationAsync_success() {
        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        when(templateEngine.process(eq("emails/class-enrollment"), any(Context.class)))
                .thenReturn("<html>OK</html>");

        emailService.sendClassEnrollmentNotificationAsync(
                "student@fpt.edu.vn",
                "John",
                "Java Course",
                "Hanoi Center"
        );

        verify(templateEngine, times(1))
                .process(eq("emails/class-enrollment"), any(Context.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
