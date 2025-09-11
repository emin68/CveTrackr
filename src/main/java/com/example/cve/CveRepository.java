package com.example.cve;

import org.springframework.data.jpa.repository.JpaRepository; // CRUD auto
import org.springframework.stereotype.Repository;            // stéréotype Spring
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

@Repository // bean Spring Data
public interface CveRepository extends JpaRepository<Cve, Long> {
    // méthodes dérivées (query methods) → auto-implémentées

    boolean existsByCveId(String cveId); // lookup unique (anti-doublon)

    // renvoie seulement les cveId qui existent déjà (léger, pas d’objets complets)
    @Query("select c.cveId from Cve c where c.cveId in :ids")
    List<String> findExistingIds(@Param("ids") Collection<String> ids);
}
