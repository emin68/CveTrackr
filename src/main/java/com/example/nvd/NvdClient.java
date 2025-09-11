package com.example.nvd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

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
}
