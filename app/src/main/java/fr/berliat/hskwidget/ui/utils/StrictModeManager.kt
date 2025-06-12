package fr.berliat.hskwidget.ui.utils

import android.os.Build
import android.os.StrictMode
import android.os.strictmode.Violation
import android.util.Log
import fr.berliat.hskwidget.BuildConfig
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Helper for enabling strict mode, with a whitelist of a particular error.
 * Based on https://medium.com/@tokudu/how-to-whitelist-strictmode-violations-on-android-based-on-stacktrace-eb0018e909aa
 */
class StrictModeManager {
    companion object {
        private const val TAG = "StrictModeManager"

        /**
         * Array of whitelisted stacktraces. If the violation stack trace contains any of these lines, the violations
         * are ignored.
         */
        private val STACKTRACE_WHITELIST = listOf(
            // This violation is related to Dex Loading optimization on Snapdragon devices.
            "android.widget.OverScroller.<init>"
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
            val builder = StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDropBox()

            // allow penaltyDeath on versions above N
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.penaltyDeath()
            }

            StrictMode.setThreadPolicy(builder.build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .penaltyDropBox()
                .penaltyDeath()
                .build())

            // Let the dirty hacks begin.
            // Source: https://atscaleconference.com/videos/eliminating-long-tail-jank-with-strictmode/
            // On API levels above N, we can use reflection to read the violationsBeingTimed field of strict
            // to get notifications about reported violations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val field = StrictMode::class.java.getDeclaredField("violationsBeingTimed")
                field.setAccessible(true) // Suppress Java language access checking
                // Remove "final" modifier
                val modifiersField = Field::class.java.getDeclaredField("accessFlags")
                modifiersField.setAccessible(true)
                modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                // Override the field with a custom ArrayList, which is able to skip whitelisted violations
                field.set(null, object: ThreadLocal<ArrayList<out Object>>() {
                    override fun get(): ArrayList<out Object> {
                        return StrictModeHackArrayList()
                    }
                })
            }
        }
    }

    /**
     * Special array list that skip additions for matching ViolationInfo instances as per
     * hack described in https://atscaleconference.com/videos/eliminating-long-tail-jank-with-strictmode/
     */
    class StrictModeHackArrayList: ArrayList<Object>() {
        override fun add(element: Object): Boolean {
            val crashInfoField = element.`class`.getDeclaredField("mViolation")
            val fField = getViolationField(element)

            fField?.stackTrace?.forEach {
                for (whitelistedStacktraceCall in STACKTRACE_WHITELIST) {
                    if (it.toString().contains(whitelistedStacktraceCall)) {
                        Log.d(
                            TAG,
                            "Skipping whitelisted StrictMode violation: $whitelistedStacktraceCall"
                        )
                        return false
                    }
                }
            }
            // call super to continue with standard violation reporting
            return super.add(element)
        }

        fun getViolationField(violationInfo: Object): Violation? {
            return try {
                val violationInfoClass = violationInfo.javaClass // or Class.forName("android.os.StrictMode$ViolationInfo")
                val mViolationField = violationInfoClass.getDeclaredField("mViolation")
                mViolationField.isAccessible = true  // bypass private access
                val field = mViolationField.get(violationInfo)
                field as Violation
            } catch (e: Throwable) {
                // Reflection failed — field missing or inaccessible
                e.printStackTrace()
                null
            }
        }
    }
}