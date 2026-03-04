package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.repositories.EmailAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailAccountRoutingService {

    private final EmailAccountRepository emailAccountRepository;
    private final ConcurrentHashMap<UUID, AtomicInteger> tenantRoundRobin = new ConcurrentHashMap<>();

    public EmailAccountRoutingService(EmailAccountRepository emailAccountRepository) {
        this.emailAccountRepository = emailAccountRepository;
    }

    public EmailAccount resolveAccount(Campaign campaign) {
        if (!campaign.isLoadBalancedDispatch()) {
            if (!campaign.getEmailAccount().isActive()) {
                List<EmailAccount> activeAccounts = emailAccountRepository.findByTenantIdAndActiveTrue(campaign.getTenantId());
                if (!activeAccounts.isEmpty()) {
                    return activeAccounts.get(0);
                }
            }
            return campaign.getEmailAccount();
        }

        List<EmailAccount> accounts = emailAccountRepository.findByTenantIdAndActiveTrue(campaign.getTenantId());
        if (accounts.isEmpty()) {
            return campaign.getEmailAccount();
        }

        AtomicInteger counter = tenantRoundRobin.computeIfAbsent(campaign.getTenantId(), ignored -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), accounts.size());
        return accounts.get(index);
    }
}
