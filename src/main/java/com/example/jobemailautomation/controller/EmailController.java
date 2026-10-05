package com.example.jobemailautomation.controller;

import com.example.jobemailautomation.dto.BulkEmailResponse;
import com.example.jobemailautomation.dto.SendEmailRequest;
import com.example.jobemailautomation.dto.SendEmailResponse;
import com.example.jobemailautomation.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/email")
@Tag(name = "Email", description = "Job application email APIs")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send an application email to one recruiter")
    @ApiResponse(responseCode = "200", description = "Email sent successfully")
    public ResponseEntity<SendEmailResponse> send(@Valid @org.springframework.web.bind.annotation.RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(emailService.send(request.email()));
    }

    @PostMapping(value = "/send-bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send application emails to recruiters from an XLSX file")
    @ApiResponse(responseCode = "200", description = "Bulk processing completed",
            content = @Content(schema = @Schema(implementation = BulkEmailResponse.class)))
    public ResponseEntity<BulkEmailResponse> sendBulk(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(emailService.sendBulk(file));
    }
}
