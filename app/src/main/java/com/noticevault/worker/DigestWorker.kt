package com.noticevault.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.noticevault.R
import com.noticevault.db.AppDatabase
import java.util.*
import java.util.concurrent.TimeUnit

class DigestWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "nv_digest"

        fun schedule(context: Context) {
            val delay = nextMorningDelay()
            val req   = PeriodicWorkRequestBuilder<DigestWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("daily_digest", ExistingPeriodicWorkPolicy.KEEP, req)
        }

        private fun nextMorningDelay(): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis())
                    add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis - System.currentTimeMillis()
        }
    }

    override suspend fun doWork(): Result {
        val db      = AppDatabase.getInstance(applicationContext)
        val since   = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val recent  = db.notificationDao().getSince(since)
        if (recent.isEmpty()) return Result.success()

        val summary = recent.groupBy { it.category }
            .entries.joinToString(" | ") { "${it.value.size} ${it.key}" }

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily Digest", NotificationManager.IMPORTANCE_DEFAULT)
        )

        nm.notify(2001,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("NoticeVault Daily Digest")
                .setContentText("${recent.size} archived: $summary")
                .setSmallIcon(R.drawable.ic_vault)
                .setAutoCancel(true)
                .build()
        )
        return Result.success()
    }
}
