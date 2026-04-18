package com.noticevault.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.noticevault.databinding.ActivitySettingsBinding
import com.noticevault.db.AppDatabase
import com.noticevault.worker.DigestWorker
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.apply { title = "Settings"; setDisplayHomeAsUpEnabled(true) }

        loadPrefs()
        b.btnSave.setOnClickListener      { savePrefs() }
        b.btnScheduleDigest.setOnClickListener { scheduleDigest() }
        b.btnClearAll.setOnClickListener  { confirmClear() }
    }

    private fun loadPrefs() {
        val p = getSharedPreferences("noticevault_prefs", MODE_PRIVATE)
        b.etApiKey.setText(p.getString("claude_api_key", ""))
        b.etWhitelist.setText(
            (p.getStringSet("whitelist_apps", emptySet()) ?: emptySet()).joinToString("\n")
        )
    }

    private fun savePrefs() {
        val key       = b.etApiKey.text.toString().trim()
        val rawList   = b.etWhitelist.text.toString().trim()
        val whitelist = if (rawList.isBlank()) emptySet()
                        else rawList.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toSet()

        getSharedPreferences("noticevault_prefs", MODE_PRIVATE).edit()
            .putString("claude_api_key", key)
            .putStringSet("whitelist_apps", whitelist)
            .apply()
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleDigest() {
        DigestWorker.schedule(this)
        Toast.makeText(this, "Daily digest scheduled for 8 AM", Toast.LENGTH_SHORT).show()
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle("Clear all notifications")
            .setMessage("This will permanently delete all archived notifications.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getInstance(this@SettingsActivity).notificationDao().deleteAll()
                    Toast.makeText(this@SettingsActivity, "All cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
