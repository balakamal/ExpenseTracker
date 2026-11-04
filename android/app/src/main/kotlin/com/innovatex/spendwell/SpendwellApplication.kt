package com.innovatex.spendwell

import android.app.Application
import android.content.Context
import android.util.Log
import com.innovatex.spendwell.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class SpendwellApplication : Application() {

    @Inject
    lateinit var categoryRepositoryProvider: Provider<CategoryRepository>

    override fun onCreate() {
        super.onCreate()

        // Capture crash details locally in app-private storage for diagnosing any runtime crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                val crashFile = File(filesDir, "latest_crash.log")
                crashFile.writeText("CRASH on thread [${thread.name}]:\n$stackTrace")
                
                // Also persist in shared preferences for in-app recovery diagnostics
                val prefs = getSharedPreferences("spendwell_crash_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_crash_thread", thread.name)
                    .putString("last_crash_message", throwable.message ?: "Unknown error")
                    .putString("last_crash_stacktrace", stackTrace)
                    .putLong("last_crash_timestamp", System.currentTimeMillis())
                    .apply()

                Log.e("SpendwellApp", "FATAL EXCEPTION in thread ${thread.name}", throwable)
            } catch (ignored: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }


        // Seed standard default categories asynchronously on first launch without blocking startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                categoryRepositoryProvider.get().initializeDefaultCategories()
            } catch (e: Throwable) {
                Log.w("SpendwellApp", "Category initialization non-fatal error: ${e.message}")
            }
        }
    }
}
