# Tanker Manager — Spring Boot API (Java 17)

Multi-tenant SaaS backend for water/tanker fleet operators. Deploy on **Render** with PostgreSQL.

## Stack

- Java 17 + Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL (prod) / H2 (local)
- Swagger UI at `/swagger-ui.html`

## Roles

| Role | Access |
|------|--------|
| SUPER_ADMIN | Register operators + owners (sell product). Login: `9999999999` / `Admin@123` |
| OWNER | Create managers & drivers; customers, fleet, trips, expenses |
| MANAGER | Add customers, book trips, fleet ops (cannot create staff) |
| DRIVER | Assigned trips, status updates, GPS |
| Customer | Public track link (no app login) |

**Flow:** Super Admin registers owner → Owner creates managers/drivers → Owner/Manager add customers & book trips.

## Run locally (Java 17)

```bash
cd backend
set JAVA_HOME=C:\Program Files\Java\jdk-17
mvn spring-boot:run
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html  

### Seeded Super Admin

- Phone: `9999999999`
- Password: `Admin@123`

## Quick API flow

1. **Super Admin login** → `POST /api/auth/login` (`9999999999` / `Admin@123`)
2. **Register operator + owner** → `POST /api/admin/operators`
3. **Owner login** → create staff → `POST /api/auth/staff` (`MANAGER` or `DRIVER`)
4. **Owner/Manager** → customers, bores, tankers, trips under `/api/manager/**`
5. **Driver** → `/api/driver/**` status + GPS
6. **Customer track** → `GET /api/public/track/{token}`

### Trip statuses

`ASSIGNED` → `GOING_FOR_LOADING` → `LOADING` → `LOADING_COMPLETED` → `EN_ROUTE` → `ARRIVED` → `UNLOADING` → `COMPLETED`

SMS is sent on each status change (console log locally; set `SMS_ENABLED=true` + API key for Fast2SMS/MSG91).

## Deploy on Render

1. Push `backend/` to GitHub  
2. In Render: New → Blueprint → select repo (uses `render.yaml`)  
   Or: New Web Service → Docker → root `backend/`  
3. Attach PostgreSQL and set env vars:

| Variable | Notes |
|----------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | e.g. `jdbc:postgresql://HOST:5432/tankermanager` |
| `DB_USERNAME` | DB user |
| `DB_PASSWORD` | DB password |
| `JWT_SECRET` | long random string (32+ chars) |
| `SMS_ENABLED` | `true` when ready |
| `GOOGLE_MAPS_API_KEY` | optional, for traffic ETA |

**Important:** Render Postgres often gives a URL like `postgres://user:pass@host/db`. Convert to JDBC:

```
jdbc:postgresql://host:5432/db
```

Or add a small URL converter — set `DATABASE_URL` to the JDBC form in Render dashboard.

## Features covered

- Multi-operator (tenant) isolation  
- Book trip by customer phone + assign tanker/driver  
- Bore → customer route + ETA  
- Live GPS while trip active; hidden after complete  
- Status SMS to customer  
- Diesel/maintenance expenses per vehicle  
- Bore power/maintenance costs  
- Driver salaries & trip performance counts  
- Super Admin operator onboarding  
