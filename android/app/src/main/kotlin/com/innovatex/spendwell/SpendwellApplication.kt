package com.innovatex.spendwell

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.innovatex.spendwell.database.SpendwellDatabase
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

    @Inject
    lateinit var databaseProvider: Provider<SpendwellDatabase>

    override fun onCreate() {
        super.onCreate()

        // Capture crash details locally in app-private storage and launch diagnostic recovery screen
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                val crashFile = File(filesDir, "latest_crash.log")
                crashFile.writeText("CRASH on thread [${thread.name}]:\n$stackTrace")
                
                val prefs = getSharedPreferences("spendwell_crash_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_crash_thread", thread.name)
                    .putString("last_crash_message", throwable.message ?: throwable.javaClass.simpleName)
                    .putString("last_crash_stacktrace", stackTrace)
                    .putLong("last_crash_timestamp", System.currentTimeMillis())
                    .commit()

                Log.e("SpendwellApp", "FATAL EXCEPTION in thread ${thread.name}", throwable)

                val intent = Intent(this@SpendwellApplication, CrashDiagnosticActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("EXTRA_STACKTRACE", stackTrace)
                    putExtra("EXTRA_MESSAGE", throwable.message ?: throwable.javaClass.simpleName)
                    putExtra("EXTRA_THREAD", thread.name)
                }
                startActivity(intent)

                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (ignored: Throwable) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }


        // Seed standard default categories asynchronously on first launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                categoryRepositoryProvider.get().initializeDefaultCategories()
            } catch (e: Throwable) {
                Log.w("SpendwellApp", "Category taxonomy initialization error: ${e.message}")
            }
        }
    }
}
