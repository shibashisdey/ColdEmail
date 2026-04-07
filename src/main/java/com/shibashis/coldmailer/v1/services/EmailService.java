package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.EmailAccount;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService implements EmailSenderService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public EmailService(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public void sendHtmlMessage(EmailAccount account, String to, String subject, String htmlBody) throws MessagingException {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("smtp-account-" + account.getId());
        try {
            breaker.executeRunnable(() -> {
                try {
                    doSend(account, to, subject, htmlBody);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (CallNotPermittedException ex) {
            throw new MessagingException("SMTP circuit breaker is OPEN for account " + account.getId(), ex);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof MessagingException messagingException) {
                throw messagingException;
            }
            throw new MessagingException("SMTP send failed: " + ex.getMessage(), ex);
        }
    }

    private void doSend(EmailAccount account, String to, String subject, String htmlBody) throws MessagingException {
        logger.info("smtp_send_attempt smtpAccountId={} host={} port={} username={} from={} to={} subject={}",
                account.getId(), account.getSmtpHost(), account.getSmtpPort(), account.getSmtpUsername(),
                account.getFromEmail(), to, subject);
        JavaMailSenderImpl sender = buildSender(account);
        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        helper.setFrom(account.getFromEmail());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        sender.send(mimeMessage);
    }

    private JavaMailSenderImpl buildSender(EmailAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getSmtpHost());
        sender.setPort(account.getSmtpPort());
        sender.setUsername(account.getSmtpUsername());
        sender.setPassword(account.getSmtpPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", Boolean.toString(account.isUseTls()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
