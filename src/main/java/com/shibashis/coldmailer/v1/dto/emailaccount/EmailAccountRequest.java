package com.shibashis.coldmailer.v1.dto.emailaccount;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailAccountRequest {

    @NotBlank
    private String label;

    @NotBlank
    private String smtpHost;

    @NotNull
    @Min(1)
    private Integer smtpPort;

    @NotBlank
    private String smtpUsername;

    @NotBlank
    private String smtpPassword;

    @NotBlank
    @Email
    private String fromEmail;

    private boolean useTls = true;

    private boolean isDefault = false;
}
