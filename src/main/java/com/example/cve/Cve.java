package com.example.cve;

import jakarta.persistence.*;
/**
 * Entité représentant une vulnérabilité CVE stockée en base.
 * Chaque objet de cette classe correspond à une ligne en base.
 */
@Entity
@Table(name = "cves")
public class Cve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // ID interne auto-incrémenté

    @Column(name = "cve_id", nullable = false, unique = true)
    private String cveId;      // ex: CVE-2024-1234

    @Column(columnDefinition = "text") // <-- description longue
    private String description; // courte description

    private String severity;    // niveau de sévérité (LOW, MEDIUM, HIGH, CRITICAL)

    // --- Getters/Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
