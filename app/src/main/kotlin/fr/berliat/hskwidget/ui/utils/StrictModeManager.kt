package fr.berliat.hskwidget.ui.utils

import android.os.Build
import android.os.StrictMode
import android.os.strictmode.Violation
import android.util.Log
import androidx.annotation.RequiresApi
import fr.berliat.hskwidget.BuildConfig
import java.util.concurrent.Executors

/**
 * Helper for enabling strict mode, with a whitelist of a particular error.
 * Based on https://medium.com/@tokudu/how-to-whitelist-strictmode-violations-on-android-based-on-stacktrace-eb0018e909aa
 */
@RequiresApi(Build.VERSION_CODES.P)
class StrictModeManager {
    companion object {
        private const val TAG = "StrictModeManager"

        /**
         * Array of whitelisted stacktraces. If the violation stack trace contains any of these lines, the violations
         * are ignored.
         */
        private val STACKTRACE_WHITELIST = listOf(
            "android.widget.OverScroller.<init>", // Triggered in NavDrawer code
            "android.graphics.AwareBitmapCacher.-\$\$Nest\$mhandleCheckBgAndRelease", // Triggered at resume by OS?
            "android.hwtheme.HwThemeManager.getDataSkinThemePackages" // Triggered when Billing activities are summoned
        )

        /**
         * Enables strict mode if necessary based on the build config.
         */
        @JvmStatic
        fun init() {
            if (BuildConfig.DEBUG) {
                enableStrictMode()
            }
        }

        private fun enableStrictMode() {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDropBox()
                .penaltyListener(Executors.newSingleThreadExecutor()) { v -> onVmViolation(v) }
                .build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .penaltyDropBox()
                .penaltyListener(Executors.newSingleThreadExecutor()) { v -> onVmViolation(v) }
                .build())

            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                if (throwable is Violation) onVmViolation(throwable)
            }
        }

        private fun onVmViolation(violation: Violation?) {
            if (violation == null) return

            violation.stackTrace.forEach {
                val method = "${it.className}.${it.methodName}"
                if (STACKTRACE_WHITELIST.contains(method)) {
                    Log.d(
                        TAG,
                        "Skipping whitelisted StrictMode violation: $method"
                    )
                    return
                }
            }

            throw violation
        }
    }
}