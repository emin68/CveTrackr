package com.example;

import com.example.cve.Cve;
import com.example.cve.CveRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;  // <-- important

@SpringBootApplication
public class CveTrackrApplication {

    public static void main(String[] args) {
        SpringApplication.run(CveTrackrApplication.class, args);
    }

    // S'exécute au démarrage une seule fois (logiquement)
    @Bean
    CommandLineRunner demo(CveRepository repo) {
        return args -> {
            String testId = "CVE-TEST-0001";
            if (!repo.existsByCveId(testId)) {     // <-- évite le doublon
                Cve v = new Cve();
                v.setCveId(testId);
                v.setDescription("Row de test pour valider JPA -> Postgres");
                v.setSeverity("LOW");
                repo.save(v);
            }
            System.out.println(">>> CVE rows in DB = " + repo.count());
        };
    }
}
