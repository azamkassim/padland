package com.mikifus.padland.Activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mikifus.padland.Database.PadListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * NEXUS TeamPad launcher.
 *
 * The inherited Padland introduction is intentionally skipped so the governed
 * build opens directly into the NEXUS collaboration workspace.
 */
class InitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force Room DB migration before the main workspace is used.
        lifecycleScope.launch(Dispatchers.Main) {
            PadListDatabase.migrateBeforeRoom(this@InitialActivity)
        }

        startActivity(Intent(this, PadListActivity::class.java))
        finish()
    }
}
