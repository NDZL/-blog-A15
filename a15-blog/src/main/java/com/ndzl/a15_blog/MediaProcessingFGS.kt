package com.ndzl.a15_blog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Blog (4): Media Processing Foreground Service
 * Wiki: A15‐(4)‐Optimizing Performance & Background Behavior
 *
 * Android 15 introduces a new foreground service type: `mediaProcessing`.
 * This type is designed for long-running, resource-intensive operations like
 * transcoding, encoding, or media file conversion.
 *
 * Key operational rules from the blog:
 *
 *   - **6-hour cumulative timeout** per 24-hour period.
 *     After 6 hours the system invokes Service.onTimeout(int startId).
 *     The service must call stopSelf() within seconds or the system throws
 *     RemoteServiceException (potential ANR crash).
 *
 *   - **User interaction resets the timer** completely.
 *
 *   - **Boot storm prevention** (Blog 4):
 *     This FGS type CANNOT be started from BOOT_COMPLETED receivers.
 *     Attempting to do so throws ForegroundServiceStartNotAllowedException.
 *     The blog recommends WorkManager for non-urgent post-boot tasks.
 *
 *   - **Background start restrictions** (Blog 4):
 *     Apps holding SYSTEM_ALERT_WINDOW must also have a visible
 *     TYPE_APPLICATION_OVERLAY window to start FGS from background.
 *
 * Required permissions (declared in AndroidManifest.xml):
 *   - android.permission.FOREGROUND_SERVICE
 *   - android.permission.FOREGROUND_SERVICE_MEDIA_PROCESSING
 *   - android.permission.POST_NOTIFICATIONS (API 33+)
 *
 * This demo starts the service, posts a notification, simulates 15 seconds
 * of work, then stops itself — illustrating the basic lifecycle.
 */
class MediaProcessingFGS : Service() {

    private val TAG = "A15-Blog"
    private val CHANNEL_ID = "media_processing_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Blog (4): MediaProcessingFGS onStartCommand")

        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, A15BlogMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Processing")
            .setContentText("Blog (4): mediaProcessing FGS type — 6h timeout")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .build()

        // API 35: FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        // This type has a 6-hour timeout enforced by the system.
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)

        Log.i(TAG, """
            Blog (4): MediaProcessing FGS started
            - Type: FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            - 6-hour cumulative timeout per 24h window
            - Cannot be started from BOOT_COMPLETED receivers (boot storm prevention)
            - Requires FOREGROUND_SERVICE_MEDIA_PROCESSING permission
            - Designed for: transcoding, encoding, media file processing
        """.trimIndent())

        // Simulate work then stop — in production, replace with actual transcoding logic
        Thread {
            Thread.sleep(15000)
            Log.i(TAG, "Blog (4): MediaProcessingFGS — simulated work done, stopping self")
            stopSelf()
        }.start()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Media Processing Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
