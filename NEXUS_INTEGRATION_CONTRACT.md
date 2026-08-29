# NEXUS Collaboration Integration Contract

## Purpose

This document defines the boundary between the hardened Padland Android client and NEXUS One Platform.

Padland remains an optional **Collaborative Workspace Adapter – Etherpad** under Workflow, Automation & Approvals. It is not a NEXUS engine and it does not become an authoritative banking information store.

## 1. Administrator-approved server registry

The build-time resource `nexus_approved_server_origins` is the authoritative collaboration-server registry.

Rules:

- End users cannot expand trust from inside the app.
- A remote server must use HTTPS and its normalized origin must already exist in the registry.
- Same-device cleartext HTTP is permitted only for the explicitly approved Termux origin `http://localhost:9001`.
- Previously persisted servers that are no longer approved are disabled during application startup.
- Adding or replacing a production collaboration server therefore requires a reviewed application build/configuration change.

## 2. Workspace linkage metadata

`NexusWorkspaceMetadata` contains only opaque linkage identifiers:

- `customerId`
- `applicationId`
- `workspaceId`
- `teamPadId`

These identifiers link the temporary pad to the NEXUS hierarchy:

`Customer -> Application -> Workspace -> Team Pad`

The Android collaboration client must not store authoritative customer names, facility amounts, security values, policy outcomes, credit recommendations, approval decisions, or final report facts inside this metadata object.

## 3. Governed snapshot boundary

`NexusSnapshotManifest` defines the first governed handoff envelope.

A manifest contains:

- schema version;
- snapshot ID;
- workspace linkage metadata;
- pad URL;
- capture timestamp;
- SHA-256 hash of the captured content;
- UTF-8 content length;
- review state `PENDING_GOVERNED_REVIEW`.

The manifest deliberately does **not** contain the collaborative text itself.

Creating a snapshot manifest does not make the pad content canonical, verified, approved, or governed evidence. The content becomes governed NEXUS information only after a future upstream submission process captures the content, validates lineage, performs human review, and routes it through the appropriate NEXUS ownership/governance layer.

## 4. Information ownership

Padland may hold temporary collaboration content for drafting, comments, teammate updates, meeting notes, working notes and temporary information requests.

Padland must never own:

- canonical facts;
- governed evidence;
- financial calculations;
- scorecard outputs;
- policy outcomes;
- credit recommendations;
- approval decisions;
- final reports.

## 5. Next implementation gate

The next production-facing step is an explicit `Submit to NEXUS` flow that:

1. requires user action;
2. captures a point-in-time pad snapshot;
3. binds it to `NexusWorkspaceMetadata`;
4. creates a `NexusSnapshotManifest`;
5. transmits or exports the snapshot only to an approved NEXUS ingestion boundary;
6. records immutable lineage;
7. leaves the submitted material in `PENDING_GOVERNED_REVIEW` until the appropriate NEXUS governance process accepts it.

No automatic promotion of collaborative text into NEXUS is permitted.
