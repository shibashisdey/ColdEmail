package com.shibashis.coldmailer.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampaignFromTextRequest {

    @NotBlank(message = "Campaign name is mandatory")
    private String name;

    @NotNull(message = "Template ID is mandatory")
    private Long templateId;

    @NotBlank(message = "Email list text cannot be blank")
    private String emailListAsText;
}
