package com.mikifus.padland.Nexus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NexusSnapshotManifestTest {
    private val workspace = NexusWorkspaceMetadata(
        customerId = "CUS-123",
        applicationId = "APP-456",
        workspaceId = "WS-789",
        teamPadId = "PAD-001"
    )

    @Test
    fun createsHashOnlyGovernanceManifest() {
        val manifest = NexusSnapshotManifest.create(
            snapshotId = "SNAP-001",
            workspace = workspace,
            padUrl = "http://localhost:9001/p/demo",
            capturedAtUtc = "2026-08-29T10:00:00Z",
            content = "hello"
        )

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", manifest.contentSha256)
        assertEquals(5, manifest.contentLength)
        assertEquals(NexusSnapshotManifest.PENDING_REVIEW, manifest.reviewState)
        assertFalse(manifest.toString().contains("hello"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafePadTransport() {
        NexusSnapshotManifest.create(
            snapshotId = "SNAP-002",
            workspace = workspace,
            padUrl = "http://remote.example.com/p/demo",
            capturedAtUtc = "2026-08-29T10:00:00Z",
            content = "draft"
        )
    }
}
