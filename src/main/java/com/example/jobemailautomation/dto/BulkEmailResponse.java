package com.example.jobemailautomation.dto;

import java.util.List;

public record BulkEmailResponse(
        int total,
        int valid,
        int invalid,
        int sent,
        int failed,
        List<String> invalidEmails,
        List<String> failedEmails
) {
}
