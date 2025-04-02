package com.VMTecnologia.userService.service;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final String defaultFromEmail = "oswaldo.schermach@gmail.com";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia um e-mail simples.
     *
     * @param to      Endereço de e-mail do destinatário (não pode ser vazio/nulo)
     * @param subject Assunto do e-mail (não pode ser vazio/nulo)
     * @param body    Corpo do e-mail (não pode ser vazio/nulo)
     * @throws IllegalArgumentException Se algum parâmetro for inválido
     * @throws EmailServiceException     Se ocorrer um erro ao enviar o e-mail
     */
    public void sendEmail(String to, String subject, String body) {
        validateEmailParameters(to, subject, body);

        try {
            SimpleMailMessage message = createEmailMessage(to, subject, body);
            mailSender.send(message);
            logger.info("E-mail enviado com sucesso para: {}", to);
        } catch (MailException ex) {
            logger.error("Falha ao enviar e-mail para: {}", to, ex);
            throw new EmailServiceException("Falha ao enviar e-mail", ex);
        }
    }

    /**
     * Cria a mensagem de e-mail com os parâmetros fornecidos.
     */
    private SimpleMailMessage createEmailMessage(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(defaultFromEmail);
        return message;
    }

    /**
     * Valida os parâmetros do e-mail.
     */
    private void validateEmailParameters(String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            throw new IllegalArgumentException("Endereço de e-mail do destinatário não pode ser vazio");
        }

        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Assunto do e-mail não pode ser vazio");
        }

        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("Corpo do e-mail não pode ser vazio");
        }

        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Endereço de e-mail do destinatário é inválido");
        }
    }

    /**
     * Validação simples de formato de e-mail.
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    /**
     * Exceção customizada para erros no serviço de e-mail.
     */
    public static class EmailServiceException extends RuntimeException {
        public EmailServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}