package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.EmailAccount;
import jakarta.mail.MessagingException;

public interface EmailSenderService {
    void sendHtmlMessage(EmailAccount account, String to, String subject, String htmlBody) throws MessagingException;
}
