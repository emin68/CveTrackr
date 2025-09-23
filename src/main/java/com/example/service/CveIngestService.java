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

    // version minimale pour tester la route (à améliorer ensuite)
    public String ingestBetween(LocalDate debut, LocalDate fin) {
        LocalDate s = (debut != null) ? debut : dates.getStartDate();
        LocalDate e = (fin   != null) ? fin   : dates.getEndDate();
        if (s == null || e == null) {
            throw new IllegalStateException("Dates par défaut manquantes (cve.start-date / cve.end-date).");
        }

        String startIso = NvdDateUtils.localDateToNvdDate(s);
        String endIso   = NvdDateUtils.localDateToNvdDate(e.plusDays(1)); // borne fin exclusive

        // TODO (ensuite) : appeler le client NVD avec ces 2 valeurs, paginer, sauvegarder
        // nvd.fetchBetween(startIso, endIso, startIndex, pageSize);

        return "Ingestion demandée pour la période " + s + " → " + e;
    }
}