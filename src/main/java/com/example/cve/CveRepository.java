package com.example.cve;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CveRepository extends JpaRepository<Cve, Long> {
    boolean existsByCveId(String cveId);
}
