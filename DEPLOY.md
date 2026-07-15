# Deploy + real-phone testing

## GitHub repo
https://github.com/VinthaRameshReddy/tankermanagerapp

## Push (login as VinthaRameshReddy — not venkat1736)

```bat
cd "C:\Users\RameshReddy\OneDrive - Snapwork Technologies Private Limited\Documents\Desktop\Tankermnager"
git remote add origin https://github.com/VinthaRameshReddy/tankermanagerapp.git
git branch -M main
git push -u origin main
```

If you get `Permission denied to venkat1736`:
1. Windows Credentials → remove old `github.com` entries for venkat1736  
2. Push again and sign in as **VinthaRameshReddy**  
   Or use GitHub Desktop → Add local repo → Publish / Push

## SMS recommendation (India)

| Provider | Verdict |
|----------|---------|
| **Fast2SMS** | **Best for this app** — cheap, easy API key, good for trip alerts |
| MSG91 | Good if you already have DLT sender ID |
| Twilio | Costlier in India; better for international |

Suggested Render env:
- `SMS_ENABLED=true`
- `SMS_PROVIDER=fast2sms`
- `SMS_API_KEY=<your key>`

## Render DB (free tier)

Render allows **only one free Postgres**. This project reuses your existing **buffalo_db**
(from openplot-api). Do **not** create `tanker-manager-db`.

Copy from openplot-api → Environment into tanker-manager-api:

| Key | From openplot |
|-----|----------------|
| `DATABASE_HOST` | same |
| `DATABASE_PORT` | `5432` |
| `DATABASE_NAME` | same (`buffalo_db`) |
| `DATABASE_USER` | same |
| `DATABASE_PASSWORD` | same |

Tanker tables (`operators`, `trips`, …) are created automatically via JPA (`ddl-auto=update`).
Keep them in the same DB as openplot — different table names, usually fine.

## Routing / ETA — OpenRouteService (free, no Google billing)

1. Sign up: https://openrouteservice.org/dev/#/signup  
2. Dashboard → copy **API key**  
3. On Render → Environment set:
   - `MAPS_PROVIDER=openrouteservice`
   - `ORS_API_KEY=<your key>`
4. Redeploy  

Without the key, the API still works using distance-based ETA.  
Driver GPS tracking does **not** need this key.

## SMS recommendation (India)
1. https://dashboard.render.com → New → Blueprint  
2. Connect `VinthaRameshReddy/tankermanagerapp` (uses root `render.yaml`)  
3. After deploy, set:
   - `APP_PUBLIC_BASE_URL=https://YOUR-SERVICE.onrender.com`
   - `GOOGLE_MAPS_API_KEY=...`
   - `SMS_ENABLED=true` + Fast2SMS key when ready  
4. Health check: `https://YOUR-SERVICE.onrender.com/api/public/health`

## Android real phone
In `android/app/build.gradle.kts` set:

```kotlin
buildConfigField("String", "BASE_URL", "\"https://YOUR-SERVICE.onrender.com/\"")
```

Then rebuild APK and install on phone.
