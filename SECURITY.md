# Security Policy

Clarity handles bank data, so security reports get priority.

## Reporting a vulnerability

Please **do not open a public issue**. Use GitHub's
["Report a vulnerability"](../../security/advisories/new) feature on this
repository instead, and include:

- Steps to reproduce
- The impact you expect
- A suggested fix, if you have one

You can expect an acknowledgment within 48 hours and a plan within five
business days. Fixes are disclosed once a release is out.

## How Clarity is checked

Every commit is linted. Every pull request is analyzed with CodeQL and
SpotBugs/FindSecBugs, checked for vulnerable dependencies and committed
secrets, and tested to keep every endpoint but setup and login closed without
a session. High and Critical findings block the merge. See
`.github/workflows/` for the details, and `.github/security/smoke_test.py` to
probe a running instance from the outside.

## Running Clarity safely

- Keep the panel bound to `127.0.0.1` and reach it through a private network
  such as a VPN. Never forward its port on your router.
- Keep `.env`, `secrets/` and database backups private; they are as sensitive
  as your bank statements.
- Use full-disk encryption on the machine Clarity runs on. The database
  itself is not encrypted at rest, except mailbox passwords, which are
  encrypted with the key in the `api-data` volume.
- Connect a mailbox with an app password or a user that can only read mail
  where your provider offers one. Clarity never changes a mailbox, but the
  password would allow it.
- Update regularly: `git pull && docker compose up -d --build`.