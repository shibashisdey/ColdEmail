package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import com.shibashis.coldmailer.v1.repositories.CampaignContactRepository;
import com.shibashis.coldmailer.v1.services.CampaignResumeService;
import com.shibashis.coldmailer.v1.services.PlatformPolicyService;
import org.springframework.core.io.Resource;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/track")
public class TrackingController {

    private final CampaignContactRepository campaignContactRepository;
    private final CampaignResumeService campaignResumeService;
    private final PlatformPolicyService platformPolicyService;

    private static final byte[] TRACKING_PIXEL_GIF = Base64.getDecoder()
            .decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    public TrackingController(CampaignContactRepository campaignContactRepository,
                              CampaignResumeService campaignResumeService,
                              PlatformPolicyService platformPolicyService) {
        this.campaignContactRepository = campaignContactRepository;
        this.campaignResumeService = campaignResumeService;
        this.platformPolicyService = platformPolicyService;
    }

    @GetMapping("/open/{trackingId}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable UUID trackingId) {
        if (!platformPolicyService.getBoolean(PlatformPolicyService.TRACK_OPEN_ENABLED, true)) {
            return new ResponseEntity<>(TRACKING_PIXEL_GIF, HttpStatus.OK);
        }
        Optional<CampaignContact> optional = campaignContactRepository.findByTrackingId(trackingId);
        if (optional.isPresent()) {
            CampaignContact cc = optional.get();
            if (cc.getOpenedAt() == null) {
                cc.setOpenedAt(LocalDateTime.now());
            }
            if (cc.getStatus() == CampaignContactStatus.SENT) {
                cc.setStatus(CampaignContactStatus.OPENED);
            }
            campaignContactRepository.save(cc);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setContentLength(TRACKING_PIXEL_GIF.length);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(TRACKING_PIXEL_GIF, headers, HttpStatus.OK);
    }

    @GetMapping("/resume/{trackingId}")
    public ResponseEntity<Resource> trackResumeDownload(@PathVariable UUID trackingId) {
        if (!platformPolicyService.getBoolean(PlatformPolicyService.TRACK_RESUME_ENABLED, true)) {
            return ResponseEntity.notFound().build();
        }
        CampaignContact cc = campaignContactRepository.findByTrackingId(trackingId)
                .orElse(null);
        if (cc == null) {
            return ResponseEntity.notFound().build();
        }

        if (cc.getResumeDownloadedAt() == null) {
            cc.setResumeDownloadedAt(LocalDateTime.now());
        }
        cc.setStatus(CampaignContactStatus.RESUME_DOWNLOADED);
        campaignContactRepository.save(cc);

        Resource resource = campaignResumeService.loadResumeResource(cc.getCampaign());
        String contentType = cc.getCampaign().getResumeContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String originalFileName = cc.getCampaign().getResumeOriginalFileName();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "resume";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalFileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
