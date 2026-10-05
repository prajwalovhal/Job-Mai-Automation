package com.example.jobemailautomation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        String subject,
        String body,
        String attachmentPath
) {
}
