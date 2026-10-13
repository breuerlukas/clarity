# Clarity

A self-hosted overview of your bank accounts. Clarity reads your accounts
through the open banking interface (PSD2) every bank in the EU has to provide,
sorts the transactions into categories, finds subscriptions and other fixed
costs on its own, and tells you how much you can still spend until your next
salary, once everything that is due before it is paid.

Money is kept in **spaces**, switched and created from the Clarity logo at the
top of the sidebar. Clarity starts with one personal space; more can be
added, each with its own name, icon and banks, such as a shared household. A
business space adds customer contracts with their renewals and cancellation
deadlines, MRR, ARR, active customers and churn derived from them, and
matches incoming payments on its accounts to the contracts; transfers to and
from your personal accounts count as private withdrawals or deposits.

React and Tailwind in the panel, Spring Boot on Java 25 and Postgres behind it.
Bank access goes through [Enable Banking](https://enablebanking.com).

|      | Build Status                                                                                                                                                         | Demo                                                                                                                                                                      |
|------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| main | [![CI](https://github.com/breuerlukas/clarity/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/breuerlukas/clarity/actions/workflows/ci.yml) | [![Demo on GitHub Pages](https://github.com/breuerlukas/clarity/actions/workflows/pages.yml/badge.svg?branch=main)](https://github.com/breuerlukas/clarity/actions/workflows/pages.yml) |

There is a [demo](https://breuerlukas.github.io/clarity/) with made-up data. It
runs entirely in the browser, without a server or a bank behind it. Locally:
`cd panel && npm install && npm run demo`.

## Security

Clarity only ever gets read access to your accounts. You sign in at your bank
yourself, so your banking password never reaches Clarity, and the consent the
bank hands out can't move money.

It is built for exactly one person. The account is created with a one-time
code that only shows up in the server log, and every sign-in needs the
password and a code from an authenticator app. Sessions sit in HttpOnly
cookies, only their hash is stored, and repeated failed sign-ins lock the
address out for a while. The panel loads nothing from third parties, and out
of the box it only listens on `127.0.0.1`; the api and the database are never
published.

Found a problem? See [SECURITY.md](SECURITY.md).

## Setup

You need Docker with Compose and a free
[Enable Banking](https://enablebanking.com) account.

**1. Get the code**

```bash
git clone https://github.com/breuerlukas/clarity.git
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

The panel only listens on `127.0.0.1`, so it is not reachable from other
devices by default. Don't forward the port on your router. Reach it through a
VPN into your home network, or put a reverse proxy with HTTPS in front of it
that only you can get to.

Enable Banking needs an `https` address to send you back to after the bank
login. Set `PUBLIC_URL` to the address you open Clarity at, register
`<PUBLIC_URL>/connect/callback` as a redirect URL in Enable Banking, and
restart with `docker compose up -d`. On the phone, open the address and choose
*Add to Home Screen*.

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

Checks: `./gradlew build` in `api/` compiles, lints (Checkstyle), looks for
bugs and security patterns (SpotBugs with FindSecBugs) and runs the tests;
`npm run lint && npm test` does the same for the panel. The panel compiles
with TypeScript 7 (`@typescript/native`), while `typescript` points to the
TypeScript 6 compatibility package, because typescript-eslint needs its api.

Against a running instance, `python3 .github/security/smoke_test.py
http://127.0.0.1:8080` checks from the outside that nothing but setup and
login answers without a session, that the security headers are in place and
that wrong logins lock the address out. It does lock out your own address for
15 minutes, so run it against a throwaway instance.

On every pull request CI also runs CodeQL, a dependency review, a secret scan
(gitleaks), Trivy on both images and their configuration, the check above
against a freshly started stack and an OWASP ZAP baseline scan. Findings of
High or Critical severity fail the build; everything else lands in the
repository's Security tab.

The demo is published by `.github/workflows/pages.yml` on every push to
`main`. In a fork, set *Settings → Pages → Source* to *GitHub Actions* once.

## Contributing

Contributions are welcome. For larger changes please open an issue first (see
[CONTRIBUTING.md](CONTRIBUTING.md)). Everyone taking part is expected to follow
the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

[AGPL-3.0](LICENSE.md)