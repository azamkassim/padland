# NEXUS Padland Hardening Baseline

## Architectural role

This fork is an optional Android front-end for the NEXUS **Collaborative Workspace Adapter – Etherpad** under Workflow, Automation & Approvals.

It is **not** a NEXUS engine and it must not become a source of canonical facts, governed evidence, calculations, policy outcomes, credit recommendations, approval decisions, or final reports.

## Information boundary

Pads are temporary human collaboration workspaces for:

- drafting;
- comments and review;
- meeting notes;
- teammate updates;
- working notes;
- temporary information requests.

Content becomes governed NEXUS information only after an explicit governed submission/snapshot process is implemented upstream.

## Security invariants

1. Public Etherpad/CryptPad services are not trusted defaults.
2. Remote collaboration traffic must use HTTPS.
3. TLS/SSL certificate errors fail closed and cannot be bypassed.
4. Cleartext HTTP is allowed only to `localhost` for a same-device Termux-hosted service.
5. Android application backup is disabled for collaboration metadata.
6. WebView resource access remains host-whitelisted.
7. Adding a remote server is an explicit user action; future releases should add an administrator-managed approved-server registry.

## Built-in development endpoint

The only built-in server is:

- Name: `NEXUS Local (Termux)`
- Home: `http://localhost:9001`
- Pad prefix: `http://localhost:9001/p/`

This endpoint is intended for same-device local development/testing only.

## Still required before production use

- Enforce HTTPS validation when a custom server is saved, with user-visible error messaging.
- Remove the temporary "ignore whitelist" path from the server warning dialog.
- Disable third-party WebView cookies unless a tested Etherpad deployment proves they are required.
- Review WebView cache/storage behaviour and minimize retained collaboration data.
- Add an administrator-managed approved-server policy rather than relying only on user-added servers.
- Add NEXUS workspace metadata (`Customer -> Application -> Workspace -> Team Pad`) without storing authoritative customer facts in the pad client.
- Add explicit `Submit to NEXUS` snapshot/export flow with immutable lineage and human review.
- Add automated Android build/tests and security regression tests.

## Release gate

Do not merge this branch into a production-distributed build until the Android project compiles successfully and the following cases are tested on-device:

1. `http://localhost:9001` works when Etherpad is running locally.
2. Remote `http://` endpoints are blocked.
3. Valid remote `https://` endpoints work only when explicitly configured/whitelisted.
4. Invalid, expired, mismatched, or untrusted TLS certificates are blocked with no bypass.
5. Public upstream default servers do not appear in the new-pad server list.
6. Application reinstall/backup behaviour does not export collaboration metadata through Android backup.
