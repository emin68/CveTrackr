# CVETrackr

CVETrackr is a **tool for automated ingestion of CVE vulnerabilities**.  
It queries the official [NVD (National Vulnerability Database)](https://nvd.nist.gov/) API  
and stores the results in a **PostgreSQL database** using **Spring Boot 3** and **JPA/Hibernate**.

---

##  Features

- **NVD API v2.0**: fetch CVE data (JSON).
- **PostgreSQL storage**:
  - Entity `Cve (id, cveId, description TEXT, severity)`.
  - **Duplicate prevention** (`existsByCveId`), **batch inserts**.
- **Time filters**: `lastModStartDate / lastModEndDate`, `pubStartDate / pubEndDate`.
- **Upsert** strategy (update on existing IDs) & pagination support.
- **HTTP API**:
  - `GET /health` → `"OK"`
  - `GET /cves` → list (simple pagination)
  - `GET /cves/count` → row count
  - **POST endpoints**: you can **create CVEs** and **trigger ingestion** (see **Testing**).

- **Containerized**:
  - **Dockerfile** for the app image.
  - **docker-compose** to run **app + Postgres in one command**.
  - **Volume** for DB persistence.

---

##  Project Structure

```text
src/main/java/com/example
├─ CveTrackrApplication.java # Spring Boot main
├─ cve/
│ ├─ Cve.java # JPA entity
│ └─ CveRepository.java # Spring Data JPA repo
├─ nvd/
│ ├─ NvdClient.java # HTTP client for NVD API
│ ├─ CveItem.java # DTO (API → DB)
│ └─ NvdDateUtils.java # date helpers (NVD filters/ranges)
├─ service/
│ └─ CveIngestService.java # batch ingestion
└─ web/
├─ CveController.java # GET/POST CVE endpoints
└─ IngestionController.java # POST ingestion endpoint(s)


## Tech Stack

- **Java 17**
- **Spring Boot 3** (Data JPA, Web)
- **Hibernate** (ORM)
- **PostgreSQL**
- **Jackson** (JSON parsing)
- **Maven** (build/dependency management)
- **Docker** + **docker-compose** (v1 or v2)

---

## Requirements

- **Docker** installed & running
- **docker-compose**
  - v1: command is `docker-compose`
  - v2: command is `docker compose`
- **Java 17** (only for building the JAR locally)
- **PostgreSQL** with a database and user:
```

# Create your local env file from the example:

```bash
cp .env.example .env
# edit values if you need to
```

# Clone the repo
```bash
git clone https://github.com/emin68/CveTrackr.git
cd CveTrackr
```

# Configure your .properties
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

## TO RUN

- Build the jar :
```bash
	./mvnw -Dmaven.test.skip=true clean package
```
	
-Compose v1 :
```bash
	docker-compose build
	docker-compose up -d
```
	
-Compose v2 :
```bash
	docker compose build
	docker compose up -d
```
-Check :
```bash
	curl http://localhost:8080/health
	curl http://localhost:8080/cves
```
-Stop :
```bash
	docker-compose down
	# or: docker compose down
```

## Testing (GET & POST)

Once the containers are running, you can test the application using both **POST** and **GET** requests. To trigger ingestion from the NVD API, send a POST request with a date window (format `YYYY-MM-DD`), for example:  
```bash
curl -s -X POST "http://localhost:8080/api/ingest?start=2025-09-16&end=2025-09-17"
# Example output:
# Inserted 40 (duplicates: 0) for 2025-09-16 → 2025-09-17
```
After ingestion, you can verify the database state with GET requests:
```bash
curl -i http://localhost:8080/health         # → 200 OK + "OK"
curl -s "http://localhost:8080/cves?page=0&size=5" | jq .   # → list of CVEs
curl -s http://localhost:8080/cves/count     # → total number of CVEs (e.g., 41)
```


