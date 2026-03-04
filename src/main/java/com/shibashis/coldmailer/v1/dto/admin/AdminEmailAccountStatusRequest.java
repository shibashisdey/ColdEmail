package com.shibashis.coldmailer.v1.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminEmailAccountStatusRequest {
    @NotNull
    private Boolean active;
    private String reason;
}
