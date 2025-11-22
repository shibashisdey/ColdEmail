package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.dto.CampaignCreateRequest;
import com.shibashis.coldmailer.v1.dto.CampaignStatsDTO;
import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.services.CampaignService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    @Autowired
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<Campaign> createCampaign(@Valid @RequestBody CampaignCreateRequest request) {
        Campaign savedCampaign = campaignService.createCampaign(request);
        return new ResponseEntity<>(savedCampaign, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Campaign> getAllCampaigns() {
        return campaignService.getAllCampaigns();
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<CampaignStatsDTO> getCampaignStats(@PathVariable Long id) {
        CampaignStatsDTO stats = campaignService.getCampaignStats(id);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Campaign> startCampaign(@PathVariable Long id) {
        Campaign updatedCampaign = campaignService.startCampaign(id);
        return new ResponseEntity<>(updatedCampaign, HttpStatus.OK);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Campaign> pauseCampaign(@PathVariable Long id) {
        Campaign updatedCampaign = campaignService.pauseCampaign(id);
        return new ResponseEntity<>(updatedCampaign, HttpStatus.OK);
    }
}
