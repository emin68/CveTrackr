package com.example.nvd;

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
