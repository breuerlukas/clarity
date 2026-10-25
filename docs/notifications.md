# Notifications

Clarity mails the owner about what happens while the panel is closed. It
sends through the owner's own SMTP server (Jakarta Mail, SSL/TLS or
STARTTLS, certificate checked); no push service or other third party is
involved. The settings live in one `notification_settings` row, the SMTP
password encrypted with the `SecretBox` like mailbox passwords.

## Events

`NotificationDispatch` runs every hour. `NotificationSources` gathers the
connections, accounts and the fixed costs of every space, and
`NotificationEvents` picks what is worth a mail. Only recent events count,
so switching notifications on doesn't send the whole past:

| Kind | When | Switch |
|---|---|---|
| `CONSENT_EXPIRING` | a bank consent ends within 7 days | bank access |
| `CONSENT_EXPIRED` | a consent ended within the last 7 days | bank access |
| `SYNC_FAILING` | an account of a valid consent failed for over a day | failing updates |
| `PRICE_CHANGED` | a fixed cost booked a new amount in the last 7 days | price changes |

## Sent once

Every notification has an event key naming its occurrence, such as
`CONSENT_EXPIRING:<connection>:<valid_until>`. Sent keys are stored in
`sent_notification`, and the dispatch skips them. A renewed consent has a new
expiry and so a new key; a failing account is keyed by its last successful
sync, so a new failure streak is announced again. A mail that fails to send
is not stored and goes out on the next run.

## Texts

The api returns no user-facing text to the panel, but a mail has to say
something. Subjects and texts per language are in
`api/src/main/resources/notification-texts.json`; the language is the
panel's at the time the settings were saved. `NotificationTexts` writes dates
and amounts the way that language does.

The panel's "Send test mail" saves the form, then sends a `TEST` mail through
the stored settings. The demo answers it with `9000`.