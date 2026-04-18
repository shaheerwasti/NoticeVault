package com.noticevault.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.noticevault.MainActivity
import com.noticevault.R
import com.noticevault.db.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "nv_bubble"
        const val NOTIF_ID   = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildPersistentNotification())
        setupBubble()
        observeCount()
    }

    private fun setupBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24; y = 220
        }

        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var ix = 0; private var iy = 0
            private var tx = 0f; private var ty = 0f
            private var t0 = 0L

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ix = params.x; iy = params.y
                        tx = e.rawX;   ty = e.rawY
                        t0 = System.currentTimeMillis()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = ix + (tx - e.rawX).toInt()
                        params.y = iy + (e.rawY - ty).toInt()
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    MotionEvent.ACTION_UP -> {
                        val elapsed = System.currentTimeMillis() - t0
                        val moved   = abs(e.rawX - tx) + abs(e.rawY - ty)
                        if (elapsed < 300 && moved < 12) {
                            startActivity(
                                Intent(this@FloatingBubbleService, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                        }
                    }
                }
                return true
            }
        })

        windowManager.addView(bubbleView, params)
    }

    private fun observeCount() {
        scope.launch {
            AppDatabase.getInstance(this@FloatingBubbleService)
                .notificationDao().getUnreadCountFlow()
                .collectLatest { count ->
                    bubbleView.findViewById<TextView>(R.id.bubble_count).text =
                        if (count > 99) "99+" else count.toString()
                }
        }
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "NoticeVault Service", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Keeps the vault running" }
            )
    }

    private fun buildPersistentNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NoticeVault Active")
            .setContentText("Archiving notifications silently")
            .setSmallIcon(R.drawable.ic_vault)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
    }
}
