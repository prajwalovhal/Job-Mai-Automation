package com.example.jobemailautomation.service;

import com.example.jobemailautomation.config.EmailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.mail.internet.MimeMessage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void sendShouldCreateAndSendOneMessage() throws Exception {
        Path resume = Files.createFile(tempDir.resolve("resume.pdf"));

        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);

        EmailProperties properties = new EmailProperties(
                "Test Subject",
                "Test Body",
                resume.toString()
        );

        EmailService service = new EmailService(sender, properties);

        var response = service.send("  recruiter@example.com  ");

        assertEquals("recruiter@example.com", response.email());
        assertEquals("SENT", response.status());
        verify(sender, times(1)).createMimeMessage();
        verify(sender, times(1)).send(mimeMessage);
    }

    @Test
    void sendShouldRejectInvalidEmail() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailProperties properties = new EmailProperties(
                "Test Subject", "Test Body", tempDir.resolve("resume.pdf").toString()
        );

        EmailService service = new EmailService(sender, properties);

        assertThrows(RuntimeException.class, () -> service.send("not-an-email"));
        verifyNoInteractions(sender);
    }

    @Test
    void bulkShouldTrimValidateDeduplicateAndContinueOnFailure() throws Exception {
        Path resume = Files.createFile(tempDir.resolve("resume.pdf"));

        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage())
                .thenReturn(mock(MimeMessage.class))
                .thenReturn(mock(MimeMessage.class));

        doNothing().when(sender).send(any(MimeMessage.class));

        EmailProperties properties = new EmailProperties(
                "Test Subject", "Test Body", resume.toString()
        );
        EmailService service = new EmailService(sender, properties);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "recruiters.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createXlsx(
                        "email",
                        " recruiter1@example.com ",
                        "recruiter1@example.com",
                        "invalid",
                        "",
                        "recruiter2@example.com"
                )
        );

        var response = service.sendBulk(file);

        assertEquals(4, response.total());
        assertEquals(2, response.valid());
        assertEquals(1, response.invalid());
        assertEquals(2, response.sent());
        assertEquals(0, response.failed());
        assertEquals(1, response.invalidEmails().size());
        verify(sender, times(2)).send(any(MimeMessage.class));
    }

    private byte[] createXlsx(String... values) throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var output = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Recruiters");
            for (int i = 0; i < values.length; i++) {
                sheet.createRow(i).createCell(0).setCellValue(values[i]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
