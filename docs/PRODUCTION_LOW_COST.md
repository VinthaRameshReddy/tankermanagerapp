# Low-cost production deployment (no photo storage)

This matches the target scale:

| Metric | Estimate |
|--------|----------|
| Operators (tenants) | ~100 |
| Vehicles + customers per operator | ~200 |
| Trips | ~15/operator/day → **~1,500/day**, **~45,000/month** |
| Data | Mostly text/numbers (trips, GPS, payments) — **no S3** needed |

## What TankerFlow uses today

| Layer | Current | Notes |
|-------|---------|--------|
| Mobile app | **Android (TankerFlow)** — Kotlin / Compose | Owner, manager, driver |
| API | **Spring Boot 3.3** (Java 17) | `backend/` |
| Database | **PostgreSQL** | Render today; Hibernate + Flyway-style DDL |
| Hosting | Render (API + external Postgres URL) | See `backend/render.yaml` |

**Angular web UI** is optional later (manager desk / reports). The architecture below leaves a slot for it behind Nginx; mobile apps talk to the same API.

## Recommended production stack (₹1,000–₹1,500/month to start)

| Component | Recommendation |
|-----------|----------------|
| Server | **AWS Lightsail** — **Mumbai (`ap-south-1`)** |
| Size | **2 GB RAM**, 1–2 vCPU (scale up if CPU pegs during peak booking) |
| Database | **Managed DB** on Lightsail — **PostgreSQL** (matches this repo) or MySQL if you migrate dialect later |
| Backend | Spring Boot JAR in **Docker** |
| Edge | **Nginx** — HTTPS (Let's Encrypt), reverse proxy to API |
| Web (optional) | Angular static build served by Nginx |
| CI/CD | **GitHub Actions** → build image → SSH deploy on Lightsail |
| Backup | Lightsail DB **automatic snapshots** + optional nightly `pg_dump` to object storage (small cost) |

No object storage (S3) is required without photos, PDFs, or file uploads.

## Architecture

```text
          Owners / Managers / Drivers
                      │
                      ▼
            TankerFlow (Android APK)
                      │
                    HTTPS
                      │
                      ▼
                    Nginx  (:443)
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
   /api/*  →  Spring Boot     /  →  Angular (optional)
          │      :8080
          ▼
    PostgreSQL (managed)
          │
    ┌─────┴─────┐
    │           │
 Operators   Customers
    │           │
    └─────┬─────┘
          ▼
    Vehicles / Locations
          ▼
        Trips
          ▼
     Payments / Reports
```

## Environment variables (API)

| Variable | Purpose |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | JDBC URL (Postgres) **or** `SPRING_DATASOURCE_*` for MySQL if migrated |
| `JWT_SECRET` | Strong random secret |
| `CORS_ALLOWED_ORIGINS` | App / web origins if needed |

Android app: set API base URL to `https://your-domain.com` in release build config.

## Migration from Render

1. Provision Lightsail instance + managed PostgreSQL (same region).
2. `pg_dump` from Render → restore on Lightsail DB.
3. Deploy API with `deploy/docker-compose.prod.yml` (see repo).
4. Point DNS A record to Lightsail static IP; run Certbot on Nginx.
5. Smoke-test login, book trip, GPS upload, public track link.
6. Cut over Android APK to new API URL; retire Render when stable.

## Capacity notes

- **45k trips/month** with indexed `operator_id`, `created_at`, `driver_id` is modest for Postgres on 2 GB RAM.
- Live GPS updates are small JSON payloads; no file I/O.
- Watch connection pool size (`spring.datasource.hikari.maximum-pool-size` ~10–20 on 2 GB).

## Optional: MySQL instead of PostgreSQL

The codebase is configured for **PostgreSQL** (`ProdDataSourceConfig`, dialect). Switching to MySQL means:

- Add `mysql-connector-j` dependency
- `spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect`
- Test migrations on a copy of production data

Until then, use **PostgreSQL on Lightsail** to avoid a database migration project.

## Related files in this repo

- `backend/Dockerfile` — API image
- `deploy/docker-compose.prod.yml` — API + Nginx (+ optional Angular volume)
- `deploy/nginx/api.conf` — reverse proxy template
- `.github/workflows/deploy-lightsail.yml` — deploy skeleton (fill secrets)
