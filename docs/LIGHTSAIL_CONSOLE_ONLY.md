# AWS clicks — single Lightsail server (PostgreSQL on same VM)

**Do not** use the **$15 managed Lightsail database.** Cancel/delete it if you created one.

Full guide: **[LIGHTSAIL_SINGLE_SERVER.md](LIGHTSAIL_SINGLE_SERVER.md)**

## A. Instance only

1. [Lightsail](https://lightsail.aws.amazon.com/) → **Mumbai**
2. **Instances** → **Create** → Ubuntu **22.04/24.04**, **$12** plan → `tanker-api`
3. **Static IP** → attach
4. Firewall: **22, 80, 443** — **not 5432**

## B. SSH — Stage 1 + 2

Connect with Cursor Remote-SSH or Lightsail **Connect using SSH**, then:

```bash
git clone https://github.com/VinthaRameshReddy/tankermanagerapp.git
cd tankermanagerapp/deploy
bash stage1-server-prep.sh
export TANKER_DB_PASSWORD='your-strong-password'
bash stage2-create-local-db.sh
export TANKER_DB_PASSWORD='your-strong-password'
bash stage3-app-setup.sh
```

Add GitHub secrets `LIGHTSAIL_HOST`, `LIGHTSAIL_USER`, `LIGHTSAIL_SSH_KEY` — then every **push to `main`** deploys the API.

App uses (written by stage 3):

```text
DATABASE_URL=postgresql://tanker:PASSWORD@localhost:5432/tankermanager
DATABASE_SSL_MODE=disable
```

## C. Test (after Stage 3 — API deployed)

```bash
curl http://YOUR_STATIC_IP/api/auth/login -H "Content-Type: application/json" -d "{\"phone\":\"9999999999\",\"password\":\"Admin@123\"}"
```

## D. Android

`BASE_URL` = `http://YOUR_STATIC_IP/` (or HTTPS domain later).
