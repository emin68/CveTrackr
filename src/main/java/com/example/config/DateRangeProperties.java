package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.LocalDate;
/**
 * Rôle: Lire la plage de dates (début/fin) depuis les .properties (ou variables d’environnement).
 */

@ConfigurationProperties(prefix = "cve")
public class DateRangeProperties {
    private LocalDate startDate;
    private LocalDate endDate;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
