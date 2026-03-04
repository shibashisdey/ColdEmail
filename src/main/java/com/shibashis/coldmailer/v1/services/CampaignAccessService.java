package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.repositories.CampaignRepository;
import com.shibashis.coldmailer.v1.repositories.EmailAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CampaignAccessService {

    private final CampaignRepository campaignRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final CurrentUserService currentUserService;

    public CampaignAccessService(CampaignRepository campaignRepository,
                                 EmailAccountRepository emailAccountRepository,
                                 CurrentUserService currentUserService) {
        this.campaignRepository = campaignRepository;
        this.emailAccountRepository = emailAccountRepository;
        this.currentUserService = currentUserService;
    }

    public User currentUser() {
        return currentUserService.getCurrentUser();
    }

    public List<Campaign> listMyCampaigns() {
        User user = currentUser();
        return campaignRepository.findByTenantIdOrderByCreatedAtDesc(user.getTenantId());
    }

    public Campaign getMyCampaign(Long campaignId) {
        User user = currentUser();
        return campaignRepository.findByIdAndTenantId(campaignId, user.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    public EmailAccount getMyEmailAccount(Long emailAccountId) {
        User user = currentUser();
        EmailAccount account = emailAccountRepository.findByIdAndTenantId(emailAccountId, user.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email account"));
        if (!account.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email account is suspended");
        }
        return account;
    }
}
