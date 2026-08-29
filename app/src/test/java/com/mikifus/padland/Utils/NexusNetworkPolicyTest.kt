package com.mikifus.padland.Utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexusNetworkPolicyTest {

    @Test
    fun allowsHttpsForRemoteHosts() {
        assertTrue(NexusNetworkPolicy.isTransportAllowed("https://collab.example.com"))
        assertTrue(NexusNetworkPolicy.isTransportAllowed("https://collab.example.com/p/demo"))
    }

    @Test
    fun allowsHttpOnlyForLocalhost() {
        assertTrue(NexusNetworkPolicy.isTransportAllowed("http://localhost:9001"))
        assertTrue(NexusNetworkPolicy.isTransportAllowed("http://localhost:9001/p/demo"))

        assertFalse(NexusNetworkPolicy.isTransportAllowed("http://example.com"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed("http://127.0.0.1:9001"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed("http://localhost.evil.example"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed("http://localhost@evil.example"))
    }

    @Test
    fun rejectsUnsupportedOrMalformedUrls() {
        assertFalse(NexusNetworkPolicy.isTransportAllowed("ftp://collab.example.com"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed("file:///tmp/pad"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed("javascript:alert(1)"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed("not a url"))
        assertFalse(NexusNetworkPolicy.isTransportAllowed(""))
        assertFalse(NexusNetworkPolicy.isTransportAllowed(null))
    }
}
