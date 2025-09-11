package com.example.service;

import com.example.cve.Cve;
import com.example.cve.CveRepository;
import com.example.nvd.CveItem;
import com.example.nvd.NvdClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service // logique métier: API -> DB
public class CveIngestService {
    private final NvdClient nvd;
    private final CveRepository repo;

    public CveIngestService(NvdClient nvd, CveRepository repo) {
        this.nvd = nvd;
        this.repo = repo;
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
}