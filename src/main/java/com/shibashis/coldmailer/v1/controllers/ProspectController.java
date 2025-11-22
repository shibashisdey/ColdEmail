package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.models.Prospect;
import com.shibashis.coldmailer.v1.repositories.ProspectRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private final ProspectRepository prospectRepository;

    @Autowired
    public ProspectController(ProspectRepository prospectRepository) {
        this.prospectRepository = prospectRepository;
    }

    @PostMapping
    public ResponseEntity<Prospect> createProspect(@Valid @RequestBody Prospect prospect) {
        Prospect savedProspect = prospectRepository.save(prospect);
        return new ResponseEntity<>(savedProspect, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Prospect> getAllProspects() {
        return prospectRepository.findAll();
    }
}
