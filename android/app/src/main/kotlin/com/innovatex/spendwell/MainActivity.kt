package com.innovatex.spendwell

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.database.SpendwellDatabase
import com.innovatex.spendwell.designsystem.theme.DeepCharcoal
import com.innovatex.spendwell.designsystem.theme.SpendwellTheme
import com.innovatex.spendwell.designsystem.theme.VividMint
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.navigation.SpendwellApp
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
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

        try {
            setContent {
                SpendwellTheme {
                    var fatalError by remember { mutableStateOf<String?>(null) }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (fatalError != null) {
                            SpendwellRecoveryScreen(
                                errorMessage = fatalError ?: "Unknown error",
                                onResetData = { resetAppData() },
                                onRetry = { recreate() }
                            )
                        } else {
                            try {
                                SpendwellApp(
                                    transactionRepository = transactionRepository,
                                    categoryRepository = categoryRepository,
                                    aiAdvisorEngine = aiAdvisorEngine
                                )
                            } catch (t: Throwable) {
                                Log.e("MainActivity", "Error during SpendwellApp composition", t)
                                fatalError = t.message ?: t.javaClass.simpleName
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "Fatal error during setContent", t)
            setContent {
                SpendwellRecoveryScreen(
                    errorMessage = t.message ?: t.javaClass.simpleName,
                    onResetData = { resetAppData() },
                    onRetry = { recreate() }
                )
            }
        }
    }

    private fun resetAppData() {
        try {
            deleteDatabase(SpendwellDatabase.DATABASE_NAME)
            val encPass = File(filesDir, "spendwell_db_passphrase.enc")
            val ivPass = File(filesDir, "spendwell_db_passphrase.iv")
            val fallbackKey = File(filesDir, "spendwell_db_fallback.key")
            encPass.delete()
            ivPass.delete()
            fallbackKey.delete()
        } catch (ignored: Throwable) {}

        // Restart activity
        val intent = intent
        finish()
        startActivity(intent)
    }
}

@Composable
fun SpendwellRecoveryScreen(
    errorMessage: String,
    onResetData: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepCharcoal
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Spendwell Recovery",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = VividMint,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "The application encountered an issue while initializing the encrypted storage.",
                fontSize = 14.sp,
                color = Color(0xFFA0B5AF),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18222A), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onResetData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VividMint),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Reset Database & Restart",
                    color = DeepCharcoal,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Try Again",
                    color = Color.White
                )
            }
        }
    }
}
