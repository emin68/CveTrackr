package com.example;

import com.example.cve.Cve;                     // entité
import com.example.cve.CveRepository;           // repo JPA
import org.springframework.boot.CommandLineRunner; // runner démarrage
import org.springframework.boot.SpringApplication; // boot
import org.springframework.boot.autoconfigure.SpringBootApplication; // auto-config
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean; // définir bean
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.example.config.DateRangeProperties;

@SpringBootApplication // scan composants, auto-config, @Configuration
@EnableConfigurationProperties(DateRangeProperties.class)
public class CveTrackrApplication {

    public static void main(String[] args) {
        SpringApplication.run(CveTrackrApplication.class, args); // démarrage app
    }
    //test la connexion a la bdd
    @Bean // expose bean au contexte Spring
    CommandLineRunner demo(CveRepository repo) { // injection repo (DI)
        return args -> { // lambda = run(String... args)
            String testId = "CVE-TEST-0001"; // identifiant fonctionnel (clé métier)
            if (!repo.existsByCveId(testId)) { // anti-doublon (check rapide)
                Cve v = new Cve();             // nouvelle entité (transient)
                v.setCveId(testId);            // champ unique (business id)
                v.setDescription("Row de test pour valider JPA -> Postgres"); // données test
                v.setSeverity("LOW");          // valeur simple
                repo.save(v);                  // persist (INSERT) -> transaction JPA
            }
            System.out.println(">>> CVE rows in DB = " + repo.count()); // feedback console
        };
    }

    @Bean
    CommandLineRunner showDates() {
        return args -> {
            var nowUtc = ZonedDateTime.now(ZoneOffset.UTC);     // <-- UTC strict
            var start  = nowUtc.minusDays(30);

            var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // <-- suffixe Z
            System.out.println("Start date (30j): " + start.format(fmt));
            System.out.println("End date (now):   " + nowUtc.format(fmt));
        };
    }

    //test le get via lurl
    @Bean
    CommandLineRunner nvdSmoke(com.example.nvd.NvdClient nvd) {
        return args -> {
            System.out.println(">>> NVD Smoke Test: Fetch + Parse");
            try {
                nvd.fetchAndLogSample(); // appelle la nouvelle méthode
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
    @Bean // déclenche l’ingest au démarrage (temporaire)
    CommandLineRunner nvdIngest(com.example.service.CveIngestService svc) {
        return args -> {
            System.out.println(">>> NVD Ingest (sample 3–5 CVE) -> DB");
            int n = svc.ingestSample();
            System.out.println(">>> Upserts: " + n); // attendu: 3–5 puis 0 au 2e run
        };
    }

}
