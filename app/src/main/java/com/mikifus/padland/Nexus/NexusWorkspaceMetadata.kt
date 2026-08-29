package com.mikifus.padland.Nexus

/**
 * Minimal linkage metadata between a temporary collaboration pad and NEXUS.
 *
 * These fields are opaque identifiers only. Customer names, financing amounts,
 * policy outcomes, approval decisions and other authoritative banking facts do
 * not belong in this client-side metadata object.
 */
data class NexusWorkspaceMetadata(
    val customerId: String,
    val applicationId: String,
    val workspaceId: String,
    val teamPadId: String
) {
    init {
        validateOpaqueId("customerId", customerId)
        validateOpaqueId("applicationId", applicationId)
        validateOpaqueId("workspaceId", workspaceId)
        validateOpaqueId("teamPadId", teamPadId)
    }

    private fun validateOpaqueId(field: String, value: String) {
        require(value.isNotBlank()) { "$field must not be blank" }
        require(value.length <= 128) { "$field must not exceed 128 characters" }
        require(!value.contains('\n') && !value.contains('\r')) { "$field must be a single-line opaque identifier" }
    }
}
