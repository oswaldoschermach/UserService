package com.nebula.userService.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendEmail - sucesso")
    void sendEmail_Success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail("dest@email.com", "Assunto", "Corpo do email");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail - to vazio lanca IllegalArgumentException")
    void sendEmail_EmptyTo_ThrowsException() {
        assertThatThrownBy(() -> emailService.sendEmail("", "Assunto", "Corpo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destinatário");
    }

    @Test
    @DisplayName("sendEmail - to nulo lanca IllegalArgumentException")
    void sendEmail_NullTo_ThrowsException() {
        assertThatThrownBy(() -> emailService.sendEmail(null, "Assunto", "Corpo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sendEmail - assunto vazio lanca IllegalArgumentException")
    void sendEmail_EmptySubject_ThrowsException() {
        assertThatThrownBy(() -> emailService.sendEmail("dest@email.com", "", "Corpo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assunto");
    }

    @Test
    @DisplayName("sendEmail - corpo vazio lanca IllegalArgumentException")
    void sendEmail_EmptyBody_ThrowsException() {
        assertThatThrownBy(() -> emailService.sendEmail("dest@email.com", "Assunto", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Corpo");
    }

    @Test
    @DisplayName("sendEmail - email invalido lanca IllegalArgumentException")
    void sendEmail_InvalidEmail_ThrowsException() {
        assertThatThrownBy(() -> emailService.sendEmail("email-invalido", "Assunto", "Corpo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("sendEmail - falha no JavaMailSender lanca EmailServiceException")
    void sendEmail_MailSenderFails_ThrowsEmailServiceException() {
        doThrow(new MailSendException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("dest@email.com", "Assunto", "Corpo"))
                .isInstanceOf(EmailService.EmailServiceException.class)
                .hasMessageContaining("Falha ao enviar e-mail");
    }
}