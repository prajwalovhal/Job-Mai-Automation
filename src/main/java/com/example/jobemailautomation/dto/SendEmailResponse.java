package com.example.jobemailautomation.dto;

public record SendEmailResponse(
        String email,
        String status,
        String message
) {
}
