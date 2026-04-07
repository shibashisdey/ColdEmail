package com.shibashis.coldmailer.v1.dto.campaign;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualCampaignContactRequest {

    @NotBlank
    @Email
    private String email;

    private String firstName;

    private String lastName;

    private String company;
}
