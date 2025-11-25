package org.fyp.tmssep490be.config;

import com.resend.Resend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.concurrent.Executor;

/**
 * Configuration for Email Service with Resend
 * Includes rate limiting (5 emails/second), async processing, and UTF-8 support
 */
@Configuration
@Slf4j
public class EmailConfig implements AsyncConfigurer {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    
    @Value("${email.async.pool-size:10}")
    private int asyncPoolSize;

    @Value("${email.async.queue-capacity:100}")
    private int asyncQueueCapacity;

    @Value("${email.async.thread-name-prefix:EmailAsync-}")
    private String asyncThreadNamePrefix;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    
    @Bean
    public Resend resendClient() {
        if (!emailEnabled) {
            log.info("Email service is disabled via configuration.");
            return null;
        }
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            log.warn("Resend API key not configured. Email service will be disabled.");
            return null;
        }
        return new Resend(resendApiKey);
    }

    
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncPoolSize);
        executor.setMaxPoolSize(asyncPoolSize * 2);
        executor.setQueueCapacity(asyncQueueCapacity);
        executor.setThreadNamePrefix(asyncThreadNamePrefix);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    @Bean(name = "emailTemplateEngine")
    public TemplateEngine emailTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean(name = "emailTemplateResolver")
    public ClassLoaderTemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(true);
        templateResolver.setCheckExistence(true);
        return templateResolver;
    }
}