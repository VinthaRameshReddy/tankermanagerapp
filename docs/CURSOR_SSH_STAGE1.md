# Stage 1: Cursor Remote-SSH → Lightsail (no app, no public DB)

Server IP example: `13.203.91.129` (use yours).

## Connect Cursor to the server

1. **Ctrl+Shift+P** → `Remote-SSH: Connect to Host`
2. **Add New SSH Host:** `ssh ubuntu@YOUR_STATIC_IP`
3. Select your **Lightsail `.pem`** key when prompted.
4. OS: **Linux**
5. **Terminal → New Terminal** — prompt should look like: `ubuntu@ip-...:~$`

## Step A — Verify (read-only)

```bash
bash -c "$(curl -fsSL https://raw.githubusercontent.com/VinthaRameshReddy/tankermanagerapp/main/deploy/stage1-verify.sh)"
```

Or clone the repo and run:

```bash
git clone https://github.com/VinthaRameshReddy/tankermanagerapp.git
cd tankermanagerapp/deploy
bash stage1-verify.sh
```

Expected: user `ubuntu`, Ubuntu **24.04** (or 22.04).

## Step B — Server prep (Stage 1 only)

**Does:** updates, 2 GB swap, Java 17, PostgreSQL, Nginx, UFW (22/80/443 — **not** 5432).

**Does not:** create DB/users, deploy app, Docker, Node, open PostgreSQL to internet.

```bash
cd ~/tankermanagerapp/deploy   # or clone first
bash stage1-server-prep.sh
```

## Step C — Paste verification output to chat

```bash
bash stage1-verify-output.sh
```

Copy the full output for **Stage 2**, then run **Stage 3** (`stage3-app-setup.sh`) and add GitHub secrets — see [LIGHTSAIL_SINGLE_SERVER.md](LIGHTSAIL_SINGLE_SERVER.md).

## Do NOT approve in Stage 1

- Docker / docker-compose (optional later; repo has it — skip for native install path)
- Opening **5432** on UFW or Lightsail firewall
- Extra Lightsail DB / RDS / second EC2
- AWS resource creation from the server

## Architecture after Stage 1

```text
Lightsail VM (Ubuntu)
  ├── Java 17
  ├── PostgreSQL (localhost only)
  ├── Nginx
  └── 2 GB swap
```

Stage 2 (same server, no $15 Lightsail DB): `bash stage2-create-local-db.sh` — see [LIGHTSAIL_SINGLE_SERVER.md](LIGHTSAIL_SINGLE_SERVER.md).

Stage 3: deploy Spring Boot JAR + Nginx + migrate from Render if needed.
