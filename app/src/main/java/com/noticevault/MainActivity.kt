package com.noticevault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.noticevault.adapter.NotificationAdapter
import com.noticevault.databinding.ActivityMainBinding
import com.noticevault.db.AppDatabase
import com.noticevault.report.CsvExporter
import com.noticevault.service.FloatingBubbleService
import com.noticevault.ui.SettingsActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        setupList()
        observeData()
        checkAndRequestPermissions()

        b.btnMarkAllRead.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getInstance(this@MainActivity).notificationDao().markAllAsRead()
                Toast.makeText(this@MainActivity, "All marked as read", Toast.LENGTH_SHORT).show()
            }
        }

        b.btnExport.setOnClickListener {
            lifecycleScope.launch {
                val list = AppDatabase.getInstance(this@MainActivity).notificationDao().getAll()
                if (list.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nothing to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val uri = CsvExporter(this@MainActivity).export(list)
                if (uri != null) {
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share Report"
                    ))
                }
            }
        }
    }

    private fun setupList() {
        adapter = NotificationAdapter { notification ->
            lifecycleScope.launch {
                AppDatabase.getInstance(this@MainActivity).notificationDao().markAsRead(notification.id)
            }
        }
        b.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@MainActivity).notificationDao()
                .getAllFlow().collectLatest { list ->
                    adapter.submitList(list)
                    b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    supportActionBar?.subtitle = "${list.size} archived"
                }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this,
                "Enable NoticeVault in notification access settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this,
                "Allow display over other apps for the bubble", Toast.LENGTH_LONG).show()
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        } else {
            startForegroundService(Intent(this, FloatingBubbleService::class.java))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?: return false
        return listeners.contains(packageName)
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, FloatingBubbleService::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
