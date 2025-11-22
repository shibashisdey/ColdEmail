package com.shibashis.coldmailer.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestEmailRequest {
    @NotBlank
    @Email
    private String to;
}
