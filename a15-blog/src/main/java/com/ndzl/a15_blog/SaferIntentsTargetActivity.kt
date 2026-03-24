package com.ndzl.a15_blog

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Blog (2): Safer Intents — Target Activity
 * Wiki: A15‐(2)‐Next‐Generation Security & Privacy Controls
 *
 * This activity serves as the **target** for the safer intents demonstration.
 * It declares an intent-filter in AndroidManifest.xml with:
 *   <action android:name="com.ndzl.a15_blog.SAFER_INTENT" />
 *   <category android:name="android.intent.category.DEFAULT" />
 *
 * In API 35, Android enforces two new intent-resolution rules:
 *
 *   1. Explicit intents must ACCURATELY match the component's intent-filter.
 *      Mismatches that silently worked before now fail.
 *
 *   2. An intent WITHOUT an action no longer matches ANY intent-filter.
 *      This closes a loophole where empty intents could resolve to unexpected
 *      components.
 *
 * Blog (2) also recommends using StrictMode.detectUnsafeIntentLaunch()
 * during development to catch unsafe intent patterns:
 *
 *   StrictMode.setVmPolicy(
 *       StrictMode.VmPolicy.Builder()
 *           .detectUnsafeIntentLaunch()
 *           .penaltyLog()
 *           .build()
 *   )
 *
 * Enterprise impact: prevents malicious apps from intercepting enterprise
 * intents through loose intent-filter matching.
 */
class SaferIntentsTargetActivity : AppCompatActivity() {

    private val TAG = "A15-Blog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val source = intent.getStringExtra("source") ?: "unknown"
        val action = intent.action ?: "no action"

        Log.i(TAG, """
            Blog (2): SaferIntentsTargetActivity launched!
            Action: $action
            Source: $source
            API 35 enforces: intents must match declared intent-filter.
            Intents without an action NO LONGER match any filter.
        """.trimIndent())

        Toast.makeText(this, "SaferIntentsTarget received intent\nAction: $action", Toast.LENGTH_LONG).show()

        // Finish immediately — this is a demonstration target, not a UI destination
        finish()
    }
}
