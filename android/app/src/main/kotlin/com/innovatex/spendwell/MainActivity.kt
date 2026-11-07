package com.innovatex.spendwell

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.designsystem.theme.SpendwellTheme
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.navigation.SpendwellApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var aiAdvisorEngine: SpendwellAiAdvisorEngine

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If app crashed recently (within last 15 seconds), open the diagnostic screen instead of looping
        val crashPrefs = getSharedPreferences("spendwell_crash_prefs", Context.MODE_PRIVATE)
        val lastCrashTime = crashPrefs.getLong("last_crash_timestamp", 0L)
        val lastCrashTrace = crashPrefs.getString("last_crash_stacktrace", null)

        if (lastCrashTrace != null && (System.currentTimeMillis() - lastCrashTime < 15_000)) {
            val intent = Intent(this, CrashDiagnosticActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_STACKTRACE", lastCrashTrace)
                putExtra("EXTRA_MESSAGE", crashPrefs.getString("last_crash_message", "Recent crash detected"))
                putExtra("EXTRA_THREAD", crashPrefs.getString("last_crash_thread", "main"))
            }
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            SpendwellTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpendwellApp(
                        transactionRepository = transactionRepository,
                        categoryRepository = categoryRepository,
                        aiAdvisorEngine = aiAdvisorEngine,
                        userPreferencesRepository = userPreferencesRepository
                    )
                }
            }
        }
    }
}
