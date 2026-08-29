package com.mikifus.padland.Activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.mikifus.padland.Database.ServerModel.ServerViewModel
import com.mikifus.padland.R

/** Governed settings surface for NEXUS TeamPad. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.activity_toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "NEXUS TeamPad Settings"

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var sharedPreferences: SharedPreferences? = null
        private var serverViewModel: ServerViewModel? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            initDefaultServerPreference()
        }

        private fun initDefaultServerPreference() {
            if (serverViewModel == null) {
                serverViewModel = ViewModelProvider(this)[ServerViewModel::class.java]
            }

            serverViewModel!!.getAllEnabled.observe(this) { servers ->
                val serverEntries = servers.map { it.mName }.toTypedArray() +
                    resources.getStringArray(R.array.etherpad_servers_name)
                val serverValues = servers.map { it.mUrl + it.mPadprefix }.toTypedArray() +
                    resources.getStringArray(R.array.etherpad_servers_url_padprefix)

                val listPreference = findPreference<ListPreference>("padland_default_server")
                listPreference?.entries = serverEntries
                listPreference?.entryValues = serverValues

                if (serverValues.isNotEmpty()) {
                    val configured = sharedPreferences?.getString("padland_default_server", null)
                    val selected = configured?.takeIf { serverValues.contains(it) }
                        ?: serverValues.first()

                    sharedPreferences?.edit()
                        ?.putString("padland_default_server", selected)
                        ?.apply()
                    listPreference?.value = selected
                }

                if (serverEntries.size == 1) {
                    listPreference?.summary = serverEntries.first()
                    listPreference?.isEnabled = false
                } else {
                    listPreference?.summary = "Choose an administrator-approved server."
                    listPreference?.isEnabled = true
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
