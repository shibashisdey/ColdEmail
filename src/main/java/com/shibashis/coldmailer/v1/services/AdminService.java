package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.admin.*;
import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.Contact;
import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import com.shibashis.coldmailer.v1.queue.EmailJobQueueService;
import com.shibashis.coldmailer.v1.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final ContactRepository contactRepository;
    private final CampaignContactRepository campaignContactRepository;
    private final UserRepository userRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final CampaignRepository campaignRepository;
    private final EmailJobQueueService emailJobQueueService;
    private final WorkerMetricsService workerMetricsService;
    private final PlatformPolicyService platformPolicyService;

    public AdminService(ContactRepository contactRepository,
                        CampaignContactRepository campaignContactRepository,
                        UserRepository userRepository,
                        EmailAccountRepository emailAccountRepository,
                        CampaignRepository campaignRepository,
                        EmailJobQueueService emailJobQueueService,
                        WorkerMetricsService workerMetricsService,
                        PlatformPolicyService platformPolicyService) {
        this.contactRepository = contactRepository;
        this.campaignContactRepository = campaignContactRepository;
        this.userRepository = userRepository;
        this.emailAccountRepository = emailAccountRepository;
        this.campaignRepository = campaignRepository;
        this.emailJobQueueService = emailJobQueueService;
        this.workerMetricsService = workerMetricsService;
        this.platformPolicyService = platformPolicyService;
    }

    @Transactional(readOnly = true)
    public List<AdminHrContactView> listHrContacts(String query) {
        List<Contact> contacts;
        if (query == null || query.trim().isEmpty()) {
            contacts = contactRepository.findAllByOrderByCreatedAtDesc();
        } else {
            String q = query.trim();
            contacts = contactRepository.findByEmailContainingIgnoreCaseOrCompanyContainingIgnoreCaseOrderByCreatedAtDesc(q, q);
        }

        List<AdminHrContactView> views = new ArrayList<>();
        for (Contact contact : contacts) {
            views.add(new AdminHrContactView(
                    contact.getId(),
                    contact.getTenantId(),
                    contact.getEmail(),
                    contact.getFirstName(),
                    contact.getLastName(),
                    contact.getCompany(),
                    contact.getCreatedAt()
            ));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<AdminFailedEmailView> listFailedEmails() {
        List<CampaignContact> failed = campaignContactRepository.findByStatusOrderByIdDesc(CampaignContactStatus.FAILED);
        List<AdminFailedEmailView> views = new ArrayList<>();
        for (CampaignContact cc : failed) {
            views.add(new AdminFailedEmailView(
                    cc.getId(),
                    cc.getCampaign().getId(),
                    cc.getCampaign().getName(),
                    cc.getCampaign().getTenantId(),
                    cc.getContact().getEmail(),
                    cc.getFailureReason(),
                    cc.getRetryCount(),
                    cc.getSentAt()
            ));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        List<AdminUserView> views = new ArrayList<>();
        for (User user : users) {
            views.add(new AdminUserView(
                    user.getId(),
                    user.getTenantId(),
                    user.getName(),
                    user.getEmail(),
                    user.isActive(),
                    user.getSuspensionReason(),
                    user.getSuspendedAt(),
                    user.getCreatedAt()
            ));
        }
        return views;
    }

    @Transactional
    public AdminUserView updateUserStatus(Long userId, AdminUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setActive(request.getActive());
        if (request.getActive()) {
            user.setSuspensionReason(null);
            user.setSuspendedAt(null);
        } else {
            user.setSuspensionReason(blankToNull(request.getReason()));
            user.setSuspendedAt(LocalDateTime.now());
        }

        User saved = userRepository.save(user);
        return new AdminUserView(
                saved.getId(),
                saved.getTenantId(),
                saved.getName(),
                saved.getEmail(),
                saved.isActive(),
                saved.getSuspensionReason(),
                saved.getSuspendedAt(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public AdminUserView removeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setActive(false);
        user.setSuspensionReason("Removed by admin");
        user.setSuspendedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        return new AdminUserView(
                saved.getId(),
                saved.getTenantId(),
                saved.getName(),
                saved.getEmail(),
                saved.isActive(),
                saved.getSuspensionReason(),
                saved.getSuspendedAt(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminEmailAccountView> listEmailAccounts() {
        List<EmailAccount> accounts = emailAccountRepository.findAllByOrderByCreatedAtDesc();
        List<AdminEmailAccountView> views = new ArrayList<>();
        for (EmailAccount account : accounts) {
            views.add(new AdminEmailAccountView(
                    account.getId(),
                    account.getUser().getId(),
                    account.getTenantId(),
                    account.getLabel(),
                    account.getSmtpHost(),
                    account.getSmtpPort(),
                    account.getSmtpUsername(),
                    account.getFromEmail(),
                    account.isActive(),
                    account.getSuspensionReason(),
                    account.getSuspendedAt(),
                    account.getCreatedAt()
            ));
        }
        return views;
    }

    @Transactional
    public AdminEmailAccountView updateEmailAccountStatus(Long emailAccountId, AdminEmailAccountStatusRequest request) {
        EmailAccount account = emailAccountRepository.findById(emailAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email account not found"));

        account.setActive(request.getActive());
        if (request.getActive()) {
            account.setSuspensionReason(null);
            account.setSuspendedAt(null);
        } else {
            account.setSuspensionReason(blankToNull(request.getReason()));
            account.setSuspendedAt(LocalDateTime.now());
        }

        EmailAccount saved = emailAccountRepository.save(account);
        return new AdminEmailAccountView(
                saved.getId(),
                saved.getUser().getId(),
                saved.getTenantId(),
                saved.getLabel(),
                saved.getSmtpHost(),
                saved.getSmtpPort(),
                saved.getSmtpUsername(),
                saved.getFromEmail(),
                saved.isActive(),
                saved.getSuspensionReason(),
                saved.getSuspendedAt(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AdminPlatformStatsView platformStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long totalCampaigns = campaignRepository.count();
        long totalContacts = contactRepository.count();

        long sent = campaignContactRepository.countByStatusIn(CampaignContactStatus.sentLikeStatuses());
        long opened = campaignContactRepository.countByOpenedAtIsNotNull();
        long resume = campaignContactRepository.countByResumeDownloadedAtIsNotNull();
        long failed = campaignContactRepository.countByStatus(CampaignContactStatus.FAILED);

        double openRate = pct(opened, sent);
        double clickRate = pct(resume, sent);
        double failRate = pct(failed, sent + failed);

        return new AdminPlatformStatsView(
                totalUsers,
                activeUsers,
                totalCampaigns,
                totalContacts,
                sent,
                opened,
                resume,
                failed,
                openRate,
                clickRate,
                failRate,
                emailJobQueueService.size()
        );
    }

    @Transactional(readOnly = true)
    public AdminWorkerHealthView workerHealth() {
        boolean redisReachable;
        try {
            emailJobQueueService.touchConnection();
            redisReachable = true;
        } catch (Exception ex) {
            redisReachable = false;
        }

        return new AdminWorkerHealthView(
                redisReachable,
                emailJobQueueService.size(),
                workerMetricsService.getProcessed(),
                workerMetricsService.getSent(),
                workerMetricsService.getFailed(),
                workerMetricsService.getLastProcessedAt()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, String> listSettings() {
        return platformPolicyService.allSettings();
    }

    @Transactional
    public Map<String, String> upsertSetting(AdminSettingUpdateRequest request) {
        platformPolicyService.upsert(request.getKey().trim(), request.getValue().trim());
        return platformPolicyService.allSettings();
    }

    private double pct(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (numerator * 100.0) / denominator;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
