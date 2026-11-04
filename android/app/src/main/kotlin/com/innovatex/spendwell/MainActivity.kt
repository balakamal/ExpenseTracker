package com.innovatex.spendwell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpendwellTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpendwellApp(
                        transactionRepository = transactionRepository,
                        categoryRepository = categoryRepository,
                        aiAdvisorEngine = aiAdvisorEngine
                    )
                }
            }
        }
    }
}
