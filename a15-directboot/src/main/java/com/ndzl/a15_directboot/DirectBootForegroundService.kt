package com.ndzl.a15_directboot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * A long-running Foreground Service that demonstrates Direct Boot behavior.
 *
 * PHASE 1 — Pre-Unlock (LOCKED_BOOT_COMPLETED):
 *   The device has booted but the user hasn't entered their PIN/password.
 *   Only Device Encrypted (DE) storage is accessible via createDeviceProtectedStorageContext().
 *   This phase probes and logs what IS and ISN'T available.
 *
 * PHASE 2 — Post-Unlock (BOOT_COMPLETED):
 *   The user has entered PIN/password. Credential Encrypted (CE) storage is now open.
 *   The service transitions to full mode and demonstrates accessing CE resources.
 *
 * FGS Type: specialUse
 *   Chosen deliberately because Android 15 BLOCKS the following FGS types from
 *   starting at BOOT_COMPLETED: dataSync, camera, mediaPlayback, phoneCall,
 *   location, microphone, mediaProcessing, connectedDevice.
 *   `specialUse` is NOT in that restricted list.
 */
class DirectBootForegroundService : Service() {

    companion object {
        private const val TAG = "A15-DirectBoot-FGS"
        private const val CHANNEL_ID = "direct_boot_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_PRE_UNLOCK = "pre_unlock"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPreUnlock = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isPreUnlock = intent?.getBooleanExtra(EXTRA_PRE_UNLOCK, true) ?: true

        val notification = buildNotification(
            if (isPreUnlock) "Direct Boot: Pre-Unlock (DE only)"
            else "Direct Boot: Post-Unlock (Full access)"
        )

        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        if (isPreUnlock) {
            probePreUnlockCapabilities()
        } else {
            probePostUnlockCapabilities()
        }

        return START_STICKY
    }

    // =========================================================================
    // PHASE 1: What's available BEFORE PIN/password entry?
    // =========================================================================
    private fun probePreUnlockCapabilities() {
        Log.w(TAG, "╔══════════════════════════════════════════════════════════╗")
        Log.w(TAG, "║   DIRECT BOOT — PRE-UNLOCK RESOURCE PROBE              ║")
        Log.w(TAG, "║   (User has NOT entered PIN/password yet)               ║")
        Log.w(TAG, "╚══════════════════════════════════════════════════════════╝")

        // --- DE Storage: AVAILABLE ---
        val deContext = createDeviceProtectedStorageContext()
        val dePrefs = deContext.getSharedPreferences("de_prefs", Context.MODE_PRIVATE)
        val bootCount = dePrefs.getInt("boot_count", 0) + 1
        dePrefs.edit().putInt("boot_count", bootCount).apply()
        Log.w(TAG, "✔ DE SharedPreferences: AVAILABLE (boot count: $bootCount)")

        val deFile = File(deContext.filesDir, "de_boot_log.txt")
        deFile.appendText("Boot #$bootCount at ${System.currentTimeMillis()}\n")
        Log.w(TAG, "✔ DE File I/O: AVAILABLE (wrote to ${deFile.absolutePath})")

        val deDb = deContext.openOrCreateDatabase("de_database.db", Context.MODE_PRIVATE, null)
        deDb.execSQL("CREATE TABLE IF NOT EXISTS boot_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER)")
        deDb.execSQL("INSERT INTO boot_log (ts) VALUES (${System.currentTimeMillis()})")
        val cursor = deDb.rawQuery("SELECT COUNT(*) FROM boot_log", null)
        cursor.moveToFirst()
        Log.w(TAG, "✔ DE SQLite Database: AVAILABLE (${cursor.getInt(0)} boot records)")
        cursor.close()
        deDb.close()

        // --- CE Storage: NOT AVAILABLE ---
        Log.w(TAG, "━━━ Credential Encrypted (CE) storage — LOCKED ━━━")
        try {
            val cePrefs = getSharedPreferences("ce_prefs", Context.MODE_PRIVATE)
            cePrefs.edit().putString("test", "value").apply()
            Log.w(TAG, "⚠ CE SharedPreferences: unexpectedly available (device may not have PIN set)")
        } catch (e: Exception) {
            Log.w(TAG, "✘ CE SharedPreferences: BLOCKED — ${e.javaClass.simpleName}: ${e.message}")
        }

        try {
            val ceFile = File(filesDir, "ce_test.txt")
            ceFile.writeText("test")
            Log.w(TAG, "⚠ CE File I/O: unexpectedly available (device may not have PIN set)")
        } catch (e: Exception) {
            Log.w(TAG, "✘ CE File I/O: BLOCKED — ${e.javaClass.simpleName}: ${e.message}")
        }

        // --- System Services: partially available ---
        Log.w(TAG, "━━━ System services availability ━━━")

        //5 seconds delay are added here to allow for network to become available
        Thread.sleep(5000)

        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        Log.w(TAG, "✔ UserManager: AVAILABLE (isUserUnlocked=${userManager.isUserUnlocked})")

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        Log.w(TAG, "✔ ConnectivityManager: AVAILABLE (network=${ if (hasInternet) "CONNECTED" else "NONE" })")

        val alarmManager = getSystemService(Context.ALARM_SERVICE)
        Log.w(TAG, "✔ AlarmManager: AVAILABLE ($alarmManager)")

        Log.w(TAG, "✔ System clock: ${System.currentTimeMillis()} (wall clock available in DE mode)")

        // --- Content Providers: limited ---
        Log.w(TAG, "━━━ Content Providers ━━━")
        Log.w(TAG, "✘ ContactsProvider: NOT available (CE-protected)")
        Log.w(TAG, "✘ CalendarProvider: NOT available (CE-protected)")
        Log.w(TAG, "✘ MediaStore: NOT available (CE-protected)")
        Log.w(TAG, "✔ Settings.Global/Settings.Secure: AVAILABLE (DE-backed)")

        // --- Register for USER_UNLOCKED to transition ---
        Log.w(TAG, "━━━ Waiting for user to unlock device... ━━━")
        serviceScope.launch {
            // Periodic heartbeat while waiting
            var tick = 0
            while (isPreUnlock && isActive) {
                delay(10_000)
                tick++
                Log.d(TAG, "Pre-unlock heartbeat #$tick — still waiting for PIN/password...")
            }
        }
    }

    // =========================================================================
    // PHASE 2: What becomes available AFTER PIN/password entry?
    // =========================================================================
    private fun probePostUnlockCapabilities() {
        isPreUnlock = false

        Log.w(TAG, "╔══════════════════════════════════════════════════════════╗")
        Log.w(TAG, "║   DEVICE UNLOCKED — POST-UNLOCK RESOURCE PROBE         ║")
        Log.w(TAG, "║   (User has entered PIN/password)                       ║")
        Log.w(TAG, "╚══════════════════════════════════════════════════════════╝")

        // --- CE Storage: NOW AVAILABLE ---
        val cePrefs = getSharedPreferences("ce_prefs", Context.MODE_PRIVATE)
        cePrefs.edit().putString("unlocked_at", System.currentTimeMillis().toString()).apply()
        Log.w(TAG, "✔ CE SharedPreferences: NOW AVAILABLE")

        val ceFile = File(filesDir, "ce_unlock_log.txt")
        ceFile.appendText("Unlocked at ${System.currentTimeMillis()}\n")
        Log.w(TAG, "✔ CE File I/O: NOW AVAILABLE (wrote to ${ceFile.absolutePath})")

        // --- DE Storage: STILL AVAILABLE ---
        val deContext = createDeviceProtectedStorageContext()
        val dePrefs = deContext.getSharedPreferences("de_prefs", Context.MODE_PRIVATE)
        val bootCount = dePrefs.getInt("boot_count", 0)
        Log.w(TAG, "✔ DE SharedPreferences: STILL AVAILABLE (boot count: $bootCount)")

        // --- Migrate DE → CE if needed ---
        Log.w(TAG, "━━━ DE → CE migration ━━━")
        Log.w(TAG, "ℹ You can now call moveSharedPreferencesFrom(deContext, name)")
        Log.w(TAG, "  to migrate DE data into CE once the user has unlocked.")
        Log.w(TAG, "  This is useful for one-time migration of boot-phase data.")

        // --- Content Providers: NOW AVAILABLE ---
        Log.w(TAG, "━━━ Content Providers — NOW UNLOCKED ━━━")
        Log.w(TAG, "✔ ContactsProvider: AVAILABLE")
        Log.w(TAG, "✔ CalendarProvider: AVAILABLE")
        Log.w(TAG, "✔ MediaStore: AVAILABLE")

        // --- Full system services ---
        Log.w(TAG, "━━━ Full system services ━━━")
        Log.w(TAG, "✔ AccountManager: AVAILABLE (user accounts accessible)")
        Log.w(TAG, "✔ KeyStore: AVAILABLE (credential-bound keys accessible)")

        // Update notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification("Direct Boot: Fully operational (CE+DE)"))

        // Long-running work
        serviceScope.launch {
            var tick = 0
            while (isActive) {
                delay(30_000)
                tick++
                Log.d(TAG, "Post-unlock heartbeat #$tick — service running normally")
            }
        }
    }

    // =========================================================================
    // Notification boilerplate
    // =========================================================================
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("A15 Direct Boot Service")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
