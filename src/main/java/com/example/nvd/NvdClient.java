package com.example.nvd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component // bean Spring
public class NvdClient {

    // --- config depuis application.properties ---
    @Value("${nvd.api.base-url}")
    private String baseUrl;

    @Value("${nvd.api.results-per-page:5}")
    private int resultsPerPage;

    @Value("${nvd.api.cvss-v3-severity:}")
    private String severity;

    @Value("${nvd.api.has-kev:false}")
    private boolean hasKev;

    // build URL (pas d'appel réseau ici)
    public String buildUrl() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("resultsPerPage", String.valueOf(resultsPerPage));
        if (!severity.isBlank()) q.put("cvssV3Severity", severity);

        StringBuilder url = new StringBuilder(baseUrl);
        if (!q.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (var e : q.entrySet()) {
                if (!first) url.append("&");
                first = false;
                url.append(e.getKey()).append("=")
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        if (hasKev) url.append(q.isEmpty() ? "?" : "&").append("hasKev");
        return url.toString();
    }

    // petite méthode de vérif
    public void logBuiltUrl() {
        System.out.println("[NVD] Built URL = " + buildUrl());
        System.out.println("[NVD] Step 1/4 OK (config injection + URL build)");
    }

    /** Appelle l'API NVD et affiche quelques CVE */
    public void fetchAndLogSample() throws Exception {
        String url = buildUrl();
        System.out.println("[NVD] Calling URL: " + url);

        // === Client HTTP ===
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "cve-trackr/0.1")
                .GET()
                .build();

        // === Envoi requête ===
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[NVD] HTTP Status = " + response.statusCode());

        if (response.statusCode() != 200) {
            System.out.println("[NVD] Error response:\n" + response.body());
            return; // stop si erreur
        }

        // === Parsing JSON ===
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        int total = root.path("totalResults").asInt(0);
        System.out.println("[NVD] totalResults = " + total);

        JsonNode vulns = root.path("vulnerabilities");
        int limit = Math.min(5, vulns.size());
        for (int i = 0; i < limit; i++) {
            JsonNode cve = vulns.get(i).path("cve");
            String id = cve.path("id").asText("");
            String sev = cve.path("metrics")
                    .path("cvssMetricV31")
                    .path(0).path("cvssData")
                    .path("baseSeverity").asText(null);
            System.out.println(" - " + id + (sev != null ? " (sev=" + sev + ")" : ""));
        }
    }
}
