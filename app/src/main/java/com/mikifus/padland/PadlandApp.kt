package com.mikifus.padland

import android.app.Application
import androidx.preference.PreferenceManager
import com.mikifus.padland.Database.PadListDatabase
import com.mikifus.padland.Utils.NexusApprovedServerPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Parent application for the governed NEXUS TeamPad build.
 *
 * Before any Activity starts, persisted servers outside the administrator-managed
 * registry are disabled and safe first-run preferences are established.
 */
class PadlandApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val approvedOrigins = resources
            .getStringArray(R.array.nexus_approved_server_origins)
            .toList()
        val builtInPadPrefixes = resources
            .getStringArray(R.array.etherpad_servers_url_padprefix)
            .toList()

        val enabledApprovedPrefixes = runBlocking(Dispatchers.IO) {
            val serverDao = PadListDatabase.getInstance(this@PadlandApp).serverDao()
            val allServers = serverDao.getAllNow()

            allServers
                .filter { server ->
                    server.mEnabled &&
                        !NexusApprovedServerPolicy.isApprovedServerUrl(server.mUrl, approvedOrigins)
                }
                .forEach { server ->
                    serverDao.update(server.copy(mEnabled = false))
                }

            serverDao.getAllNow()
                .filter { server ->
                    server.mEnabled &&
                        NexusApprovedServerPolicy.isApprovedServerUrl(server.mUrl, approvedOrigins)
                }
                .map { server -> server.mUrl + server.mPadprefix }
        }

        val allowedPadPrefixes = (enabledApprovedPrefixes + builtInPadPrefixes).distinct()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        var changed = false

        if (!preferences.contains("auto_save_new_pads")) {
            editor.putBoolean("auto_save_new_pads", false)
            changed = true
        }

        val configuredDefault = preferences.getString("padland_default_server", null)
        if (configuredDefault == null || !allowedPadPrefixes.contains(configuredDefault)) {
            builtInPadPrefixes.firstOrNull()?.let { safeDefault ->
                editor.putString("padland_default_server", safeDefault)
                changed = true
            }
        }

        if (changed) editor.apply()
    }
}
