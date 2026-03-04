package com.shibashis.coldmailer.v1.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSettingUpdateRequest {
    @NotBlank
    private String key;

    @NotBlank
    private String value;
}
