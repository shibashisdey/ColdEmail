package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.ProspectData;
import com.shibashis.coldmailer.v1.dto.campaign.ManualCampaignContactRequest;
import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.Contact;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import com.shibashis.coldmailer.v1.repositories.CampaignContactRepository;
import com.shibashis.coldmailer.v1.repositories.ContactRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class CampaignContactImportService {

    private final ContactRepository contactRepository;
    private final CampaignContactRepository campaignContactRepository;
    private final ProspectDerivationService prospectDerivationService;
    private final PlatformPolicyService platformPolicyService;
    private final long maxCsvBytes;
    private final long maxCsvRows;

    public CampaignContactImportService(ContactRepository contactRepository,
                                        CampaignContactRepository campaignContactRepository,
                                        ProspectDerivationService prospectDerivationService,
                                        PlatformPolicyService platformPolicyService,
                                        @Value("${app.csv.max-bytes:1048576}") long maxCsvBytes,
                                        @Value("${app.csv.max-rows:10000}") long maxCsvRows) {
        this.contactRepository = contactRepository;
        this.campaignContactRepository = campaignContactRepository;
        this.prospectDerivationService = prospectDerivationService;
        this.platformPolicyService = platformPolicyService;
        this.maxCsvBytes = maxCsvBytes;
        this.maxCsvRows = maxCsvRows;
    }

    @Transactional
    public int importCsv(Campaign campaign, User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file cannot be empty");
        }
        if (file.getSize() > maxCsvBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV file exceeds allowed size");
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contacts can only be uploaded in DRAFT or PAUSED state");
        }

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim().parse(reader)) {

            int created = 0;
            long processedRows = 0;
            long maxContactsPolicy = platformPolicyService.getLong(PlatformPolicyService.MAX_CONTACTS_PER_CAMPAIGN, 10000);
            Set<Long> existingContactIds = new HashSet<>();
            for (CampaignContact existing : campaignContactRepository.findByCampaignIdOrderByIdAsc(campaign.getId())) {
                existingContactIds.add(existing.getContact().getId());
            }

            for (CSVRecord record : parser) {
                processedRows++;
                if (processedRows > maxCsvRows) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV row count exceeds allowed limit");
                }

                String email = getColumn(record, "email");
                if (isBlank(email)) {
                    continue;
                }

                String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
                Contact contact = contactRepository.findByTenantIdAndEmail(user.getTenantId(), normalizedEmail)
                        .orElseGet(() -> buildContactFromCsv(user, record, normalizedEmail));
                created += attachContactIfEligible(campaign, user, contact, existingContactIds, maxContactsPolicy);
            }
            return created;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse CSV: " + ex.getMessage());
        }
    }

    @Transactional
    public CampaignContact addManualContact(Campaign campaign, User user, ManualCampaignContactRequest request) {
        validateMutableCampaign(campaign);
        long maxContactsPolicy = platformPolicyService.getLong(PlatformPolicyService.MAX_CONTACTS_PER_CAMPAIGN, 10000);
        Set<Long> existingContactIds = existingContactIds(campaign);

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Contact contact = contactRepository.findByTenantIdAndEmail(user.getTenantId(), normalizedEmail)
                .orElseGet(() -> buildContactFromManualRequest(user, request, normalizedEmail));

        int created = attachContactIfEligible(campaign, user, contact, existingContactIds, maxContactsPolicy);
        if (created == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact already exists in campaign");
        }

        return campaignContactRepository.findByCampaignIdOrderByIdAsc(campaign.getId()).stream()
                .filter(campaignContact -> campaignContact.getContact().getId().equals(contact.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create campaign contact"));
    }

    private Contact buildContactFromCsv(User user, CSVRecord record, String email) {
        // Email-derived identity takes priority for consistency.
        ProspectData derived = prospectDerivationService.deriveFromEmail(email);
        String firstName = derived.getFirstName();
        String lastName = derived.getLastName();
        String company = derived.getCompanyName();

        // Fallback to CSV only when derivation misses a field.
        if (isBlank(firstName)) {
            firstName = getColumn(record, "first_name");
        }
        if (isBlank(lastName)) {
            lastName = getColumn(record, "last_name");
        }
        if (isBlank(company)) {
            company = getColumn(record, "company");
        }

        if (isBlank(firstName)) {
            firstName = "Generic";
        }

        Contact contact = new Contact();
        contact.setUser(user);
        contact.setEmail(email);
        contact.setFirstName(firstName);
        contact.setLastName(blankToNull(lastName));
        contact.setCompany(blankToNull(company));
        return contact;
    }

    private Contact buildContactFromManualRequest(User user, ManualCampaignContactRequest request, String email) {
        ProspectData derived = prospectDerivationService.deriveFromEmail(email);

        String firstName = firstNonBlank(derived.getFirstName(), request.getFirstName(), "Generic");
        String lastName = firstNonBlank(derived.getLastName(), request.getLastName());
        String company = firstNonBlank(derived.getCompanyName(), request.getCompany());

        Contact contact = new Contact();
        contact.setUser(user);
        contact.setEmail(email);
        contact.setFirstName(firstName);
        contact.setLastName(blankToNull(lastName));
        contact.setCompany(blankToNull(company));
        return contact;
    }

    private int attachContactIfEligible(Campaign campaign,
                                        User user,
                                        Contact contact,
                                        Set<Long> existingContactIds,
                                        long maxContactsPolicy) {
        validateMutableCampaign(campaign);
        Contact savedContact = contactRepository.save(contact);

        if (existingContactIds.contains(savedContact.getId())) {
            return 0;
        }
        if (existingContactIds.size() >= maxContactsPolicy) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign contact limit reached");
        }

        CampaignContact campaignContact = new CampaignContact();
        campaignContact.setCampaign(campaign);
        campaignContact.setContact(savedContact);
        campaignContact.setStatus(CampaignContactStatus.PENDING);
        campaignContactRepository.save(campaignContact);
        existingContactIds.add(savedContact.getId());
        return 1;
    }

    private Set<Long> existingContactIds(Campaign campaign) {
        Set<Long> existingContactIds = new HashSet<>();
        for (CampaignContact existing : campaignContactRepository.findByCampaignIdOrderByIdAsc(campaign.getId())) {
            existingContactIds.add(existing.getContact().getId());
        }
        return existingContactIds;
    }

    private void validateMutableCampaign(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contacts can only be uploaded in DRAFT or PAUSED state");
        }
    }

    private String getColumn(CSVRecord record, String key) {
        if (record.isMapped(key)) {
            return record.get(key);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }
}
