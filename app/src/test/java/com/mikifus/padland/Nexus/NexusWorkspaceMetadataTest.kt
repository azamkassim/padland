package com.mikifus.padland.Nexus

import org.junit.Assert.assertEquals
import org.junit.Test

class NexusWorkspaceMetadataTest {
    @Test
    fun acceptsOpaqueWorkspaceReferences() {
        val metadata = NexusWorkspaceMetadata(
            customerId = "CUS-123",
            applicationId = "APP-456",
            workspaceId = "WS-789",
            teamPadId = "PAD-001"
        )

        assertEquals("WS-789", metadata.workspaceId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankIdentifiers() {
        NexusWorkspaceMetadata(
            customerId = "",
            applicationId = "APP-456",
            workspaceId = "WS-789",
            teamPadId = "PAD-001"
        )
    }
}
