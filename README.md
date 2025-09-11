# CVETrackr

CVETrackr is a **tool for automated ingestion of CVE vulnerabilities**.  
It queries the official [NVD (National Vulnerability Database)](https://nvd.nist.gov/) API  
and stores the results in a **PostgreSQL database** using **Spring Boot 3** and **JPA/Hibernate**.

---

##  Features

-  **NVD API connection**: fetch CVE data in JSON (REST API v2.0).
-  **PostgreSQL storage**:
  - Entity `Cve` (`id`, `cveId`, `description`, `severity`).
  - `description` column set to `TEXT` (supports long descriptions).
-  **Duplicate prevention**: insert only if `cveId` is not already present.
-  **Batch insertion optimization** with `saveAll()` and Hibernate batch settings.
-  **Time filters**:
  - `lastModStartDate` / `lastModEndDate` (recently modified CVEs).
  - `pubStartDate` / `pubEndDate` (recently published CVEs) – **ready to activate**.
-  Pagination and Upsert (update existing CVEs).

---

##  Project Structure

```text
src/main/java/com/example
 ├── CveTrackrApplication.java       # Main Spring Boot app
 ├── cve/
 │    ├── Cve.java                   # JPA entity
 │    └── CveRepository.java         # Spring Data JPA repository
 ├── nvd/
 │    ├── NvdClient.java             # NVD API client (HTTP, JSON parsing)
 │    └── CveItem.java               # DTO (API -> DB)
 └── service/
      └── CveIngestService.java      # Batch ingestion service


## Tech Stack

- **Java 17**
- **Spring Boot 3** (Data JPA, Web)
- **Hibernate** (ORM)
- **PostgreSQL**
- **Jackson** (JSON parsing)
- **Maven** (build/dependency management)

---

## Requirements

- **Java 17+** installed
- **PostgreSQL** with a database and user:

```sql
CREATE DATABASE nvd_db;
CREATE USER nvd_user WITH ENCRYPTED PASSWORD 'secret';
GRANT ALL PRIVILEGES ON DATABASE nvd_db TO nvd_user;


# Clone the repo
git clone https://github.com/emin68/CveTrackr.git
cd CveTrackr

# Configure your .properties
cp src/main/resources/application.properties.example src/main/resources/application.properties


To run
./mvnw spring-boot:run

Check the Database
psql -U nvd_user -d nvd_db -h localhost
SELECT id, cve_id, severity, LEFT(description,80) FROM cves ORDER BY id DESC LIMIT 5;


