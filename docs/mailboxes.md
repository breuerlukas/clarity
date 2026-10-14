# Connecting a mailbox

A business space reads its invoices from an IMAP mailbox. Clarity only
reads: the folders are opened read-only (IMAP `EXAMINE`), so no mail is
marked as seen, moved or deleted.

## 1. Get an app password

Most providers do not accept the normal account password over IMAP once two
factor sign-in is on. Create an app password and use that one:

| Provider | Server | Port | Security |
|---|---|---|---|
| Gmail / Google Workspace | `imap.gmail.com` | 993 | SSL |
| IONOS | `imap.ionos.de` | 993 | SSL |
| Strato | `imap.strato.de` | 993 | SSL |

Gmail needs IMAP to be switched on in its settings first. Outlook.com and
Microsoft 365 only allow IMAP sign-in through OAuth, which Clarity does not
support; forward the invoices to another mailbox instead.

Only encrypted connections are offered: SSL (usually port 993) or STARTTLS
(usually port 143). The server certificate and host name are always checked.

## 2. Add it in Clarity

Open the invoices page of the business space and add the mailbox. After the
login works, Clarity lists the folders to pick from:

- **Incoming**: where bills and payment advices from others arrive, usually
  `INBOX`.
- **Outgoing** (optional): the folder with the invoices you sent, usually
  `Sent`, `Gesendet` or `[Gmail]/Gesendet`.

The password is stored encrypted with the key in `secrets.key_file`
(`/app/data/clarity.key` in Docker). It is never sent back to the panel.

## 3. What is read

The first check goes back 90 days. After that Clarity remembers the last mail
per folder and only reads newer ones, at most 100 per folder and run, so a
full mailbox is worked through over several runs. If the server renumbers a
folder (its `UIDVALIDITY` changes), the folder is read again from 90 days
back; invoices already stored are recognised by their file and not added
twice.

The sent folder is read before the incoming one, so a payment advice finds
the invoice it announces even when both arrive in the same run.

Mails are checked every 15 minutes; *Check now* reads them right away. From
each mail Clarity keeps PDFs, XML files and attached (not inline) images of
at most 10 MB, at most 20 per mail, including those inside forwarded mails.
See [invoice-recognition.md](invoice-recognition.md) for how they are read.

## When a check fails

The mailbox card shows why the last check failed:

| Shown | Meaning |
|---|---|
| Login | The server refused the user name or password |
| Folder | A chosen folder no longer exists |
| Password | The stored password cannot be decrypted, usually because the key file changed; enter it again |
| Connection | The server could not be reached or the TLS check failed |