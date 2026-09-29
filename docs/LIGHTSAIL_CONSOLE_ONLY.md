# What you must click in AWS (I cannot log in for you)

Everything else is automated in GitHub (`deploy/bootstrap-on-server.sh`).  
**~10 minutes** in the browser, then **one SSH command**.

## A. Database (PostgreSQL)

1. [Lightsail](https://lightsail.aws.amazon.com/) → region **Mumbai**
2. **Databases** → **Create database** → **PostgreSQL**
3. Plan: **$15 USD** (smallest managed DB) → name `tankermanager-db`
4. Save **master password** in a safe place
5. When **Available**, open database → copy:
   - Endpoint (hostname)
   - Port `5432`
   - Master username
   - Database name (create `tankermanager` in **Connect** tab if needed)

Build URL (encode `@` in password as `%40`):

```text
postgresql://USER:PASSWORD@ENDPOINT:5432/tankermanager
```

## B. Server (API)

1. **Instances** → **Create** → Ubuntu **22.04**, **$12** plan (2 GB)
2. Name: `tanker-api`
3. **Create static IP** → attach to instance
4. **Networking** → firewall: **22** (your IP), **80**, **443** open
5. **Connect using SSH** → download default key if prompted

## C. Link DB to server

1. Open your **database** → **Connectivity** / **Manage**
2. **Connect to instance** → choose `tanker-api`

## D. One command on the server

SSH (replace IP and key path):

```bash
ssh -i ~/Downloads/LightsailDefaultKey-ap-south-1.pem ubuntu@YOUR_STATIC_IP
```

Then paste (edit `DATABASE_URL`):

```bash
export DATABASE_URL='postgresql://USER:PASS@ls-xxxxx.ap-south-1.cs.amazonlightsail.com:5432/tankermanager'
export JWT_SECRET="$(openssl rand -base64 48)"
git clone https://github.com/VinthaRameshReddy/tankermanagerapp.git /opt/tankermanager
bash /opt/tankermanager/deploy/bootstrap-on-server.sh
```

If Docker was just installed, log out, SSH again, run the same `export` + `bash /tmp/bootstrap.sh` once more.

## E. Test

From your PC:

```bash
curl http://YOUR_STATIC_IP/api/auth/login -H "Content-Type: application/json" -d "{\"phone\":\"9999999999\",\"password\":\"Admin@123\"}"
```

## F. Android app

After API works, tell your developer (or rebuild APK) with:

`android/app/build.gradle.kts` → `BASE_URL` = `http://YOUR_STATIC_IP/` (or `https://api.yourdomain.com/` after SSL).

Full guide: [LIGHTSAIL_POSTGRES_SETUP.md](LIGHTSAIL_POSTGRES_SETUP.md)
