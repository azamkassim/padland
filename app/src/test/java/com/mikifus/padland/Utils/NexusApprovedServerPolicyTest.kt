package com.mikifus.padland.Utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexusApprovedServerPolicyTest {
    private val approvedOrigins = listOf(
        "http://localhost:9001",
        "https://collab.example.com"
    )

    @Test
    fun allowsOnlyConfiguredOrigins() {
        assertTrue(NexusApprovedServerPolicy.isApprovedServerUrl("http://localhost:9001/p/demo", approvedOrigins))
        assertTrue(NexusApprovedServerPolicy.isApprovedServerUrl("https://collab.example.com/p/demo", approvedOrigins))
        assertTrue(NexusApprovedServerPolicy.isApprovedServerUrl("https://collab.example.com:443/p/demo", approvedOrigins))

        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("https://other.example.com/p/demo", approvedOrigins))
        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("https://collab.example.com:8443/p/demo", approvedOrigins))
        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("http://localhost:9002/p/demo", approvedOrigins))
    }

    @Test
    fun rejectsUnsafeOrConfusingUrls() {
        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("http://collab.example.com", approvedOrigins))
        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("https://collab.example.com@evil.example", approvedOrigins))
        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("https://user:pass@collab.example.com", approvedOrigins))
        assertFalse(NexusApprovedServerPolicy.isApprovedServerUrl("not a url", approvedOrigins))
    }
}
