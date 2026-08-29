package com.mikifus.padland.Utils

import java.net.URI

/**
 * Administrator-managed server approval policy for the NEXUS collaboration adapter.
 *
 * The approved origin list is supplied from an immutable application resource at build time.
 * End users may configure only servers whose origin is already present in that registry.
 */
object NexusApprovedServerPolicy {
    fun isApprovedServerUrl(rawUrl: String?, approvedOrigins: Collection<String>): Boolean {
        if (!NexusNetworkPolicy.isTransportAllowed(rawUrl)) return false

        val candidateOrigin = normalizeOrigin(rawUrl) ?: return false
        val approved = approvedOrigins.mapNotNull(::normalizeOrigin).toSet()
        return candidateOrigin in approved
    }

    private fun normalizeOrigin(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null

        val uri = try {
            URI(rawUrl.trim())
        } catch (_: Exception) {
            return null
        }

        if (uri.userInfo != null) return null

        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val port = when {
            uri.port == -1 -> null
            scheme == "https" && uri.port == 443 -> null
            scheme == "http" && uri.port == 80 -> null
            else -> uri.port
        }

        return buildString {
            append(scheme)
            append("://")
            append(host)
            if (port != null) {
                append(":")
                append(port)
            }
        }
    }
}
