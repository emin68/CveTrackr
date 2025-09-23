package com.example.web;

import com.example.config.DateRangeProperties;
import com.example.service.CveIngestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/**
 * Route d’ingestion CVE : déclenche la récupération NVD et l’enregistrement en base.
 * POST /api/ingest  (paramètres optionnels : start, end au format AAAA-MM-JJ)
 *
 * Exemple :
 *   POST http://localhost:8080/api/ingest?start=2025-09-10&end=2025-09-12
 */


@RestController
@RequestMapping("/api")
public class IngestionController {

    private final CveIngestService service;
    private final DateRangeProperties dates;

    public IngestionController(CveIngestService service, DateRangeProperties dates) {
        this.service = service;
        this.dates = dates;
    }

    // POST /api/ingest?start=AAAA-MM-JJ&end=AAAA-MM-JJ (params facultatifs)
    @PostMapping("/ingest")
    public String lancer(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDate s = (start != null) ? start : dates.getStartDate();
        LocalDate e = (end   != null) ? end   : dates.getEndDate();
        return service.ingestBetween(s, e);
    }
}
