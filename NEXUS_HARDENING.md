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

## Security and governance invariants implemented

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
12. `nexus_approved_server_origins` is the administrator-managed runtime trust registry.
13. End users cannot add a collaboration origin that is absent from that registry.
14. Persisted server records that are no longer approved are disabled at application startup.
15. `NexusWorkspaceMetadata` defines opaque `Customer -> Application -> Workspace -> Team Pad` linkage identifiers without storing authoritative banking facts.
16. `NexusSnapshotManifest` defines a pending-review handoff manifest containing lineage metadata plus SHA-256/content length, not the collaborative text itself.
17. Creating a snapshot manifest does not make draft content canonical, verified, approved, or governed evidence.

## Built-in development endpoint

The only built-in and currently approved server is:

- Name: `NEXUS Local (Termux)`
- Origin: `http://localhost:9001`
- Pad prefix: `http://localhost:9001/p/`

This endpoint is intended for same-device local development/testing only.

## Administrator server policy

The resource `nexus_approved_server_origins` is authoritative. A production remote server must be added there through a reviewed build/configuration change and must use HTTPS.

The ordinary server-management UI can configure only origins already approved by that resource; it cannot expand the trust boundary itself.

## NEXUS handoff contract

See `NEXUS_INTEGRATION_CONTRACT.md`.

Current code provides:

- `NexusWorkspaceMetadata` for opaque NEXUS linkage IDs;
- `NexusSnapshotManifest` for immutable snapshot lineage/hash metadata;
- review state `PENDING_GOVERNED_REVIEW`.

The manifest deliberately excludes collaborative content. A future explicit `Submit to NEXUS` workflow must capture and transmit/export the point-in-time content separately to an approved ingestion boundary and preserve human review.

## Still required before production use

- Wire `NexusWorkspaceMetadata` to actual pad/workspace creation and selection without adding authoritative banking facts to the collaboration client.
- Implement the explicit user-triggered `Submit to NEXUS` snapshot/export flow to an approved NEXUS ingestion boundary.
- Preserve immutable snapshot lineage and human review after submission.
- Add device/instrumentation tests for TLS failure, whitelist navigation, cookies, backup behaviour and administrator-registry enforcement.
- Complete real-device Android acceptance testing.

## Automated verification

The branch includes `.github/workflows/android-ci.yml` to run:

- inherited unit tests;
- NEXUS transport-policy tests;
- administrator-approved-server policy tests;
- NEXUS workspace/snapshot contract tests;
- `assembleDebug`;
- debug APK artifact upload.

## Release gate

Do not merge this branch into a production-distributed build until the following cases are tested on-device:

1. `http://localhost:9001` works when Etherpad is running locally.
2. Remote `http://` endpoints are blocked and cannot be saved.
3. An unregistered remote HTTPS origin cannot be added through the app.
4. A registry-approved remote HTTPS origin works when explicitly configured.
5. Invalid, expired, mismatched, or untrusted TLS certificates are blocked with no bypass.
6. Public upstream default servers do not appear in the new-pad server list.
7. Cancelling an unapproved-host dialog does not load or whitelist that host.
8. Third-party cookies remain disabled for tested Etherpad use cases.
9. Application reinstall/backup behaviour does not export collaboration metadata through Android backup.
10. A future submitted snapshot remains `PENDING_GOVERNED_REVIEW` until accepted by the appropriate NEXUS governance process.
