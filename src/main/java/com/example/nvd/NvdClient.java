package com.example.nvd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;                         // URL requête
import java.net.URLEncoder;                 // encodage query params
import java.net.http.HttpClient;            // client HTTP
import java.net.http.HttpRequest;           // requête HTTP
import java.net.http.HttpResponse;          // réponse HTTP
import java.nio.charset.StandardCharsets;   // UTF-8
import java.time.Duration;                  // timeout
import java.util.LinkedHashMap;             // map ordonnée (params)
import java.util.Map;
import java.util.ArrayList;                 // liste résultat
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;    // arbre JSON
import com.fasterxml.jackson.databind.ObjectMapper; // parseur JSON

/**
 * Rôle: Appeler l'API NVD et récupérer les CVE brutes (JSON) pour une date donnée,
 * puis les convertir en NvdCveDto (pas d’accès DB ici).
 */


@Component // bean Spring (injectable dans un runner, service, etc.)
public class NvdClient {

    // ===== Config (injectée via application.properties) =====
    @Value("${nvd.api.base-url}")                  // ex: https://services.nvd.nist.gov/rest/json/cves/2.0
    private String baseUrl;

    @Value("${nvd.api.results-per-page:5}")        // défaut = 5
    private int resultsPerPage;

    @Value("${nvd.api.cvss-v3-severity:}")         // ex: CRITICAL (peut être vide)
    private String severity;

    @Value("${nvd.api.has-kev:false}")             // flag "Known Exploited Vulnerabilities"
    private boolean hasKev;

    @Value("${nvd.http.user-agent:cve-trackr/0.1}") // UA par défaut (overridable)
    private String userAgent;

    @Value("${nvd.http.timeout-ms:10000}")          // 10s par défaut
    private int timeoutMs;

    @Value("${nvd.api.last-mod-start-date:}")
    private String lastModStart;

    @Value("${nvd.api.last-mod-end-date:}")
    private String lastModEnd;

    @Value("${nvd.api.pub-start-date:}")
    private String pubStart;

    @Value("${nvd.api.pub-end-date:}")
    private String pubEnd;

    private final ObjectMapper mapper = new ObjectMapper(); // parseur JSON partagé


    /**
     * Appelle la NVD UNE FOIS sur une plage de dates (ISO UTC) en réutilisant la logique existante.
     * On écrase temporairement les props interne (lastModStart/End + resultsPerPage),
     * on appelle fetchSampleItems(), puis on RESTAURE les anciennes valeurs.
     * @param startIso  ex: "2025-09-10T00:00:00.000Z"
     * @param endIso    ex: "2025-09-13T00:00:00.000Z" (borne FIN EXCLUSIVE: fin+1j à 00:00Z)
     * @param pageSize  nb d’items demandés (ex: 2000). Si <=0, on garde la valeur des props.
     * @return          liste de CveItem parsés (taille <= pageSize)
     * @throws Exception erreur réseau/parse
     */
    public java.util.List<CveItem> fetchByRangeOnce(String startIso, String endIso, int pageSize) throws Exception {
        // sauvegarde des valeurs actuelles (on les remettra dans le finally)
        String oldStart = this.lastModStart;
        String oldEnd   = this.lastModEnd;
        int    oldRpp   = this.resultsPerPage;
        try {
            // surcharge TEMPORAIRE pour cet appel
            this.lastModStart   = startIso;
            this.lastModEnd     = endIso;
            if (pageSize > 0) this.resultsPerPage = pageSize;

            // on réutilise ton pipeline existant: buildUrl() → HTTP → parse → CveItem
            return fetchSampleItems();
        } finally {
            // RESTAURE l’état initial pour ne pas impacter les appels suivants
            this.lastModStart   = oldStart;
            this.lastModEnd     = oldEnd;
            this.resultsPerPage = oldRpp;
        }
    }

    // ===== 1) Construction d'URL (pas d'appel réseau) =====
    // mots-clés: params, encodage, query string, lisibilité
    public String buildUrl() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("resultsPerPage", String.valueOf(resultsPerPage));
        if (!severity.isBlank()) q.put("cvssV3Severity", severity);

        if (!lastModStart.isBlank()) q.put("lastModStartDate", lastModStart);
        if (!lastModEnd.isBlank())   q.put("lastModEndDate", lastModEnd);

        StringBuilder url = new StringBuilder(baseUrl);
        if (!q.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (var e : q.entrySet()) {
                if (!first) url.append("&");
                first = false;
                url.append(e.getKey())
                        .append("=")
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        if (hasKev) url.append(q.isEmpty() ? "?" : "&").append("hasKev");
        return url.toString();
    }

    // ===== 2) Méthode interne factorisée (réseau -> JsonNode) =====
    // mots-clés: DRY, facteur commun, unique point, maintenance
    private JsonNode fetchRootNode() throws Exception {
        String url = buildUrl();
        System.out.println("[NVD] URL: " + url);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs)) // timeout configurable
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)              // UA configurable
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[NVD] HTTP Status = " + response.statusCode());
        if (response.statusCode() != 200) {
            System.out.println("[NVD] Error response:\n" + response.body());
            return null;                                     // signal d'erreur
        }
        return mapper.readTree(response.body());             // JSON -> arbre
    }

    // ===== 3) Variante “demo console” (affichage) =====
    // mots-clés: réutilisation, parsing léger, logs
    public void fetchAndLogSample() throws Exception {
        JsonNode root = fetchRootNode();            // réutilise la méthode factorisée
        if (root == null) return;

        int total = root.path("totalResults").asInt(0);
        System.out.println("[NVD] totalResults = " + total);

        JsonNode vulns = root.path("vulnerabilities"); // liste JSON des vulnérabilités
        int limit = Math.min(resultsPerPage, vulns.size());         // borne max d'affichage = 5
        for (int i = 0; i < limit; i++) {              // boucle bornée
            JsonNode cve = vulns.get(i).path("cve");   // sous-objet "cve"
            String id = cve.path("id").asText("");     // identifiant CVE (fallback "")
            String sev = pickSeverity(cve.path("metrics")); // helper: calcule la sévérité
            System.out.println(" - " + id + (sev != null ? " (sev=" + sev + ")" : "")); // log propre
        }
    }

    // ===== 4) Variante “renvoie des objets” (prêt pour DB) =====
    // mots-clés: DTO, propre, testable, anti-duplication future
    public List<CveItem> fetchSampleItems() throws Exception {
        JsonNode root = fetchRootNode();                 // appel HTTP + JSON centralisé
        if (root == null) return List.of();              // sécurité: pas de données -> liste vide

        JsonNode vulns = root.path("vulnerabilities");   // tableau d'objets "vulnerability"
        int limit = Math.min(resultsPerPage, vulns.size());           // max 5 items
        List<CveItem> out = new ArrayList<>(Math.min(resultsPerPage, vulns.size())); // liste résultat (capacité initiale = limit)

        for (int i = 0; i < limit; i++) {                // boucle bornée
            JsonNode cve = vulns.get(i).path("cve");     // extrait le sous-objet "cve"
            String id   = cve.path("id").asText("");     // cveId (fallback "")
            String desc = pickEnglishDescription(cve.path("descriptions")); // description EN -> fallback
            String sev  = pickSeverity(cve.path("metrics"));                // sévérité calculée

            // normalise la sévérité: UNKNOWN si null/vide
            out.add(new CveItem(id, desc, (sev == null || sev.isBlank()) ? "UNKNOWN" : sev));
        }
        return out; // 3–5 items prêts pour DB/affichage
    }


    // ===== Helpers parsing (lisibilité, responsabilités séparées) =====

    // description: priorité EN, fallback 1er élément
    private String pickEnglishDescription(JsonNode descriptions) {
        if (descriptions != null && descriptions.isArray()) {
            for (JsonNode d : descriptions) {
                if ("en".equalsIgnoreCase(d.path("lang").asText(""))) {
                    return d.path("value").asText("");
                }
            }
            if (!descriptions.isEmpty()) {
                return descriptions.get(0).path("value").asText("");
            }
        }
        return "";
    }

    // severity: CVSS v3.1 -> v3.0 -> v2 -> UNKNOWN
    private String pickSeverity(JsonNode metrics) {
        if (metrics == null || metrics.isMissingNode()) return "UNKNOWN";

        String s = severityFrom(metrics.path("cvssMetricV31"));
        if (s != null) return s;

        s = severityFrom(metrics.path("cvssMetricV30"));
        if (s != null) return s;

        if (metrics.path("cvssMetricV2").isArray() && !metrics.path("cvssMetricV2").isEmpty()) {
            String v2 = metrics.path("cvssMetricV2").get(0).path("baseSeverity").asText(null);
            if (v2 != null && !v2.isBlank()) return v2;
        }
        return "UNKNOWN";
    }

    // extrait baseSeverity pour v3.x (structure: [ { cvssData: { baseSeverity } } ])
    private String severityFrom(JsonNode arr) {
        if (arr != null && arr.isArray() && !arr.isEmpty()) {           // check tableau non vide
            String base = arr.get(0).path("cvssData").path("baseSeverity").asText(null);
            if (base != null && !base.isBlank()) return base;           // retourne si présent
        }
        return null; // signale "rien trouvé" au caller
    }

    // ===== Petit utilitaire de vérif (étape 1) =====
    public void logBuiltUrl() {
        System.out.println("[NVD] Built URL = " + buildUrl());
        System.out.println("[NVD] Step 1/4 OK (config injection + URL build)");
    }
}
