package com.example.nvd;
/**
 * Rôle: Objet de transfert (DTO) représentant une CVE telle que renvoyée par la NVD.
 * Ne pas persister cet objet tel quel en DB (utiliser l’entity Cve).
DTO = Data Transfer Object = objet de transport de données.
→ Sert à transférer des infos entre couches/systèmes (ex : réponse de l’API NVD, ou payload HTTP).
**/

// DTO minimal pour porter API -> Service -> DB
public class CveItem {
    public final String cveId;
    public final String description;
    public final String severity;

    public CveItem(String cveId, String description, String severity) {
        this.cveId = cveId;
        this.description = description;
        this.severity = severity;
    }
}
