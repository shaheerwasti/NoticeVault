package com.noticevault.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.noticevault.classifier.NotificationClassifier
import com.noticevault.db.AppDatabase
import com.noticevault.db.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VaultNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var classifier: NotificationClassifier
    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
        classifier = NotificationClassifier(this)
        Log.d("VaultService", "Notification listener started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName) return                          // skip our own
        if (sbn.isOngoing) return                               // skip system ongoing

        val prefs = getSharedPreferences("noticevault_prefs", MODE_PRIVATE)
        val whitelist = prefs.getStringSet("whitelist_apps", emptySet()) ?: emptySet()
        if (pkg in whitelist) return                            // skip whitelisted apps

        val extras = sbn.notification.extras
        val title   = extras.getString("android.title")?.trim() ?: ""
        val content = extras.getCharSequence("android.text")?.toString()?.trim() ?: ""

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }

        // Silently dismiss
        try { cancelNotification(sbn.key) } catch (e: Exception) { /* ignore */ }

        // Archive then classify
        scope.launch {
            val id = db.notificationDao().insert(
                NotificationEntity(
                    appPackage = pkg,
                    appName    = appName,
                    title      = title,
                    content    = content,
                    timestamp  = System.currentTimeMillis()
                )
            )
            val category = classifier.classify(appName, title, content)
            db.notificationDao().updateCategory(id, category)
        }
    }
}
