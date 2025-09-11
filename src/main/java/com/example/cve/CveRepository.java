package com.example.cve;

import org.springframework.data.jpa.repository.JpaRepository; // CRUD auto
import org.springframework.stereotype.Repository;            // stéréotype Spring

@Repository // bean Spring Data
public interface CveRepository extends JpaRepository<Cve, Long> {
    // méthodes dérivées (query methods) → auto-implémentées

    boolean existsByCveId(String cveId); // lookup unique (anti-doublon)
    Cve findByCveId(String cveId);       // recherche par id métier (utile ingestion)
}
