package com.innovatex.spendwell

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.innovatex.spendwell.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SpendwellApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher native binaries for encrypted database
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            // Handled gracefully in JVM unit test environments where native binaries are absent
        }

        // Seed standard default categories on first launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                categoryRepository.initializeDefaultCategories()
            } catch (e: Exception) {
                // Ignore initialization race conditions
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
