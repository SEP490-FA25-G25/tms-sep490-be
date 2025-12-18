package org.fyp.tmssep490be.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;
import java.util.concurrent.Executor;

@Configuration
@Slf4j
public class EmailConfig implements AsyncConfigurer {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:t03612977@gmail.com}")
    private String mailUsername;

    @Value("${spring.mail.password:hejz yray vzsj anli}")
    private String mailPassword;

    @Value("${tms.email.from.email:t03612977@gmail.com}")
    private String fromEmail;

    @Getter
    @Value("${tms.email.from.name:Hệ thống TMS}")
    private String fromName;

    @Value("${email.async.pool-size:10}")
    private int asyncPoolSize;

    @Value("${email.async.queue-capacity:100}")
    private int asyncQueueCapacity;

    @Value("${email.async.thread-name-prefix:EmailAsync-}")
    private String asyncThreadNamePrefix;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;


    @Bean
    public JavaMailSender javaMailSender() {
        if (!emailEnabled) {
            log.info("Email service is disabled via configuration.");
            return null;
        }

        log.info("Configuring Gmail SMTP with username: {}", mailUsername);

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        log.info("Gmail SMTP configured for username: {}", mailUsername);
        return mailSender;
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

    public String getFromEmail() {
        return fromEmail.isEmpty() ? mailUsername : fromEmail;
    }

}