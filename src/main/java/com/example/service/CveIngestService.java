package com.example.service;

import com.example.cve.Cve;
import com.example.cve.CveRepository;
import com.example.nvd.CveItem;
import com.example.nvd.NvdClient;
import com.example.nvd.NvdDateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import com.example.config.DateRangeProperties;
import java.util.stream.Collectors;

/**
 * Rôle: Orchestration de l’ingestion: forcer NvdApiClient -> mapper -> enregistrer via CveRepository.
 * Ne fait pas d'appel HTTP direct ni de SQL brut: délègue aux composants dédiés.
 */

@Service // logique métier: API -> DB
public class CveIngestService {
    private final NvdClient nvd;
    private final CveRepository repo;
    private final DateRangeProperties dates;


    public CveIngestService(NvdClient nvd, CveRepository repo, DateRangeProperties dates) {
        this.nvd = nvd;
        this.repo = repo;
        this.dates = dates;
    }

    @Transactional // une seule transaction: propre & rapide
    public int ingestSample() throws Exception {
        // 1) lire 3–5 items depuis l’API (déjà parsés)
        List<CveItem> items = nvd.fetchSampleItems();
        if (items.isEmpty()) return 0; // rien à faire

        // 2) set d’IDs pour lookup (O(1) par test)
        List<String> ids = items.stream().map(i -> i.cveId).toList();

        // 3) 1 seul round-trip DB pour connaître ceux qui existent déjà
        Set<String> existing = new HashSet<>(repo.findExistingIds(ids));

        // 4) construire la liste à insérer (filtrer doublons)
        List<Cve> toSave = new ArrayList<>();
        for (CveItem it : items) {
            if (!existing.contains(it.cveId)) {
                Cve e = new Cve();
                e.setCveId(it.cveId);
                e.setDescription(it.description); // déjà “long text” côté JPA
                e.setSeverity(it.severity == null ? "UNKNOWN" : it.severity.toUpperCase());
                toSave.add(e);
            }
        }


        // 5) insertion batch (1 appel)
        if (!toSave.isEmpty()) repo.saveAll(toSave);
        return toSave.size(); // nb de nouvelles lignes
    }

    /**
     * Lance l’ingestion sur une période.
     * - Si les paramètres sont null → utilise les dates par défaut (DateRangeProperties).
     * - Convertit les LocalDate au format NVD (ISO UTC), fin EXCLUSIVE = fin + 1 jour.
     * - Appelle la NVD en 1 seule page (pageSize=2000), mappe et enregistre.
     * - Ignore les doublons (index unique conseillé sur cve_id).
     * @return résumé texte (insérées / doublons / période)
     */
    public String ingestBetween(java.time.LocalDate debut, java.time.LocalDate fin) {
        // 1) Choix des dates effectives (paramètres si présents, sinon valeurs par défaut)
        java.time.LocalDate s = (debut != null) ? debut : dates.getStartDate();
        java.time.LocalDate e = (fin   != null) ? fin   : dates.getEndDate();
        if (s == null || e == null) {
            throw new IllegalStateException("Dates par défaut manquantes (cve.start-date / cve.end-date).");
        }

        // 2) Conversion vers le format attendu par la NVD (ISO UTC 'Z')
        //    Borne fin EXCLUSIVE: on ajoute 1 jour pour couvrir toute la journée 'fin'
        String startIso = com.example.nvd.NvdDateUtils.localDateToNvdDate(s);
        String endIso   = com.example.nvd.NvdDateUtils.localDateToNvdDate(e.plusDays(1));

        // 3) Appel NVD (version simple: une seule grosse page)
        java.util.List<com.example.nvd.CveItem> items;
        try {
            items = nvd.fetchByRangeOnce(startIso, endIso, 2000);
        } catch (Exception ex) {
            return "Erreur NVD: " + ex.getMessage();
        }

        // 4) Mapping + insert en base, comptage des doublons
        int inserted = 0, duplicates = 0;
        for (var it : items) {
            var ent = new com.example.cve.Cve();
            ent.setCveId(it.cveId);
            ent.setDescription(it.description);
            ent.setSeverity((it.severity == null || it.severity.isBlank()) ? "UNKNOWN" : it.severity.toUpperCase());
            try {
                repo.save(ent);       // insertion
                inserted++;
            } catch (org.springframework.dao.DataIntegrityViolationException d) {
                // doublon (si index unique sur cve_id) → on ignore
                duplicates++;
            }
        }

        // 5) Résumé pour l'appelant (Postman verra ça)
        return "Insérées " + inserted + " (doublons: " + duplicates + ") pour " + s + " → " + e;
    }

}