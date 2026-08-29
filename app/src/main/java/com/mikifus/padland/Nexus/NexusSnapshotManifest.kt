package com.mikifus.padland.Nexus

import com.mikifus.padland.Utils.NexusNetworkPolicy
import java.security.MessageDigest

/**
 * Immutable manifest for a human-reviewed collaboration snapshot handoff.
 *
 * The manifest intentionally contains only lineage metadata and a content hash;
 * collaborative text itself is not stored here and does not become governed
 * NEXUS information merely because a manifest has been created.
 */
data class NexusSnapshotManifest(
    val schemaVersion: String,
    val snapshotId: String,
    val workspace: NexusWorkspaceMetadata,
    val padUrl: String,
    val capturedAtUtc: String,
    val contentSha256: String,
    val contentLength: Int,
    val reviewState: String
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = "1.0"
        const val PENDING_REVIEW = "PENDING_GOVERNED_REVIEW"

        fun create(
            snapshotId: String,
            workspace: NexusWorkspaceMetadata,
            padUrl: String,
            capturedAtUtc: String,
            content: String
        ): NexusSnapshotManifest {
            require(snapshotId.isNotBlank()) { "snapshotId must not be blank" }
            require(capturedAtUtc.isNotBlank()) { "capturedAtUtc must not be blank" }
            require(NexusNetworkPolicy.isTransportAllowed(padUrl)) { "padUrl violates NEXUS transport policy" }

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(content.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

            return NexusSnapshotManifest(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                snapshotId = snapshotId,
                workspace = workspace,
                padUrl = padUrl,
                capturedAtUtc = capturedAtUtc,
                contentSha256 = digest,
                contentLength = content.toByteArray(Charsets.UTF_8).size,
                reviewState = PENDING_REVIEW
            )
        }
    }
}
