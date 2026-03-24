package com.ndzl.a15_directboot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

/**
 * Main Activity — NOT direct-boot-aware.
 * Only accessible after the user has unlocked the device.
 *
 * Shows a dashboard of:
 *  - Current device lock state
 *  - DE storage contents (written by the FGS before unlock)
 *  - CE storage contents (written by the FGS after unlock)
 *  - Manual controls to start/stop the FGS
 */
class DirectBootActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDeStorage: TextView
    private lateinit var tvCeStorage: TextView
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var btnRefresh: Button

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op, just need to ask */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Edge-to-Edge is ENFORCED for targetSdk 35 — handle WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        tvDeStorage = findViewById(R.id.tvDeStorage)
        tvCeStorage = findViewById(R.id.tvCeStorage)
        btnStartService = findViewById(R.id.btnStartService)
        btnStopService = findViewById(R.id.btnStopService)
        btnRefresh = findViewById(R.id.btnRefresh)

        // Request POST_NOTIFICATIONS on API 33+
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        btnStartService.setOnClickListener {
            val intent = Intent(this, DirectBootForegroundService::class.java).apply {
                putExtra(DirectBootForegroundService.EXTRA_PRE_UNLOCK, false)
            }
            startForegroundService(intent)
            refreshStatus()
        }

        btnStopService.setOnClickListener {
            stopService(Intent(this, DirectBootForegroundService::class.java))
            refreshStatus()
        }

        btnRefresh.setOnClickListener { refreshStatus() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager

        val sb = StringBuilder()
        sb.appendLine("── Device State ──")
        sb.appendLine("User unlocked: ${userManager.isUserUnlocked}")
        sb.appendLine("API level: ${Build.VERSION.SDK_INT}")
        sb.appendLine("Android version: ${Build.VERSION.RELEASE}")
        sb.appendLine()
        sb.appendLine("── A15 Direct Boot Notes ──")
        sb.appendLine("• LOCKED_BOOT_COMPLETED fires before PIN entry")
        sb.appendLine("• Only DE storage is available pre-unlock")
        sb.appendLine("• FGS type 'specialUse' is safe to start at boot")
        sb.appendLine("  (A15 blocks: dataSync, camera, mediaPlayback,")
        sb.appendLine("   location, microphone, phoneCall, etc.)")
        tvStatus.text = sb.toString()

        // Read DE storage
        val deContext = createDeviceProtectedStorageContext()
        val deSb = StringBuilder()
        deSb.appendLine("── DE SharedPreferences ──")
        val dePrefs = deContext.getSharedPreferences("de_prefs", Context.MODE_PRIVATE)
        deSb.appendLine("boot_count = ${dePrefs.getInt("boot_count", 0)}")
        deSb.appendLine()
        deSb.appendLine("── DE File (de_boot_log.txt) ──")
        val deFile = File(deContext.filesDir, "de_boot_log.txt")
        if (deFile.exists()) {
            val lines = deFile.readLines()
            // Show last 5 entries
            lines.takeLast(5).forEach { deSb.appendLine(it) }
            if (lines.size > 5) deSb.appendLine("... (${lines.size} total entries)")
        } else {
            deSb.appendLine("(no boot log yet — reboot the device)")
        }
        tvDeStorage.text = deSb.toString()

        // Read CE storage
        val ceSb = StringBuilder()
        ceSb.appendLine("── CE SharedPreferences ──")
        val cePrefs = getSharedPreferences("ce_prefs", Context.MODE_PRIVATE)
        val unlockedAt = cePrefs.getString("unlocked_at", "(none)")
        ceSb.appendLine("last unlocked_at = $unlockedAt")
        ceSb.appendLine()
        ceSb.appendLine("── CE File (ce_unlock_log.txt) ──")
        val ceFile = File(filesDir, "ce_unlock_log.txt")
        if (ceFile.exists()) {
            val lines = ceFile.readLines()
            lines.takeLast(5).forEach { ceSb.appendLine(it) }
            if (lines.size > 5) ceSb.appendLine("... (${lines.size} total entries)")
        } else {
            ceSb.appendLine("(no unlock log yet — reboot the device)")
        }
        tvCeStorage.text = ceSb.toString()
    }
}
