package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.models.CampaignProspect;
import com.shibashis.coldmailer.v1.repositories.CampaignProspectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Base64;

@RestController
@RequestMapping("/api/track")
public class TrackingController {

    private final CampaignProspectRepository campaignProspectRepository;

    // A 1x1 transparent GIF, Base64 encoded.
    private static final byte[] TRACKING_PIXEL_GIF = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    @Autowired
    public TrackingController(CampaignProspectRepository campaignProspectRepository) {
        this.campaignProspectRepository = campaignProspectRepository;
    }

    @GetMapping("/open/{token}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable String token) {
        campaignProspectRepository.findByOpenTrackedToken(token).ifPresent(cp -> {
            if (cp.getOpenedAt() == null) {
                cp.setOpenedAt(LocalDateTime.now());
                campaignProspectRepository.save(cp);
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setContentLength(TRACKING_PIXEL_GIF.length);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(TRACKING_PIXEL_GIF, headers, HttpStatus.OK);
    }
}
