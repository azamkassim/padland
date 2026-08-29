package com.mikifus.padland.Utils

import java.net.URI

/**
 * Canonical transport policy for NEXUS collaboration traffic.
 *
 * Remote servers must use HTTPS. Cleartext HTTP is permitted only for
 * same-device localhost so a Termux-hosted Etherpad can be used locally.
 */
object NexusNetworkPolicy {
    fun isTransportAllowed(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false

        val uri = try {
            URI(rawUrl)
        } catch (_: Exception) {
            return false
        }

        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false

        return scheme == "https" || (scheme == "http" && host == "localhost")
    }
}
