package com.example.web;
import com.example.cve.Cve;
import com.example.cve.CveRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;  // pour la pagination
import org.springframework.data.domain.Sort;        // pour le tri

import java.util.List;
/**
 * Contrôleur REST très simple pour vérifier que l'appli et la base tournent bien.
 * Expose 3 routes :
 *   - GET /health      → "OK" (sonde de vie rapide)
 *   - GET /cves        → liste des 50 derniers CVE (triés par id décroissant)
 *   - GET /cves/count  → nombre total de CVE en base
 *
 */
@RestController
public class CveController {

    private final CveRepository repo;

    /**
     * Injection par constructeur : Spring fournit automatiquement
     * une instance de CveRepository (bean géré).
     */
    public CveController(CveRepository repo) {
        this.repo = repo;
    }

    /**
     * Sonde de vie très simple.
     * Utile pour tester rapidement depuis l’hôte : curl http://localhost:8080/health
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    /**
     * Retourne jusqu’à 50 enregistrements, triés par id décroissant.
     * Objectif : avoir une vue rapide sans implémenter encore la pagination complète.
     * Remarque : si ta colonne de tri diffère, adapte "id".
     */
    @GetMapping("/cves")
    public List<Cve> list() {
        var page = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(page).getContent();
    }

    /**
     * Donne le nombre total de lignes dans la table cves.
     * Pratique pour vérifier l’ingestion.
     */
    @GetMapping("/cves/count")
    public long count() {
        return repo.count();
    }
}
