# Running on a shared server

Clarity runs on the same machine as other services (Insulink, Kimbiss)
without touching them. It binds neither port 80 nor 443 of the host: Caddy
terminates TLS on its own ports (8089 for HTTP, 8444 for HTTPS by default)
and fetches its own Let's Encrypt certificate. Clarity has its own compose
project, networks, Postgres and volumes, and hard memory and CPU limits
(about 1.2 GB in total), so it can't starve its neighbours.

## 1. Start Clarity on the server

The server builds nothing. CI publishes `ghcr.io/breuerlukas/clarity-api`
and `ghcr.io/breuerlukas/clarity-panel` on every push to `main`, and the
server pulls them. The checkout lives in `/home/clarity`, owned by the user
`clarity`, who is in the `docker` group.

```bash
git clone https://github.com/breuerlukas/clarity.git /home/clarity && cd /home/clarity
cp .env.example .env    # DB_PASSWORD, PUBLIC_URL=https://<domain>, ENABLE_BANKING_APPLICATION_ID
cat >> .env <<'ENV'
COMPOSE_FILE=docker-compose.yml:docker-compose.server.yml
CLARITY_DOMAIN=clarity.lukasbreuer.de
ENV
# secrets/enable-banking.pem from the Enable Banking control panel
docker compose pull && docker compose up -d
```

With `COMPOSE_FILE` in `.env`, every `docker compose` in this folder uses the
server setup without `-f`. Check first that 8089 and 8444 are free
(`ss -ltn`); otherwise pick others with `CLARITY_HTTP_PORT` and
`CLARITY_HTTPS_PORT`. `PUBLIC_URL` must be the `https://` address: it turns on
secure cookies and is where Enable Banking sends the browser back to, so
register `<PUBLIC_URL>/connect/callback` in the Enable Banking control panel.

## 2. Route the domain at the edge proxy

The edge nginx routes port 80 by Host header and passes port 443 through by
SNI. Clarity needs one entry in each, pointing at its own ports on the
server (here `192.168.178.54`).

In the `http` block, a new server:

```nginx
    # Clarity
    server {
        listen 80;
        listen [::]:80;
        server_name clarity.lukasbreuer.de;

        location / {
            proxy_pass http://192.168.178.54:8089;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
    }
```

In the `stream` map, one line:

```nginx
        clarity.lukasbreuer.de  192.168.178.54:8444;
```

Then `nginx -t && nginx -s reload` on the edge. Without the map line the
`default` would send Clarity traffic to the service behind 443 of that host.

## 3. Certificate

Caddy requests the certificate on its first start, once DNS for the domain
points at the edge and the routes above are live. If it started earlier, it
retries on its own; `docker compose logs caddy` shows the progress.

## 4. Automatic deployment

A merge to `main` reaches the server by itself within about five minutes,
the same way as Insulink and Kimbiss:

1. CI (`.github/workflows/ci.yml`, job `publish`) builds and tests api and
   panel, then pushes both images as `:latest` and `:sha-<commit>`.
2. A systemd timer on the server (`deploy/clarity-update.timer`) runs every
   five minutes: `git pull --ff-only` (so compose and Caddyfile changes
   arrive too), `docker compose pull api panel caddy`,
   `docker compose up -d --wait`, a Caddy reload and a prune of old images.
   Compose only restarts a container whose image or configuration changed.

Pull instead of push: GitHub holds no SSH key and no way into the server; a
compromised workflow can at worst publish a bad image. The publish job runs
on pushes to `main` only, never for a pull request. Postgres is never pulled,
its major version changes only by hand (dump, new volume, restore).

### Set it up once

1. **Make the images public.** After the first CI run on `main`, open both
   packages on GitHub (profile, Packages, `clarity-api` and `clarity-panel`,
   Package settings) and change the visibility to public. A first push
   creates them private, and the server would need `docker login ghcr.io`
   otherwise. They hold no secrets: those come from `.env`, `secrets/` and
   the `api-data` volume.
2. **Moving an existing installation?** Back up first (database dump and the
   `api-data` volume, see the README), then check `docker volume ls`: compose
   now pins the project name `clarity`, so the volumes must be
   `clarity_postgres` and `clarity_api-data`. A folder of another name used to
   give another prefix; if so, change `name:` in `docker-compose.yml` to that
   prefix first, or Postgres starts on an empty volume.
3. **Install the timer:**
   ```bash
   sudo cp deploy/clarity-update.* /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable --now clarity-update.timer
   ```
   The unit runs as user `clarity` in `/home/clarity`. For another user or
   folder, edit `User=` and `WorkingDirectory=` in the copy under
   `/etc/systemd/system/`, then `daemon-reload` again.
4. **Check it:**
   ```bash
   systemctl list-timers clarity-update.timer       # next run
   sudo systemctl start clarity-update.service      # one run right now
   journalctl -u clarity-update.service -n 50       # what it did
   ```

Leave the tracked files on the server alone: a local edit makes
`git pull --ff-only` fail, and the run stops before touching any container
(`git status` shows the culprit, `git checkout -- <file>` drops it). Server
settings belong in `.env` and `secrets/`.

To roll back, revert the commit on `main`; the timer deploys that. In a
hurry: `sudo systemctl stop clarity-update.timer`, then
`docker tag ghcr.io/breuerlukas/clarity-api:sha-<commit> ghcr.io/breuerlukas/clarity-api:latest`
and `docker compose up -d`; start the timer again once `main` is fixed.

## Security on the open internet

Clarity holds bank data, so a public address deserves more care than other
services:

- Every page and request needs the owner session with password and TOTP;
  sessions are HttpOnly, Secure, SameSite=Strict cookies.
- Caddy sends HSTS, so the browser never tries plain HTTP again.
- The edge passes 443 through by SNI, so Caddy and Clarity see the edge
  proxy's address, not the visitor's. The login throttle and the session
  list therefore show one address for everyone. If that matters, enable the
  PROXY protocol between edge and Caddy.
- To be reachable only from home or a VPN, add a matcher to the Caddyfile,
  e.g. `@outside not remote_ip 192.168.178.0/24 10.8.0.0/24` with
  `respond @outside 403`, and leave the domain out of public DNS if possible.

## Memory

| Container | Limit |
|---|---|
| postgres | 256 MB |
| api | 768 MB (heap 60 %, the rest for Tesseract) |
| panel | 64 MB |
| caddy | 128 MB |

Everyday use stays well below that; reading a scanned invoice is the peak.