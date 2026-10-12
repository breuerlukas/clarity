# Setting up Enable Banking

Clarity reads your accounts through [Enable Banking](https://enablebanking.com),
a licensed account information service (PSD2). For accounts you link yourself,
it is free. The labels in their control panel may differ slightly from the
ones below.

## 1. Create an account

Open <https://enablebanking.com/cp> and sign in with your email address. You
get a login link by mail; there is no password.

## 2. Register an application

Under **Applications**, register a new application:

| Field | Value |
|---|---|
| Environment | **Production** (not Sandbox) |
| Name | `Clarity` |
| Redirect URL | The address you open Clarity at, plus `/connect/callback` |

Redirect URL examples:

- Only on this computer: `http://localhost:8080/connect/callback`
- Behind your own HTTPS address: `https://clarity.example.com/connect/callback`

If privacy policy, terms or description are required, a link to your copy of
this repository is enough for personal use.

For the key, let the control panel **generate it in the browser**. It downloads
a file named `<application-id>.pem`. It is offered **only once**: keep it.

## 3. Link your own accounts

A new application runs in the free **restricted** mode: it may only read
accounts you link in the control panel yourself. Open the application, choose
**Link accounts** and link every bank you want in Clarity (for example
Openbank, N26, PayPal). Without this step Clarity finds no banks.

Afterwards the application shows as *Restricted* and *Active*. That is correct.

## 4. Hand the key to Clarity

```bash
cp ~/Downloads/<application-id>.pem secrets/enable-banking.pem
chmod 600 secrets/enable-banking.pem
```

In `.env`:

```
ENABLE_BANKING_APPLICATION_ID=<application-id>
PUBLIC_URL=http://localhost:8080
```

`PUBLIC_URL` must match the redirect URL from step 2 without
`/connect/callback`. Restart with `docker compose up -d`.

## 5. Connect in Clarity

Open **Accounts → Connect a bank**, pick country and bank, sign in at the bank.
You land back in Clarity and the first sync starts right away.

Banks grant access for up to 180 days. Clarity warns two weeks before it ends;
reconnecting the bank renews it.

## Testing locally without HTTPS

Enable Banking only accepts `https` redirect URLs, but the URL does not have
to be reachable: the bank only sends your browser there. To try Clarity on
`http://localhost:8080` before setting up HTTPS:

1. Register any `https` URL you own as redirect URL, for example
   `https://clarity.example.com/connect/callback`.
2. Put exactly that URL into `.env` as `BANKING_REDIRECT_URL` and run
   `docker compose up -d --build`.
3. Connect the bank in Clarity as usual. After the bank login your browser
   lands on that URL, which may not load. That is fine.
4. In the address bar, replace everything before `?code=` with
   `http://localhost:8080/connect/callback` and open it within 30 minutes.

The code in that address is useless to anyone else: exchanging it needs your
private key and the one-time state only your Clarity instance knows.

## Good to know

- **Four reads a day:** PSD2 allows banks to refuse more than four unattended
  reads per account and day. Clarity syncs every six hours on its own. When
  you press *Update now*, it tells the bank you are present, which does not
  count against that limit.
- **Old key format:** If your key starts with `BEGIN RSA PRIVATE KEY`, convert
  it once:
  `openssl pkcs8 -topk8 -nocrypt -in old.pem -out secrets/enable-banking.pem`.
- **Revoking access:** Removing a bank in Clarity revokes the session at
  Enable Banking and deletes its transactions. You can also revoke access in
  your banking app at any time.