//package com.example.jobemailautomation.controller;
//
//import com.example.jobemailautomation.dto.SendEmailResponse;
//import com.example.jobemailautomation.service.EmailService;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(EmailController.class)
//class EmailControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Mock
//    private EmailService emailService;
//
//    @Test
//    void sendShouldReturnOk() throws Exception {
//        when(emailService.send("recruiter@example.com"))
//                .thenReturn(new SendEmailResponse("recruiter@example.com", "SENT", "Email sent successfully"));
//
//        mockMvc.perform(post("/api/v1/email/send")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                                {"email":"recruiter@example.com"}
//                                """))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value("recruiter@example.com"))
//                .andExpect(jsonPath("$.status").value("SENT"));
//
//        verify(emailService).send("recruiter@example.com");
//    }
//
//    @Test
//    void sendShouldRejectInvalidEmail() throws Exception {
//        mockMvc.perform(post("/api/v1/email/send")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                                {"email":"invalid"}
//                                """))
//                .andExpect(status().isBadRequest());
//
//        verifyNoInteractions(emailService);
//    }
//}
