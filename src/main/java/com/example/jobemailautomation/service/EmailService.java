package com.example.jobemailautomation.service;

import com.example.jobemailautomation.config.EmailProperties;
import com.example.jobemailautomation.dto.BulkEmailResponse;
import com.example.jobemailautomation.dto.SendEmailResponse;
import com.example.jobemailautomation.exception.BadRequestException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public SendEmailResponse send(String email) {
        String normalizedEmail = normalize(email);
        if (!isValidEmail(normalizedEmail)) {
            throw new BadRequestException("Invalid email address: " + normalizedEmail);
        }

        sendOne(normalizedEmail);
        return new SendEmailResponse(normalizedEmail, "SENT", "Email sent successfully");
    }

    public BulkEmailResponse sendBulk(MultipartFile file) {
        validateExcelFile(file);

        log.info("Bulk processing started: file={}", safeFilename(file.getOriginalFilename()));

        List<String> rawEmails = readEmails(file);
        int total = rawEmails.size();

        Set<String> uniqueEmails = new LinkedHashSet<>();
        List<String> invalidEmails = new ArrayList<>();

        for (String rawEmail : rawEmails) {
            String email = normalize(rawEmail);
            if (email.isBlank()) {
                continue;
            }

            if (isValidEmail(email)) {
                uniqueEmails.add(email);
            } else {
                invalidEmails.add(email);
            }
        }

        int valid = uniqueEmails.size();
        int sent = 0;
        List<String> failedEmails = new ArrayList<>();

        log.info("Bulk email summary before sending: total={}, valid={}, invalid={}",
                total, valid, invalidEmails.size());

        for (String email : uniqueEmails) {
            try {
                sendOne(email);
                sent++;
                log.info("Email sent successfully: {}", email);
            } catch (Exception ex) {
                failedEmails.add(email);
                log.error("Email sending failed for {}: {}", email, ex.getMessage(), ex);
            }
        }

        int failed = failedEmails.size();

        log.info("Bulk processing completed: total={}, valid={}, invalid={}, sent={}, failed={}",
                total, valid, invalidEmails.size(), sent, failed);

        return new BulkEmailResponse(
                total,
                valid,
                invalidEmails.size(),
                sent,
                failed,
                invalidEmails,
                failedEmails
        );
    }

    private void sendOne(String email) {
        Path attachment = Path.of(emailProperties.attachmentPath());

        if (!Files.exists(attachment) || !Files.isRegularFile(attachment)) {
            throw new BadRequestException("Attachment file not found: " + attachment);
        }

        String extension = getExtension(attachment.getFileName().toString());
        if (!Set.of("pdf", "doc", "docx").contains(extension)) {
            throw new BadRequestException("Unsupported attachment type: " + extension);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject(emailProperties.subject());
            helper.setText(emailProperties.body(), false);
            helper.addAttachment(attachment.getFileName().toString(), new FileSystemResource(attachment));

            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Could not build email message", ex);
        }
    }

    private List<String> readEmails(MultipartFile file) {
        List<String> emails = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("Excel file contains no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            boolean headerProcessed = false;

            for (Row row : sheet) {
                if (!headerProcessed) {
                    headerProcessed = true;
                    continue;
                }

                Cell cell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    continue;
                }

                String value = getCellValue(cell).trim();
                if (!value.isBlank()) {
                    emails.add(value);
                }
            }

            return emails;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to read XLSX file: {}", ex.getMessage(), ex);
            throw new BadRequestException("Unable to read XLSX file");
        }
    }

    private String getCellValue(Cell cell) {
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("XLSX file is required");
        }

        String filename = safeFilename(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new BadRequestException("Only .xlsx files are supported");
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim();
    }

    private boolean isValidEmail(String email) {
        return !email.isBlank() && EMAIL_PATTERN.matcher(email).matches();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String safeFilename(String filename) {
        return filename == null ? "" : filename.replaceAll("[\\r\\n]", "_");
    }
}
