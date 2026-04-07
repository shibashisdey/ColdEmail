package com.shibashis.coldmailer.v1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String subject;

    @NotBlank
    private String templateBody;

    private Long emailAccountId;

    private boolean loadBalancedDispatch = false;
}
