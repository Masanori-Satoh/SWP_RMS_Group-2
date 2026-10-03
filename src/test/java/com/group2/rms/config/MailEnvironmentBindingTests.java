package com.group2.rms.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailEnvironmentBindingTests {

    @Test
    void documentedEnvironmentVariableNamesBindToMailProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-systemEnvironment", Map.of(
                "SPRING_MAIL_HOST", "smtp.example.test",
                "SPRING_MAIL_PORT", "587",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH", "true",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE", "true",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED", "true",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT", "10000",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT", "10000",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT", "10000")));

        MailProperties mail = Binder.get(environment).bind("spring.mail", MailProperties.class)
                .orElseThrow(() -> new AssertionError("spring.mail was not bound"));
        assertEquals("smtp.example.test", mail.getHost());
        assertEquals(587, mail.getPort());
        assertEquals("true", mail.getProperties().get("mail.smtp.auth"));
        assertEquals("true", mail.getProperties().get("mail.smtp.starttls.enable"));
        assertEquals("true", mail.getProperties().get("mail.smtp.starttls.required"));
        assertEquals("10000", mail.getProperties().get("mail.smtp.connectiontimeout"));
        assertEquals("10000", mail.getProperties().get("mail.smtp.timeout"));
        assertEquals("10000", mail.getProperties().get("mail.smtp.writetimeout"));
    }

    @Test
    void springBootCreatesMailSenderWhenHostIsConfiguredWithoutSendingEmail() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withPropertyValues("spring.mail.host=smtp.example.test", "spring.mail.port=587")
                .run(context -> {
                    assertTrue(context.containsBean("mailSender"));
                    JavaMailSender sender = context.getBean(JavaMailSender.class);
                    JavaMailSenderImpl implementation = assertInstanceOf(JavaMailSenderImpl.class, sender);
                    assertEquals("smtp.example.test", implementation.getHost());
                    assertEquals(587, implementation.getPort());
                });
    }
}
