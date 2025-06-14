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
            // This violation is related to Dex Loading optimization on Snapdragon devices.
            "android.widget.OverScroller.<init>",
            "com.yalantis.ucrop.UCropActivity.cropAndSaveImage"
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
        }

        private fun onVmViolation(v: Violation?) {
            if (v == null) return

            v.stackTrace.forEach {
                for (whitelistedStacktraceCall in STACKTRACE_WHITELIST) {
                    if (it.toString().contains(whitelistedStacktraceCall)) {
                        Log.d(
                            TAG,
                            "Skipping whitelisted StrictMode violation: $whitelistedStacktraceCall"
                        )
                        return
                    }
                }
            }

            throw RuntimeException("StrictMode Violation: ${v.stackTrace}")
        }
    }
}