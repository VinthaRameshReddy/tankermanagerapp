# Tanker Manager

Multi-tenant tanker fleet management product (sell to operators).

## Project layout

| Folder | Description |
|--------|-------------|
| `backend/` | **Spring Boot 3.3 + Java 17** REST API |
| `android/` | **TankerFlow** — Kotlin + Jetpack Compose app (dynamic UI) |

## Backend

See [backend/README.md](backend/README.md).

```bash
cd backend
set JAVA_HOME=C:\Program Files\Java\jdk-17
mvn -s .mvn/settings.xml spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html  
- Super Admin: phone `9999999999` / password `Admin@123`  
- Deploy: `backend/render.yaml` + Docker (Java 17)

## Android (TankerFlow)

See [android/README.md](android/README.md).

Open the `android` folder in **Android Studio**, start the backend, then run on an emulator.

- Owner/Manager: dashboard, book trips, fleet, expenses & salaries  
- Driver: jobs, status updates, live GPS  
- Customer: track by token (no login)
