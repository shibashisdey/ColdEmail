package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.CampaignCreateRequest;
import com.shibashis.coldmailer.v1.dto.CampaignStatsDTO;
import com.shibashis.coldmailer.v1.dto.campaign.CampaignContactView;
import com.shibashis.coldmailer.v1.dto.campaign.CampaignProgressDTO;
import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.Contact;
import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import com.shibashis.coldmailer.v1.repositories.CampaignContactRepository;
import com.shibashis.coldmailer.v1.repositories.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignContactRepository campaignContactRepository;
    private final CampaignAccessService campaignAccessService;
    private final CampaignContactImportService campaignContactImportService;
    private final CampaignDispatchService campaignDispatchService;
    private final CampaignProgressService campaignProgressService;
    private final CampaignResumeService campaignResumeService;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignContactRepository campaignContactRepository,
                           CampaignAccessService campaignAccessService,
                           CampaignContactImportService campaignContactImportService,
                           CampaignDispatchService campaignDispatchService,
                           CampaignProgressService campaignProgressService,
                           CampaignResumeService campaignResumeService) {
        this.campaignRepository = campaignRepository;
        this.campaignContactRepository = campaignContactRepository;
        this.campaignAccessService = campaignAccessService;
        this.campaignContactImportService = campaignContactImportService;
        this.campaignDispatchService = campaignDispatchService;
        this.campaignProgressService = campaignProgressService;
        this.campaignResumeService = campaignResumeService;
    }

    public List<Campaign> listMyCampaigns() {
        return campaignAccessService.listMyCampaigns();
    }

    @Transactional
    public Campaign createCampaign(CampaignCreateRequest request) {
        User user = campaignAccessService.currentUser();
        EmailAccount emailAccount = campaignAccessService.getMyEmailAccount(request.getEmailAccountId());

        Campaign campaign = new Campaign();
        campaign.setUser(user);
        campaign.setEmailAccount(emailAccount);
        campaign.setName(request.getName().trim());
        campaign.setSubject(request.getSubject().trim());
        campaign.setTemplateBody(request.getTemplateBody());
        campaign.setLoadBalancedDispatch(request.isLoadBalancedDispatch());
        campaign.setStatus(CampaignStatus.DRAFT);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public int uploadContactsCsv(Long campaignId, MultipartFile file) {
        Campaign campaign = campaignAccessService.getMyCampaign(campaignId);
        User user = campaignAccessService.currentUser();
        return campaignContactImportService.importCsv(campaign, user, file);
    }

    @Transactional
    public Campaign uploadResume(Long campaignId, MultipartFile file) {
        Campaign campaign = campaignAccessService.getMyCampaign(campaignId);
        campaignResumeService.attachResume(campaign, file);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign startCampaign(Long campaignId) {
        Campaign campaign = campaignAccessService.getMyCampaign(campaignId);
        long enqueued = campaignDispatchService.enqueueCampaign(campaign);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setProcessingDetails("Queued " + enqueued + " email jobs.");
        return campaignRepository.save(campaign);
    }

    public CampaignStatsDTO getStats(Long campaignId) {
        Campaign campaign = campaignAccessService.getMyCampaign(campaignId);
        return campaignProgressService.getStats(campaign);
    }

    public CampaignProgressDTO getProgress(Long campaignId) {
        Campaign campaign = campaignAccessService.getMyCampaign(campaignId);
        return campaignProgressService.getProgress(campaign);
    }

    public List<CampaignContactView> listCampaignContacts(Long campaignId) {
        Campaign campaign = campaignAccessService.getMyCampaign(campaignId);
        List<CampaignContact> contacts = campaignContactRepository.findByCampaignIdOrderByIdAsc(campaign.getId());
        List<CampaignContactView> views = new ArrayList<>();

        for (CampaignContact cc : contacts) {
            Contact c = cc.getContact();
            views.add(new CampaignContactView(
                    cc.getId(),
                    c.getEmail(),
                    c.getFirstName(),
                    c.getLastName(),
                    c.getCompany(),
                    cc.getStatus(),
                    cc.getTrackingId(),
                    cc.getSentAt(),
                    cc.getOpenedAt(),
                    cc.getResumeDownloadedAt(),
                    cc.getFailureReason(),
                    cc.getRetryCount()
            ));
        }
        return views;
    }
}
