package com.ndzl.a15_blog

import android.Manifest
import android.app.ActivityManager
import android.app.ApplicationStartInfo
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.function.Consumer

/**
 * A15BlogMainActivity — Main entry point for the Android 15 blog companion samples.
 *
 * This activity provides a grid of buttons, each demonstrating an API covered in the blog series:
 *   https://github.com/NDZL/-blog-A15/wiki/A15%E2%80%90(0)%E2%80%90Agenda
 *
 * ┌──────────────────────────────────────────────────────────────────────────────────┐
 * │ Blog Post                          │ Features (→ handler method)                │
 * ├──────────────────────────────────────────────────────────────────────────────────┤
 * │ (1) Foundational Compatibility     │ targetSdk 35, 16KB page size              │
 * │     & Build Updates                │ → see build.gradle.kts + onCreate() log   │
 * ├──────────────────────────────────────────────────────────────────────────────────┤
 * │ (2) Security & Privacy Controls    │ Screen Recording Detection                │
 * │                                    │   → onClickbtn_SCREEN_RECORDING()         │
 * │                                    │ Safer Intents                             │
 * │                                    │   → onClickbtn_SAFER_INTENTS()            │
 * │                                    │ Private Space                             │
 * │                                    │   → onClickbtn_PRIVATE_SPACE()            │
 * ├──────────────────────────────────────────────────────────────────────────────────┤
 * │ (3) UX & Multitasking              │ Edge-to-Edge (mandatory)                  │
 * │                                    │   → onClickbtn_EDGE_TO_EDGE()             │
 * │                                    │ Text Features / i18n                      │
 * │                                    │   → onClickbtn_TEXT_FEATURES()            │
 * │                                    │ Large Screen / Multi-Window               │
 * │                                    │   → onClickbtn_LARGE_SCREEN()             │
 * ├──────────────────────────────────────────────────────────────────────────────────┤
 * │ (4) Performance & Background       │ ApplicationStartInfo                      │
 * │     Behavior                       │   → onClickbtn_APP_START_INFO()           │
 * │                                    │ Detailed App Size                         │
 * │                                    │   → onClickbtn_APP_SIZE_INFO()            │
 * │                                    │ Media Processing FGS                      │
 * │                                    │   → onClickbtn_MEDIA_PROCESSING_FGS()     │
 * │                                    │ App-Managed Profiling                     │
 * │                                    │   → onClickbtn_PROFILING()                │
 * ├──────────────────────────────────────────────────────────────────────────────────┤
 * │ (5) Testing Strategy & Migration   │ Informational — see onCreate() log        │
 * └──────────────────────────────────────────────────────────────────────────────────┘
 */
class A15BlogMainActivity : AppCompatActivity() {

    private val TAG = "A15-Blog"
    private lateinit var ctx: Context

    private var screenRecordingCallback: Consumer<Int>? = null

    // ── Blog (2): Sensitive-data views that will be masked during recording ──
    private lateinit var tvEmployeeId: TextView
    private lateinit var tvCreditCard: TextView
    private lateinit var tvPin: TextView
    private lateinit var tvRecordingStatus: TextView

    // Original (real) values — kept in memory so they can be restored
    private val REAL_EMPLOYEE_ID = "EMP-001234"
    private val REAL_CREDIT_CARD = "4532-1234-5678-9012"
    private val REAL_PIN        = "4-digit: 7391"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Blog (3): Edge-to-Edge is ENFORCED for apps targeting API 35 ──
        // The system no longer applies default inset padding.
        // We must handle WindowInsets ourselves or UI will be obscured by system bars.
        setContentView(R.layout.activity_main)
        ctx = this@A15BlogMainActivity

        // Bind sensitive-data panel views
        tvEmployeeId     = findViewById(R.id.tv_employee_id)
        tvCreditCard     = findViewById(R.id.tv_credit_card)
        tvPin            = findViewById(R.id.tv_pin)
        tvRecordingStatus = findViewById(R.id.tv_recording_status)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ── Blog (1): Foundational info ──
        Log.i(TAG, """
            Blog (1) — Foundational App Compatibility & Build Updates
            Running on SDK ${Build.VERSION.SDK_INT}, targeting API 35.
            Minimum target SDK enforcement: apps must target >= API 24 on Android 15 devices.
            16KB page size: pure Kotlin/Java apps are compliant; NDK requires recompilation.
        """.trimIndent())

        // ── Blog (5): Testing Strategy ──
        Log.i(TAG, """
            Blog (5) — Testing Strategy & Migration Timeline
            Phase 1 (Immediate): target SDK 35 ✓, 16KB alignment ✓, deprecated onBackPressed ✓
            Phase 2 (Short-term): edge-to-edge layouts ✓, background launch restructuring ✓
            Phase 3 (Medium-term): screen recording detection ✓, ApplicationStartInfo ✓
            Phase 4 (Ongoing): monitor BSP updates, maintain Android 15 device testing in CI/CD
        """.trimIndent())
    }

    // =====================================================================
    // Blog (3): Edge-to-Edge UI Changes
    // Wiki: A15‐(3)‐Enhancing User Experience & Multitasking
    //
    // Starting with API 35, edge-to-edge rendering is mandatory.
    // - Status bar and navigation bar become transparent by default.
    // - setNavigationBarColor() and setStatusBarColor() are deprecated.
    // - Display cutouts use LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS.
    // - Apps MUST handle WindowInsets or UI elements will be hidden behind
    //   system bars.
    // - Use ViewCompat.setOnApplyWindowInsetsListener() for View-based UIs;
    //   Material 3 Compose components handle insets automatically.
    // =====================================================================
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_EDGE_TO_EDGE(v: View) {
        val msg = """
            Blog (3): Edge-to-Edge is ENFORCED for targetSdk 35.

            What changed:
            - Status bar & nav bar are transparent by default
            - setNavigationBarColor()/setStatusBarColor() are deprecated
            - Three-button nav bar becomes translucent (80% opacity)
            - Display cutouts: LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            - R.attr.windowOptOutEdgeToEdgeEnforcement can temporarily opt out

            What to do:
            - Handle WindowInsets via ViewCompat.setOnApplyWindowInsetsListener()
            - Material 3 Compose components handle insets automatically
            - Test all screen form factors (phones, tablets, foldables)

            This activity already handles insets in onCreate().
        """.trimIndent()
        Log.i(TAG, msg)
        Toast.makeText(ctx, "Edge-to-edge enforced! Check logcat.", Toast.LENGTH_LONG).show()
    }

    // =====================================================================
    // Blog (2): Screen Recording Detection
    // Wiki: A15‐(2)‐Next‐Generation Security & Privacy Controls
    //
    // New API: WindowManager.addScreenRecordingCallback()
    // Similar to screenshot detection (API 34), but for active recording.
    // The callback receives SCREEN_RECORDING_STATE_VISIBLE when recording
    // starts, allowing apps to hide sensitive content or warn the user.
    //
    // Requires: android.permission.DETECT_SCREEN_RECORDING
    //
    // Enterprise use case: detect when sensitive enterprise data is being
    // recorded on screen. This complements DLP (Data Loss Prevention)
    // policies in MDM-managed environments.
    // =====================================================================
    @RequiresPermission(Manifest.permission.DETECT_SCREEN_RECORDING)
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_SCREEN_RECORDING(v: View) {
        if (screenRecordingCallback != null) {
            // Toggle off — remove the callback
            windowManager.removeScreenRecordingCallback(screenRecordingCallback!!)
            screenRecordingCallback = null
            maskSensitiveData(false)   // restore real data when detection is off
            tvRecordingStatus.text = "● Recording detection: OFF"
            tvRecordingStatus.setTextColor(0xFF888888.toInt())
            Toast.makeText(ctx, "Screen recording detection STOPPED", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Blog (2): Screen recording callback removed")
            return
        }

        // Register callback to detect screen recording
        screenRecordingCallback = Consumer { state: Int ->
            val isVisible = state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
            if (isVisible) {
                // ── Mask BEFORE the recorder can capture the real values ──
                // maskSensitiveData() runs on the main thread so the UI update
                // is committed prior to the next frame being composited.
                runOnUiThread {
                    maskSensitiveData(true)
                    tvRecordingStatus.text = "🔴 RECORDING — data masked"
                    tvRecordingStatus.setTextColor(0xFFCC0000.toInt())
                    Toast.makeText(ctx, "Screen recording DETECTED! Sensitive data masked.", Toast.LENGTH_LONG).show()
                }
                Log.w(TAG, "Blog (2): SCREEN RECORDING DETECTED — sensitive data masked")
            } else {
                runOnUiThread {
                    maskSensitiveData(false)
                    tvRecordingStatus.text = "🟢 Recording stopped — data restored"
                    tvRecordingStatus.setTextColor(0xFF007700.toInt())
                    Toast.makeText(ctx, "Screen recording stopped. Data restored.", Toast.LENGTH_SHORT).show()
                }
                Log.i(TAG, "Blog (2): Screen recording stopped — sensitive data restored")
            }
        }

        windowManager.addScreenRecordingCallback(mainExecutor, screenRecordingCallback!!)
        tvRecordingStatus.text = "🟡 Recording detection: ON (watching…)"
        tvRecordingStatus.setTextColor(0xFF886600.toInt())
        Toast.makeText(ctx, "Screen recording detection STARTED.\nToggle again to stop.", Toast.LENGTH_LONG).show()
        Log.i(TAG, "Blog (2): Screen recording callback registered")
    }

    // =====================================================================
    // Blog (2): Sensitive-data masking helper
    //
    // When [mask] is true, replaces each sensitive field with '●' characters
    // so that no real value is visible in the recording frame buffer.
    // When [mask] is false, the original values are restored.
    //
    // Alternative (full-screen) approach: window.addFlags(FLAG_SECURE) makes
    // the *entire* window appear black in recordings — useful when masking
    // individual views is not feasible.
    // =====================================================================
    private fun maskSensitiveData(mask: Boolean) {
        if (mask) {
            tvEmployeeId.text = "EMP-●●●●●●"
            tvCreditCard.text = "●●●●-●●●●-●●●●-9012"   // last 4 kept (common UX pattern)
            tvPin.text        = "4-digit: ●●●●"
        } else {
            tvEmployeeId.text = REAL_EMPLOYEE_ID
            tvCreditCard.text = REAL_CREDIT_CARD
            tvPin.text        = REAL_PIN
        }
    }


    // =====================================================================
    // Blog (4): ApplicationStartInfo
    // Wiki: A15‐(4)‐Optimizing Performance & Background Behavior
    //
    // New API: ActivityManager.getHistoricalProcessStartReasons()
    // Returns a list of ApplicationStartInfo objects with rich telemetry:
    //   - Start type: COLD (new process), WARM (cached), HOT (resumed)
    //   - Reason: LAUNCHER, BROADCAST, SERVICE, ALARM, PUSH, etc.
    //   - Timestamps: LAUNCH, FORK, BIND_APPLICATION, FIRST_FRAME, etc.
    //
    // Blog (5) places this in Phase 3 (Medium-term) of the migration
    // timeline: "implement ApplicationStartInfo telemetry".
    //
    // Enterprise use: measure actual startup latency across device fleets.
    // =====================================================================
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_APP_START_INFO(v: View) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val startInfoList: List<ApplicationStartInfo> = am.getHistoricalProcessStartReasons(10)

        if (startInfoList.isEmpty()) {
            Toast.makeText(ctx, "No ApplicationStartInfo records found", Toast.LENGTH_SHORT).show()
            return
        }

        for (info in startInfoList) {
            val startType = when (info.startType) {
                ApplicationStartInfo.START_TYPE_COLD -> "COLD"
                ApplicationStartInfo.START_TYPE_WARM -> "WARM"
                ApplicationStartInfo.START_TYPE_HOT -> "HOT"
                else -> "UNKNOWN(${info.startType})"
            }

            val reason = when (info.reason) {
                ApplicationStartInfo.START_REASON_ALARM -> "ALARM"
                ApplicationStartInfo.START_REASON_BACKUP -> "BACKUP"
                ApplicationStartInfo.START_REASON_BOOT_COMPLETE -> "BOOT_COMPLETE"
                ApplicationStartInfo.START_REASON_BROADCAST -> "BROADCAST"
                ApplicationStartInfo.START_REASON_CONTENT_PROVIDER -> "CONTENT_PROVIDER"
                ApplicationStartInfo.START_REASON_JOB -> "JOB"
                ApplicationStartInfo.START_REASON_LAUNCHER -> "LAUNCHER"
                ApplicationStartInfo.START_REASON_OTHER -> "OTHER"
                ApplicationStartInfo.START_REASON_PUSH -> "PUSH"
                ApplicationStartInfo.START_REASON_SERVICE -> "SERVICE"
                ApplicationStartInfo.START_REASON_START_ACTIVITY -> "START_ACTIVITY"
                else -> "UNKNOWN(${info.reason})"
            }

            Log.i(TAG, """
                Blog (4): === ApplicationStartInfo ===
                Start Type: $startType
                Reason: $reason
                Launch Mode: ${info.launchMode}
                Process Name: ${info.processName}
                Timestamps entries: ${info.startupTimestamps.size}
            """.trimIndent())

            // Log each timestamp — these track the full startup pipeline
            for ((key, value) in info.startupTimestamps) {
                val tsName = when (key) {
                    ApplicationStartInfo.START_TIMESTAMP_LAUNCH -> "LAUNCH"
                    ApplicationStartInfo.START_TIMESTAMP_FORK -> "FORK"
                    ApplicationStartInfo.START_TIMESTAMP_APPLICATION_ONCREATE -> "APP_ONCREATE"
                    ApplicationStartInfo.START_TIMESTAMP_BIND_APPLICATION -> "BIND_APPLICATION"
                    ApplicationStartInfo.START_TIMESTAMP_FIRST_FRAME -> "FIRST_FRAME"
                    ApplicationStartInfo.START_TIMESTAMP_FULLY_DRAWN -> "FULLY_DRAWN"
                    ApplicationStartInfo.START_TIMESTAMP_INITIAL_RENDERTHREAD_FRAME -> "INITIAL_RENDERTHREAD_FRAME"
                    ApplicationStartInfo.START_TIMESTAMP_SURFACEFLINGER_COMPOSITION_COMPLETE -> "SURFACEFLINGER_COMPOSITION_COMPLETE"
                    else -> "KEY($key)"
                }
                Log.i(TAG, "  Timestamp $tsName: ${value}ns")
            }
        }

        Toast.makeText(ctx, "${startInfoList.size} start records logged. Check logcat.", Toast.LENGTH_LONG).show()
    }

    // =====================================================================
    // Blog (4): Detailed App Size Information
    // Wiki: A15‐(4)‐Optimizing Performance & Background Behavior
    //
    // API: StorageStatsManager.queryStatsForPackage()
    // Returns appBytes, dataBytes, cacheBytes for the package.
    //
    // Requires: android.permission.PACKAGE_USAGE_STATS (protected —
    // the user must grant it via Settings > Apps > Special app access >
    // Usage access).
    //
    // Enterprise use: audit installed app footprint across managed devices.
    // =====================================================================
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_APP_SIZE_INFO(v: View) {
        try {
            val storageStatsManager = getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid = StorageManager.UUID_DEFAULT

            val stats = storageStatsManager.queryStatsForPackage(
                uuid, packageName, android.os.Process.myUserHandle()
            )

            Log.i(TAG, """
                Blog (4): === Detailed App Size (API 35) ===
                App Bytes: ${stats.appBytes} (${stats.appBytes / 1024} KB)
                Data Bytes: ${stats.dataBytes} (${stats.dataBytes / 1024} KB)
                Cache Bytes: ${stats.cacheBytes} (${stats.cacheBytes / 1024} KB)
            """.trimIndent())

            Toast.makeText(
                ctx,
                "App: ${stats.appBytes / 1024}KB, Data: ${stats.dataBytes / 1024}KB, Cache: ${stats.cacheBytes / 1024}KB\nSee logcat.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: SecurityException) {
            Log.e(TAG, "Blog (4): Need PACKAGE_USAGE_STATS permission for detailed storage stats", e)
            Toast.makeText(ctx, "Grant Usage Access in Settings > Apps > Special access", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Blog (4): Error querying storage stats", e)
            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // =====================================================================
    // Blog (3): Text Features / i18n
    // Wiki: A15‐(3)‐Enhancing User Experience & Multitasking
    //
    // Launches TextFeaturesActivity which demonstrates:
    //   - Inter-character justification (JUSTIFICATION_MODE_INTER_CHARACTER)
    //   - LINE_BREAK_WORD_STYLE_AUTO (phrase-based line breaking)
    //   - elegantTextHeight (now true by default — prevents clipping of
    //     tall scripts like Thai, Myanmar, Tibetan)
    //   - CJK variable font support (NotoSansCJK)
    // =====================================================================
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_TEXT_FEATURES(v: View) {
        val intent = Intent(ctx, TextFeaturesActivity::class.java)
        startActivity(intent)
    }

    // =====================================================================
    // Blog (2): Safer Intents — Enhanced Intent Security
    // Wiki: A15‐(2)‐Next‐Generation Security & Privacy Controls
    //
    // Two new rules for apps targeting API 35:
    //   1. Explicit intents MUST accurately match the target component's
    //      intent-filter specifications.
    //   2. An Intent sent WITHOUT an action will NO LONGER match any
    //      intent-filter at all.
    //
    // This demo sends two intents:
    //   a) A correct intent with matching action → should resolve OK.
    //   b) An intent without action → should NOT resolve in API 35.
    //
    // Blog (2) also recommends StrictMode.detectUnsafeIntentLaunch()
    // for debugging unsafe intent patterns during development.
    // =====================================================================
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_SAFER_INTENTS(v: View) {
        // (a) Correct intent with matching action — should work
        try {
            val correctIntent = Intent("com.ndzl.a15_blog.SAFER_INTENT")
            correctIntent.addCategory(Intent.CATEGORY_DEFAULT)
            correctIntent.putExtra("source", "safer_intents_demo")
            startActivity(correctIntent)
            Log.i(TAG, "Blog (2): Safer Intents — correct intent delivered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Blog (2): Safer Intents — correct intent failed", e)
        }

        // (b) Intent WITHOUT action — in API 35 this no longer matches any filter
        try {
            val noActionIntent = Intent()
            noActionIntent.setPackage(packageName)
            val resolved = packageManager.resolveActivity(noActionIntent, 0)
            Log.i(TAG, "Blog (2): Safer Intents — no-action intent resolved to: " +
                    "${resolved?.activityInfo?.name ?: "NOTHING (expected in API 35)"}")
        } catch (e: Exception) {
            Log.e(TAG, "Blog (2): Safer Intents — no-action intent error", e)
        }

        Toast.makeText(ctx, "Safer Intents tested. Check logcat.", Toast.LENGTH_LONG).show()
    }

    // =====================================================================
    // Blog (4): Media Processing Foreground Service
    // Wiki: A15‐(4)‐Optimizing Performance & Background Behavior
    //
    // New FGS type: FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
    //   - Designed for transcoding, encoding, media file processing.
    //   - Has a 6-hour cumulative runtime limit per 24-hour window.
    //   - User interaction resets the timer completely.
    //   - System invokes Service.onTimeout(int startId) at the limit.
    //   - Non-compliant timeout handling triggers RemoteServiceException.
    //
    // Blog (4) also covers boot storm prevention:
    //   - This FGS type CANNOT be started from BOOT_COMPLETED receivers.
    //   - Attempting to do so throws ForegroundServiceStartNotAllowedException.
    //   - Use WorkManager for non-urgent background task scheduling.
    //
    // Requires POST_NOTIFICATIONS on API 33+ to display the FGS notification.
    // =====================================================================
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_MEDIA_PROCESSING_FGS(v: View) {
        // Check POST_NOTIFICATIONS permission (required on API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
                Toast.makeText(ctx, "Please grant notification permission first", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val intent = Intent(ctx, MediaProcessingFGS::class.java)
        startForegroundService(intent)
        Toast.makeText(ctx, "MediaProcessing FGS started.\n6h timeout in API 35.", Toast.LENGTH_LONG).show()
        Log.i(TAG, "Blog (4): Started mediaProcessing foreground service")
    }

    // =====================================================================
    // Blog (4): App-Managed Profiling
    // Wiki: A15‐(4)‐Optimizing Performance & Background Behavior
    //
    // New API: ProfilingManager.requestProfiling()
    // Allows apps to request profiling data (heap dumps, stack sampling)
    // in production without external tooling.
    //
    // Types available: PROFILING_TYPE_JAVA_HEAP_DUMP, and others.
    // Results are delivered asynchronously via a callback.
    //
    // Blog (5) places this in Phase 3 (Medium-term) of the migration
    // timeline: "leverage improved JobScheduler capabilities".
    // =====================================================================
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_PROFILING(v: View) {
        try {
            @Suppress("ServiceCast")
            val profilingManager = getSystemService("profiling") as android.os.ProfilingManager

            Log.i(TAG, "Blog (4): ProfilingManager obtained. Output dir: ${cacheDir.absolutePath}")

            profilingManager.requestProfiling(
                android.os.ProfilingManager.PROFILING_TYPE_JAVA_HEAP_DUMP,
                null, // params bundle
                null, // tag
                null, // cancellation signal
                mainExecutor
            ) { result ->
                Log.i(TAG, "Blog (4): Profiling result received: $result")
                runOnUiThread {
                    Toast.makeText(ctx, "Profiling result: $result", Toast.LENGTH_LONG).show()
                }
            }

            Toast.makeText(ctx, "Heap dump profiling requested.\nCheck logcat.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Blog (4): Profiling error", e)
            Toast.makeText(ctx, "Profiling error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // =====================================================================
    // Blog (2): Private Space
    // Wiki: A15‐(2)‐Next‐Generation Security & Privacy Controls
    //
    // Private Space is a new isolated user profile in Android 15:
    //   - Exists only in the personal (primary) profile, not in Work Profile.
    //   - On BYOD devices: user-controlled, IT admins cannot restrict.
    //   - On COPE devices: IT admins can block creation via
    //     UserManager.DISALLOW_ADD_PRIVATE_PROFILE.
    //   - On fully managed devices: Private Space is unavailable.
    //
    // This demo queries the UserManager to show the current user type
    // and checks if the private profile restriction is set.
    // =====================================================================
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_PRIVATE_SPACE(v: View) {
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager

        // Check if adding a private profile is restricted (COPE/managed devices)
        val isPrivateProfileRestricted = userManager.hasUserRestriction(
            UserManager.DISALLOW_ADD_PRIVATE_PROFILE
        )

        // Check if this is a managed profile (Work Profile)
        val isManagedProfile = userManager.isManagedProfile

        Log.i(TAG, """
            Blog (2): === Private Space Info ===
            Is managed profile (Work Profile): $isManagedProfile
            DISALLOW_ADD_PRIVATE_PROFILE restriction: $isPrivateProfileRestricted

            Notes:
            - Private Space exists only in the personal profile
            - BYOD: user-controlled, IT cannot restrict
            - COPE: IT can block via DISALLOW_ADD_PRIVATE_PROFILE
            - Fully managed: Private Space unavailable
        """.trimIndent())

        Toast.makeText(
            ctx,
            "Managed profile: $isManagedProfile\nPrivate profile restricted: $isPrivateProfileRestricted\nSee logcat.",
            Toast.LENGTH_LONG
        ).show()
    }

    // =====================================================================
    // Blog (3): Large Screen & Multi-Window Support
    // Wiki: A15‐(3)‐Enhancing User Experience & Multitasking
    //
    // Android 15 enhances large-screen multitasking for tablets and
    // enterprise devices:
    //   - Permanent taskbar pinning for rapid app switching.
    //   - Saved split-screen app combinations.
    //   - PC-like multitasking capabilities.
    //
    // Developer requirement: apps must be truly adaptive, handling
    // full-screen, split-screen, and windowed modes seamlessly.
    // Use responsive design with Material 3 adaptive libraries.
    //
    // This demo reports the current multi-window state and screen size.
    // =====================================================================
    @Suppress("UNUSED_PARAMETER")
    fun onClickbtn_LARGE_SCREEN(v: View) {
        val isMultiWindow = isInMultiWindowMode
        val isPictureInPicture = isInPictureInPictureMode

        val config = resources.configuration
        val screenLayout = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val screenSize = when (screenLayout) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> "SMALL"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> "NORMAL"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> "LARGE"
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> "XLARGE"
            else -> "UNDEFINED($screenLayout)"
        }

        Log.i(TAG, """
            Blog (3): === Large Screen / Multi-Window ===
            Multi-window mode: $isMultiWindow
            Picture-in-picture mode: $isPictureInPicture
            Screen size class: $screenSize
            Screen width dp: ${config.screenWidthDp}
            Screen height dp: ${config.screenHeightDp}
            Smallest width dp: ${config.smallestScreenWidthDp}

            Blog (3) recommends:
            - Apps must handle full-screen, split-screen, and windowed modes
            - Use Material 3 adaptive libraries for responsive design
            - Test on tablets (ET401, ET4x) and foldables
        """.trimIndent())

        Toast.makeText(
            ctx,
            "Multi-window: $isMultiWindow\nScreen: $screenSize (${config.screenWidthDp}x${config.screenHeightDp}dp)\nSee logcat.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up screen recording callback to prevent leaks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && screenRecordingCallback != null) {
            windowManager.removeScreenRecordingCallback(screenRecordingCallback!!)
        }
    }
}
