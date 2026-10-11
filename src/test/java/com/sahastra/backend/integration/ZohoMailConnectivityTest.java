package com.sahastra.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ZohoMailConnectivityTest {

    @Test
    void sendsTestMessageThroughConfiguredSmtpServer() {
        String host = System.getenv("MAIL_HOST");
        String username = System.getenv("MAIL_USERNAME");
        String password = System.getenv("MAIL_PASSWORD");
        String from = System.getenv("MAIL_FROM");
        String recipient = System.getenv("MAIL_TEST_TO");

        assumeTrue(isPresent(host) && isPresent(username) && isPresent(password)
                        && isPresent(from) && isPresent(recipient),
                "Set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM, and MAIL_TEST_TO to run the SMTP test");

        int port = Integer.parseInt(System.getenv().getOrDefault("MAIL_PORT", "587"));
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        mailProperties.put("mail.smtp.starttls.required", "true");
        mailProperties.put("mail.smtp.connectiontimeout", "5000");
        mailProperties.put("mail.smtp.timeout", "5000");
        mailProperties.put("mail.smtp.writetimeout", "5000");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Sahastra Digital SMTP connectivity test");
        message.setText("This is a test message confirming SMTP connectivity from the backend.");

        mailSender.send(message);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
