package com.shibashis.coldmailer.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProspectData {
    private String firstName;
    private String lastName;
    private String companyName;
}
