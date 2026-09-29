# Step-by-step: Tanker Manager on AWS Lightsail + PostgreSQL

Deploy the **Spring Boot API** (TankerFlow Android connects to it) in **Mumbai** with **managed PostgreSQL**.  
Estimated cost: about **₹1,000–₹1,500/month** to start (2 GB instance + small DB plan).

**Time:** ~1–2 hours first time.

---

## Before you start

| Item | You need |
|------|----------|
| AWS account | [aws.amazon.com](https://aws.amazon.com) |
| Domain (optional but recommended) | e.g. `api.yourcompany.com` — for HTTPS |
| GitHub repo | `VinthaRameshReddy/tankermanagerapp` |
| Local tools | Git, SSH client (PuTTY or Windows OpenSSH) |

**Region:** **Asia Pacific (Mumbai) `ap-south-1`** — lowest latency for India.

---

## Part 1 — Create PostgreSQL database

1. Open **[Lightsail console](https://lightsail.aws.amazon.com/)** → sign in.
2. Top-right: set region to **Mumbai**.
3. Left menu → **Databases** → **Create database**.
4. Choose:
   - **Engine:** PostgreSQL (latest stable, e.g. 16)
   - **Plan:** smallest that fits (e.g. **$15/mo** tier) — upgrade later if needed
   - **Database name:** `tankermanager` (or note the default `postgres` and create DB later)
   - **Master username / password:** choose a **strong password** → save in a password manager
5. Click **Create database** — wait until status is **Available** (~5–10 min).
6. Open the database → **Connectivity** tab:
   - Note **Endpoint** (host), **Port** (usually `5432`), **User name**, **Database name**.
7. **Link to your app later (Part 2):** under **Connected resources** (or **Manage**), you will **attach your Lightsail instance** so the API can reach the DB on the private network (recommended).  
   - If you must connect over the public endpoint first for testing, enable only temporarily and restrict by IP.

**Build `DATABASE_URL` for the app** (special characters in password must be URL-encoded):

```text
postgresql://USERNAME:PASSWORD@ENDPOINT:5432/tankermanager
```

Example:

```text
postgresql://dbmasteruser:MyP%40ss@ls-abc123.ap-south-1.rds.amazonaws.com:5432/tankermanager
```

The Spring Boot app (`ProdDataSourceConfig`) reads `DATABASE_URL` and uses JDBC with SSL.

---

## Part 2 — Create the application server (Lightsail instance)

1. Lightsail → **Instances** → **Create instance**.
2. **Platform:** Linux/Unix  
3. **Blueprint:** OS Only → **Ubuntu 22.04 LTS**
4. **Plan:** **$12/mo** (2 GB RAM, 1 vCPU) or next size if budget allows
5. **Name:** `tanker-api`
6. **Create instance** — wait until **Running**.
7. Instance → **Networking**:
   - Attach a **Static IP** (create if needed) — note the IP for DNS.
8. **Firewall** (instance → Networking → IPv4 firewall):
   - **SSH (22)** — your IP only (recommended)
   - **HTTP (80)** — anywhere
   - **HTTPS (443)** — anywhere
9. **Connect database to instance:**
   - Database → **Connectivity** → connect / authorize **`tanker-api`** instance (Lightsail pairs them in-region).

---

## Part 3 — DNS (HTTPS)

If you have a domain:

1. At your DNS provider, add an **A record**:
   - Name: `api` (or `tanker-api`)
   - Value: **static IP** from Part 2
2. Wait 5–30 minutes for DNS to propagate.
3. You will use this hostname in Nginx and Let’s Encrypt (e.g. `api.yourcompany.com`).

If you **skip DNS** for now, you can test with `http://STATIC_IP` only (no HTTPS on phone without extra steps).

---

## Part 4 — Install Docker on the instance

1. Instance → **Connect** → **SSH** (browser) or use your SSH key:

```bash
ssh -i your-key.pem ubuntu@YOUR_STATIC_IP
```

2. Run:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker ubuntu
```

3. Log out and SSH in again so `docker` works without sudo.

---

## Part 5 — Deploy the API

1. On the server:

```bash
sudo mkdir -p /opt/tankermanager
sudo chown ubuntu:ubuntu /opt/tankermanager
cd /opt/tankermanager
git clone https://github.com/VinthaRameshReddy/tankermanagerapp.git .
```

2. Create environment file:

```bash
cd /opt/tankermanager/deploy
cp .env.example .env
nano .env
```

3. Set values in `.env`:

```env
DATABASE_URL=postgresql://USER:PASSWORD@ENDPOINT:5432/tankermanager
JWT_SECRET=at-least-32-random-characters-here-change-me
```

Generate JWT secret (on server):

```bash
openssl rand -base64 48
```

4. Edit Nginx host name:

```bash
nano /opt/tankermanager/deploy/nginx/api.conf
```

Replace `YOUR_DOMAIN` with `api.yourcompany.com` (or comment out HTTPS block and use HTTP-only for a quick test).

5. Build and start:

```bash
cd /opt/tankermanager/deploy
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f api
```

6. **Smoke test** (from your PC):

```bash
curl -s http://YOUR_STATIC_IP/api/auth/login -H "Content-Type: application/json" -d "{\"phone\":\"9999999999\",\"password\":\"Admin@123\"}"
```

You should get JSON with a token (if DB is empty, super-admin is created on first startup — see `application.yml` / seed logic).

**Swagger (optional):** `http://YOUR_IP/swagger-ui.html` — if exposed by security config.

---

## Part 6 — HTTPS with Let’s Encrypt (production)

On the server (with DNS pointing to static IP):

```bash
sudo apt-get install -y certbot
# Stop nginx container briefly if certbot needs port 80
cd /opt/tankermanager/deploy
docker compose -f docker-compose.prod.yml stop nginx

sudo certbot certonly --standalone -d api.yourcompany.com

# Update deploy/nginx/api.conf: uncomment ssl_certificate lines and set paths
# Mount certs in docker-compose.prod.yml under nginx volumes:
#   - /etc/letsencrypt:/etc/letsencrypt:ro

docker compose -f docker-compose.prod.yml up -d
```

Renewal: `sudo certbot renew` (add cron).

---

## Part 7 — Migrate data from Render (if you already have live data)

1. On Render: copy **External Database URL** and export:

```bash
pg_dump "postgresql://..." --no-owner --no-acl -f tanker_backup.sql
```

2. Import into Lightsail Postgres (from a machine that can reach the DB):

```bash
psql "postgresql://USER:PASS@LIGHTSAIL_ENDPOINT:5432/tankermanager" -f tanker_backup.sql
```

3. Redeploy API (Part 5) and verify customers/trips in the app.

---

## Part 8 — Point the Android app to Lightsail

1. On your PC, edit `android/app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", "\"https://api.yourcompany.com/\"")
```

2. Rebuild APK:

```bash
cd android
# JDK 17
.\gradlew :app:assembleDebug
```

3. Install the new APK on phones. All users must use this build (old APK still calls Render).

---

## Part 9 — Backups and monitoring

| Task | How |
|------|-----|
| DB backup | Lightsail database → **Snapshots** → enable automatic |
| Extra backup | Weekly `pg_dump` to S3 (optional, small cost) |
| Logs | `docker compose logs -f api` |
| Restart after reboot | `restart: unless-stopped` in compose (already set) |

---

## Part 10 — GitHub Actions (optional auto-deploy)

1. GitHub repo → **Settings → Secrets**:
   - `LIGHTSAIL_HOST` — static IP or hostname
   - `LIGHTSAIL_USER` — `ubuntu`
   - `LIGHTSAIL_SSH_KEY` — private key PEM
2. On server: ensure `/opt/tankermanager` is a git repo and `.env` exists.
3. Push to `main` → workflow `.github/workflows/deploy-lightsail.yml` can rebuild API (configure as needed).

---

## Troubleshooting

| Problem | What to try |
|---------|-------------|
| API exits on start | `docker compose logs api` — usually **DB connection** |
| `DATABASE_URL` / SSL errors | Use full URL; ensure instance is **connected** to DB in Lightsail; try appending `?sslmode=require` |
| 502 from Nginx | API not up — `docker compose ps`; wait 60s after first start |
| Login works on Render but not Lightsail | Empty DB — migrate dump or use default super-admin on fresh DB |
| Android “no connection” | Wrong `BASE_URL`, HTTP blocked on device, or firewall |

---

## Checklist (print this)

- [ ] PostgreSQL database **Available** in Mumbai
- [ ] Instance **Running** with **static IP**
- [ ] Database **connected** to instance in Lightsail
- [ ] `.env` with `DATABASE_URL` + `JWT_SECRET`
- [ ] `docker compose up` healthy
- [ ] Login API returns token
- [ ] DNS + HTTPS (production)
- [ ] New APK with `BASE_URL` → Lightsail
- [ ] Render decommissioned after cutover

---

## Quick reference (files in repo)

| File | Purpose |
|------|---------|
| `backend/Dockerfile` | API image |
| `deploy/docker-compose.prod.yml` | API + Nginx |
| `deploy/nginx/api.conf` | Reverse proxy |
| `deploy/.env.example` | Env template |
| `docs/PRODUCTION_LOW_COST.md` | Architecture overview |
