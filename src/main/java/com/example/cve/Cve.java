package com.example.cve;

import jakarta.persistence.*; // JPA annotations

/**
 * Entity CVE
 * - ORM mapping
 * - Table: cves
 */
@Entity                       // entité JPA (table SQL)
@Table(name = "cves")         // nom table explicite
public class Cve {

    @Id                                           // clé primaire
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-incrément (Postgres)
    private Long id;                               // id technique

    @Column(name = "cve_id", nullable = false, unique = true)
    // name: nom colonne ; nullable=false: obligatoire ; unique=true: contrainte unique
    private String cveId;      // identifiant métier (ex: CVE-2024-1234)

    @Column(columnDefinition = "text") // type SQL TEXT (long)
    private String description; // description longue

    private String severity;    // LOW/MEDIUM/HIGH/CRITICAL (valeur simple)

    // --- Getters/Setters (JavaBean) ---
    public Long getId() { return id; }                 // getter id
    public void setId(Long id) { this.id = id; }       // setter id

    public String getCveId() { return cveId; }         // getter cveId
    public void setCveId(String cveId) { this.cveId = cveId; } // setter cveId

    public String getDescription() { return description; }     // getter description
    public void setDescription(String description) { this.description = description; } // setter description

    public String getSeverity() { return severity; }   // getter severity
    public void setSeverity(String severity) { this.severity = severity; } // setter severity
}
