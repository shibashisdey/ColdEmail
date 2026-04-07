package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.dto.CampaignCreateRequest;
import com.shibashis.coldmailer.v1.dto.CampaignStatsDTO;
import com.shibashis.coldmailer.v1.dto.campaign.CampaignContactView;
import com.shibashis.coldmailer.v1.dto.campaign.ManualCampaignContactRequest;
import com.shibashis.coldmailer.v1.dto.campaign.CampaignProgressDTO;
import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.services.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<Campaign> createCampaign(@Valid @RequestBody CampaignCreateRequest request) {
        Campaign campaign = campaignService.createCampaign(request);
        return new ResponseEntity<>(campaign, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Campaign> listCampaigns() {
        return campaignService.listMyCampaigns();
    }

    @PostMapping(path = "/{id}/contacts/upload-csv", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadContactsCsv(@PathVariable Long id,
                                                                 @RequestParam("file") MultipartFile file) {
        int created = campaignService.uploadContactsCsv(id, file);
        return ResponseEntity.ok(Map.of("createdCampaignContacts", created));
    }

    @PostMapping(path = "/{id}/resume/upload", consumes = "multipart/form-data")
    public ResponseEntity<Campaign> uploadResume(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file) {
        Campaign updated = campaignService.uploadResume(id, file);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/contacts")
    public ResponseEntity<CampaignContactView> addContact(@PathVariable Long id,
                                                          @Valid @RequestBody ManualCampaignContactRequest request) {
        return new ResponseEntity<>(campaignService.addManualContact(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/contacts")
    public List<CampaignContactView> listCampaignContacts(@PathVariable Long id) {
        return campaignService.listCampaignContacts(id);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Campaign> startCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.startCampaign(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<CampaignStatsDTO> stats(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getStats(id));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<CampaignProgressDTO> progress(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getProgress(id));
    }
}
