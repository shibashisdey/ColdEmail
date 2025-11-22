package com.shibashis.coldmailer.v1.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Template name is mandatory")
    @Column(unique = true)
    private String name;
    
    @NotBlank(message = "Subject is mandatory")
    private String subject;

    @NotBlank(message = "Body is mandatory")
    @Column(columnDefinition = "TEXT")
    private String body;
}
