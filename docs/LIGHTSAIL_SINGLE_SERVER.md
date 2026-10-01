# Lightsail: one server + PostgreSQL on the same VM

**No separate $15 Lightsail database.** Everything on one Ubuntu instance (~**$12/month** plan).

```text
Internet → Nginx (:80/443) → Spring Boot (:8080) → PostgreSQL (localhost:5432 only)
```

## AWS setup (browser only)

1. Region: **Mumbai (ap-south-1)**
2. **Instances** → Create **Ubuntu 24.04** (or 22.04), **$12** plan (2 GB RAM recommended; if you use 1 GB, swap from Stage 1 is required)
3. **Static IP** → attach
4. Firewall: **22, 80, 443** — **do not** open **5432**
5. **Skip** Lightsail **Databases** ($15 plan) — delete/cancel if you already created one

## Cursor Remote-SSH

1. `ssh ubuntu@YOUR_STATIC_IP` with Lightsail **.pem**
2. See **[CURSOR_SSH_STAGE1.md](CURSOR_SSH_STAGE1.md)** — run `stage1-server-prep.sh`
3. Stage 2 — database on same machine:

```bash
cd ~/tankermanagerapp/deploy
export TANKER_DB_PASSWORD='choose-a-strong-password'
bash stage2-create-local-db.sh
```

4. Stage 3 — one-time app layout + Nginx + systemd (creates `/opt/tankermanager/deploy/.env`):

```bash
cd ~/tankermanagerapp/deploy
export TANKER_DB_PASSWORD='same-as-stage-2'
bash stage3-app-setup.sh
```

## GitHub auto-deploy (every push to `main`)

After Stage 3, add **repository secrets** (GitHub → **Settings → Secrets and variables → Actions**):

| Secret | Value |
|--------|--------|
| `LIGHTSAIL_HOST` | Static IP, e.g. `13.203.91.129` |
| `LIGHTSAIL_USER` | `ubuntu` |
| `LIGHTSAIL_SSH_KEY` | Entire `.pem` file (private key text) |

Workflow: [`.github/workflows/deploy-lightsail.yml`](../.github/workflows/deploy-lightsail.yml)

- **Push to `main`** → deploys API **and** builds a **debug APK** (Actions → workflow run → **Artifacts** → `tankerflow-debug-apk`)
- APK `BASE_URL` = `http://LIGHTSAIL_HOST/` unless you set optional secret `API_PUBLIC_BASE_URL` (e.g. `https://api.yourdomain.com/`)
- Local builds: set `API_BASE_URL` in `android/gradle.properties`
- DB password and `JWT_SECRET` stay **only** on the server in `/opt/tankermanager/deploy/.env` (never in GitHub)

## Migrate from Render (optional)

From a machine that can reach both DBs:

```bash
pg_dump "RENDER_EXTERNAL_URL" --no-owner --no-acl -f backup.sql
psql "postgresql://tanker:PASS@YOUR_SERVER_IP:5432/tankermanager" -f backup.sql
```

For import, SSH tunnel or run `psql` **on the server** as `ubuntu` using `localhost`.

## Android app

`BASE_URL` = `http://YOUR_STATIC_IP/` (or `https://api.yourdomain.com/` after SSL).

## Cost

| Item | Cost |
|------|------|
| Lightsail instance | ~$12/mo |
| Managed DB | **$0** (not used) |
| **Total** | ~**$12/mo** (+ domain optional) |
