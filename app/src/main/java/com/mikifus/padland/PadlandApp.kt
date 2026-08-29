package com.mikifus.padland

import android.app.Application
import com.mikifus.padland.Database.PadListDatabase
import com.mikifus.padland.Utils.NexusApprovedServerPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Parent App class.
 *
 * NEXUS hardening disables any persisted server that is no longer present in
 * the administrator-managed approved-origin registry before an Activity starts.
 */
class PadlandApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val approvedOrigins = resources
            .getStringArray(R.array.nexus_approved_server_origins)
            .toList()

        runBlocking(Dispatchers.IO) {
            val serverDao = PadListDatabase.getInstance(this@PadlandApp).serverDao()
            val unapprovedEnabledServers = serverDao.getAllNow().filter { server ->
                server.mEnabled &&
                    !NexusApprovedServerPolicy.isApprovedServerUrl(server.mUrl, approvedOrigins)
            }

            unapprovedEnabledServers.forEach { server ->
                serverDao.update(server.copy(mEnabled = false))
            }
        }
    }
}
