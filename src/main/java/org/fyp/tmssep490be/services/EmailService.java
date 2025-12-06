package org.fyp.tmssep490be.services;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    CompletableFuture<Void> sendEmailAsync(String to, String subject, String htmlContent);

    CompletableFuture<Void> sendEmailWithTemplateAsync(String to, String subject, String templateName, Map<String, Object> templateData);

    CompletableFuture<Void> sendNewStudentCredentialsAsync(
            String to,
            String studentName,
            String studentCode,
            String email,
            String defaultPassword,
            String branchName
    );

}

