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

## Security invariants implemented in v1

1. Public Etherpad/CryptPad services are not trusted defaults.
2. Remote collaboration traffic must use HTTPS.
3. TLS/SSL certificate errors fail closed and cannot be bypassed.
4. Cleartext HTTP is allowed only to `localhost` for a same-device Termux-hosted service.
5. Android application backup is disabled for collaboration metadata.
6. WebView resource access remains host-whitelisted.
7. Non-HTTPS remote servers are rejected when a custom server is saved.
8. Cancelling a server approval dialog cannot temporarily whitelist the host.
9. Third-party WebView cookies are disabled.
10. Mixed HTTP content is blocked.
11. WebView cache is set to no-cache and local file/content access is disabled.

## Built-in development endpoint

The only built-in server is:

- Name: `NEXUS Local (Termux)`
- Home: `http://localhost:9001`
- Pad prefix: `http://localhost:9001/p/`

This endpoint is intended for same-device local development/testing only.

## Still required before production use

- Add an administrator-managed approved-server policy rather than relying only on user-added servers.
- Add NEXUS workspace metadata (`Customer -> Application -> Workspace -> Team Pad`) without storing authoritative customer facts in the pad client.
- Add explicit `Submit to NEXUS` snapshot/export flow with immutable lineage and human review.
- Add security regression tests for transport, TLS failure, whitelist and cookie behaviour.
- Complete Android build and on-device acceptance testing.

## Automated verification

The branch includes `.github/workflows/android-ci.yml` to run:

- `testDebugUnitTest`
- `assembleDebug`

on pull requests to `master` and pushes to the hardening branch.

## Release gate

Do not merge this branch into a production-distributed build until the Android project compiles successfully and the following cases are tested on-device:

1. `http://localhost:9001` works when Etherpad is running locally.
2. Remote `http://` endpoints are blocked and cannot be saved.
3. Valid remote `https://` endpoints work only when explicitly configured/whitelisted.
4. Invalid, expired, mismatched, or untrusted TLS certificates are blocked with no bypass.
5. Public upstream default servers do not appear in the new-pad server list.
6. Cancelling an unapproved-host dialog does not load or whitelist that host.
7. Third-party cookies remain disabled for tested Etherpad use cases.
8. Application reinstall/backup behaviour does not export collaboration metadata through Android backup.
