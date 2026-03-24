package com.ndzl.a15_directboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log

/**
 * Direct Boot-aware receiver.
 *
 * Handles two distinct boot phases:
 *
 * 1) LOCKED_BOOT_COMPLETED — device has booted but the user has NOT entered PIN/password yet.
 *    Only Device Encrypted (DE) storage is available. CE storage is locked.
 *    The FGS starts here in "pre-unlock" mode with limited capabilities.
 *
 * 2) BOOT_COMPLETED — user has entered PIN/password, CE storage is now available.
 *    The FGS transitions to "post-unlock" mode with full capabilities.
 *
 * Android 15 note:
 *   A15 blocks several FGS types from starting at BOOT_COMPLETED:
 *   dataSync, camera, mediaPlayback, phoneCall, location, microphone,
 *   mediaProcessing, connectedDevice. Our service uses `specialUse`,
 *   which is allowed.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "A15-DirectBoot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val isUserUnlocked = userManager.isUserUnlocked

        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.w(TAG, "━━━ LOCKED_BOOT_COMPLETED ━━━")
                Log.w(TAG, "Device booted but user has NOT entered PIN/password yet")
                Log.w(TAG, "CE storage locked: ${ !isUserUnlocked }")
                Log.w(TAG, "Starting FGS in pre-unlock (DE-only) mode...")

                startService(context, preUnlock = true)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.w(TAG, "━━━ BOOT_COMPLETED ━━━")
                Log.w(TAG, "User has entered PIN/password — full storage available")
                Log.w(TAG, "CE storage unlocked: $isUserUnlocked")
                Log.w(TAG, "Transitioning FGS to post-unlock (full) mode...")

                startService(context, preUnlock = false)
            }
        }
    }

    private fun startService(context: Context, preUnlock: Boolean) {
        val serviceIntent = Intent(context, DirectBootForegroundService::class.java).apply {
            putExtra(DirectBootForegroundService.EXTRA_PRE_UNLOCK, preUnlock)
        }
        // On API 26+ we must use startForegroundService for FGS
        context.startForegroundService(serviceIntent)
    }
}
