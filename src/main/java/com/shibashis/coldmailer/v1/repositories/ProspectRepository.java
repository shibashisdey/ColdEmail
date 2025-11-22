package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.Prospect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, Long> {
}
