# Clarity

Your bank accounts in one calm place, only for you.

Clarity shows what is on all your accounts, where your money went this month
and which fixed costs are still coming. It runs on your own computer or
server, reads your accounts through the open banking interfaces every EU bank
has to offer, and never shows anything to anyone but you.

- **One balance** across all connected accounts (banks and PayPal)
- **What's left** this month once the remaining fixed costs are paid
- **Where the money went**, by category, with automatic categorization and
  rules you teach it with one tap
- **Fixed costs** found on their own: subscriptions, rent, contracts,
  insurance, monthly or yearly
- Works on the phone like an app, light and dark

**Try it first:** the public demo runs entirely in your browser with made-up
sample data, no bank and no server involved. It is published to GitHub Pages
on every push to `main` (enable it once under *Settings → Pages → Source:
GitHub Actions*). To run it locally:
`cd panel && VITE_DEMO=true npm run dev`.

## How your data stays yours

- **Read-only access.** Banks hand out read-only consent (PSD2). Clarity cannot
  move money, and neither can anyone who gets into it.
- **Your password never reaches Clarity.** You sign in at your bank directly.
- **Exactly one owner.** The account is created with a one-time code that only
  appears in the server log, and every login needs password **and** a code
  from an authenticator app.
- **Hardened sessions.** HttpOnly, SameSite=Strict cookies; only a hash of the
  session token is stored; sessions end after 7 idle days or 30 days at most.
- **Brute force is pointless.** Five failed attempts lock an address out for
  15 minutes; authenticator codes cannot be replayed.
- **No third parties in the page.** No CDN, no web fonts from elsewhere, no
  analytics; a strict Content Security Policy enforces it.
- **Closed by default.** The panel only listens on `127.0.0.1`, the api is
  never published, and the database sits on a network without internet
  access.

See [SECURITY.md](SECURITY.md) for how to report a vulnerability.

## Setup

You need Docker with Compose and a free
[Enable Banking](https://enablebanking.com) account.

**1. Get the code**

```bash
git clone https://github.com/<you>/clarity.git
cd clarity
cp .env.example .env
```

**2. Set up Enable Banking** as described in
[docs/enable-banking.md](docs/enable-banking.md). Put the key file at
`secrets/enable-banking.pem` and the application id into `.env`.

**3. Fill in `.env`**

```bash
DB_PASSWORD=$(openssl rand -hex 24)   # paste the output
PUBLIC_URL=http://localhost:8080
ENABLE_BANKING_APPLICATION_ID=<your application id>
```

**4. Start**

```bash
docker compose up -d --build
docker compose logs api | grep "setup code"
```

**5. Open <http://localhost:8080>**, enter the setup code, choose a name and
password, scan the QR code with your authenticator app, then connect your
first bank under **Accounts**.

## Using it on your phone

Don't open the port to the internet. Use [Tailscale](https://tailscale.com)
(free for personal use) instead: it gives the machine a private HTTPS address
that only your own devices can reach.

```bash
sudo tailscale serve --bg 8080
```

Set `PUBLIC_URL` to the `https://<machine>.<tailnet>.ts.net` address it prints,
add `https://<machine>.<tailnet>.ts.net/connect/callback` as a redirect URL in
Enable Banking, and restart with `docker compose up -d`. On the phone, open
the address and choose *Add to Home Screen*.

## Everyday operation

| Task | Command |
|---|---|
| Update | `git pull && docker compose up -d --build` |
| Back up | `docker compose exec postgres pg_dump -U clarity clarity > clarity.sql` |
| Restore | `docker compose exec -T postgres psql -U clarity clarity < clarity.sql` |
| Logs | `docker compose logs -f api` |

Backups contain all your transactions. Keep them as safe as your bank
statements, ideally encrypted.

Clarity syncs every account every six hours on its own; *Update now* on the
accounts page syncs right away.

## Development

| Part | Stack | Run |
|---|---|---|
| `api/` | Java 25, Spring Boot, PostgreSQL | `./gradlew bootRun` (reads `api/config.ini`) |
| `panel/` | React, TypeScript, Tailwind, shadcn/ui | `npm install && npm run dev` |

For a local database:
`docker run -d -p 127.0.0.1:5432:5432 -e POSTGRES_USER=clarity -e POSTGRES_PASSWORD=clarity -e POSTGRES_DB=clarity postgres:18`.
The panel's dev server proxies `/v1` to `localhost:8080`.

Tests: `./gradlew test` in `api/`, `npm test` in `panel/`.

## License

[GNU Affero General Public License v3.0](LICENSE.md)