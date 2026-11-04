package com.innovatex.spendwell

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.innovatex.spendwell.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class SpendwellApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var categoryRepositoryProvider: Provider<CategoryRepository>

    override fun onCreate() {
        super.onCreate()

        // Capture crash details locally in app-private storage for diagnosing any runtime crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashFile = File(filesDir, "latest_crash.log")
                crashFile.writeText("CRASH on thread [${thread.name}]:\n" + Log.getStackTraceString(throwable))
                Log.e("SpendwellApp", "FATAL EXCEPTION in thread ${thread.name}", throwable)
            } catch (ignored: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize SQLCipher native binaries for encrypted database
        try {
            System.loadLibrary("sqlcipher")
        } catch (t: Throwable) {
            Log.w("SpendwellApp", "SQLCipher native library load non-fatal warning: ${t.message}")
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().apply {
            if (::workerFactory.isInitialized) {
                setWorkerFactory(workerFactory)
            }
        }.build()
}
