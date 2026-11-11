---
name: security-review
description: Security review checklist for the ElisDB repo (secrets, auth, storage, network, DB). Use before commits, when reviewing code for vulnerabilities, or when asked to audit security.
---

# Security Review

## Secrets to hunt for (verified against this project)

- Device config files `*.config.json` (contain `private_seed` ed25519) — must stay git-ignored.
- Databases `*.db`.
- Android signing: `*.jks`, `*.keystore`, `keystore.properties`, passwords.
- `.env`, API keys, tokens, hardcoded passwords, credentials in URLs.

## Checks by area

- **Auth**: every protected server route must enforce `require_auth`; verify signature
  freshness (±300s) and replay protection are not bypassed. The app must never sign with a
  blank or baked-in key (`ApiClientProvider` throws on blank key).
- **Storage at rest**: the app's signing key is in `EncryptedSharedPreferences`
  (Android Keystore master key); do not downgrade to plain `SharedPreferences`.
- **Network**: `network_security_config.xml` currently permits cleartext HTTP (LAN sync).
  Do not widen this further; do not embed credentials in URLs.
- **Path safety**: photo ids must not contain `/`, `\`, `..` (path traversal guard). Keep it.
- **DB access**: queries use parameterized statements (no string concatenation of user input).
- **Logs**: no signatures, seeds, photo bytes, or passwords in logs (server and app).

## Rules

- Report findings with the affected file(s) and impact. **Do not silently auto-fix critical
  issues** — explain the impact first and get confirmation before changing security behavior.
- Never commit or push secrets; never rotate/delete a key or keystore without confirmation.
- If a secret may already be committed/pushed, treat it as compromised and recommend rotation.

## Quick pre-commit scan

```bash
git status --short | grep -iE '\.config\.json$|\.db$|\.jks$|\.keystore$|keystore\.properties|\.env$|ELIS\.csv'
grep -rniE '(password|secret|api[_-]?key|token|private_seed)' --include='*.kt' --include='*.py' --include='*.json' . | grep -v '/build/' | grep -v 'test'
```
