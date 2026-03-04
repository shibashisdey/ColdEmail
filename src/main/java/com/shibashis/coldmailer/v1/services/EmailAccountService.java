package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.emailaccount.EmailAccountRequest;
import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.repositories.EmailAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmailAccountService {

    private final EmailAccountRepository emailAccountRepository;
    private final CurrentUserService currentUserService;

    public EmailAccountService(EmailAccountRepository emailAccountRepository,
                               CurrentUserService currentUserService) {
        this.emailAccountRepository = emailAccountRepository;
        this.currentUserService = currentUserService;
    }

    public List<EmailAccount> listMyAccounts() {
        User user = currentUserService.getCurrentUser();
        return emailAccountRepository.findByTenantId(user.getTenantId());
    }

    @Transactional
    public EmailAccount create(EmailAccountRequest request) {
        User user = currentUserService.getCurrentUser();

        if (request.isDefault()) {
            unsetExistingDefault(user);
        }

        EmailAccount account = new EmailAccount();
        account.setUser(user);
        account.setLabel(request.getLabel());
        account.setSmtpHost(request.getSmtpHost());
        account.setSmtpPort(request.getSmtpPort());
        account.setSmtpUsername(request.getSmtpUsername());
        account.setSmtpPassword(request.getSmtpPassword());
        account.setFromEmail(request.getFromEmail());
        account.setUseTls(request.isUseTls());
        account.setDefault(request.isDefault());

        EmailAccount saved = emailAccountRepository.save(account);

        if (emailAccountRepository.findByTenantIdAndIsDefaultTrue(user.getTenantId()).isEmpty()) {
            saved.setDefault(true);
            saved = emailAccountRepository.save(saved);
        }

        return saved;
    }

    private void unsetExistingDefault(User user) {
        emailAccountRepository.findByTenantIdAndIsDefaultTrue(user.getTenantId()).ifPresent(existing -> {
            existing.setDefault(false);
            emailAccountRepository.save(existing);
        });
    }
}
