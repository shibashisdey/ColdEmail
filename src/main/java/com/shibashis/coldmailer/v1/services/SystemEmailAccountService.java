package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.repositories.EmailAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemEmailAccountService {

    private final EmailAccountRepository emailAccountRepository;

    @Value("${app.sender.label:System Sender}")
    private String senderLabel;

    @Value("${app.sender.smtp-host}")
    private String smtpHost;

    @Value("${app.sender.smtp-port}")
    private Integer smtpPort;

    @Value("${app.sender.smtp-username}")
    private String smtpUsername;

    @Value("${app.sender.smtp-password}")
    private String smtpPassword;

    @Value("${app.sender.from-email}")
    private String fromEmail;

    @Value("${app.sender.use-tls:true}")
    private boolean useTls;

    public SystemEmailAccountService(EmailAccountRepository emailAccountRepository) {
        this.emailAccountRepository = emailAccountRepository;
    }

    @Transactional
    public EmailAccount ensureSystemAccount(User user) {
        return emailAccountRepository.findByTenantIdAndIsDefaultTrue(user.getTenantId())
                .map(this::refreshSystemAccount)
                .orElseGet(() -> createSystemAccount(user));
    }

    private EmailAccount createSystemAccount(User user) {
        EmailAccount account = new EmailAccount();
        account.setUser(user);
        applyConfiguredSender(account);
        account.setDefault(true);
        account.setActive(true);
        return emailAccountRepository.save(account);
    }

    private EmailAccount refreshSystemAccount(EmailAccount account) {
        applyConfiguredSender(account);
        account.setDefault(true);
        account.setActive(true);
        return emailAccountRepository.save(account);
    }

    private void applyConfiguredSender(EmailAccount account) {
        account.setLabel(senderLabel);
        account.setSmtpHost(smtpHost);
        account.setSmtpPort(smtpPort);
        account.setSmtpUsername(smtpUsername);
        account.setSmtpPassword(smtpPassword);
        account.setFromEmail(fromEmail);
        account.setUseTls(useTls);
    }
}
