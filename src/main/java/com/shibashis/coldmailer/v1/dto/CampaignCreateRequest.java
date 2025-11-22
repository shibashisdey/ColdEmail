package com.shibashis.coldmailer.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CampaignCreateRequest {

    @NotBlank(message = "Campaign name is mandatory")
    private String name;

    @NotNull(message = "Template ID is mandatory")
    private Long templateId;

    private List<String> emailAddresses; // Changed from prospectIds
}
