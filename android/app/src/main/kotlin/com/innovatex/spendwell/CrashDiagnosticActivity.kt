package com.innovatex.spendwell

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.database.SpendwellDatabase

class CrashDiagnosticActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("spendwell_crash_prefs", Context.MODE_PRIVATE)
        val stackTrace = intent.getStringExtra("EXTRA_STACKTRACE")
            ?: prefs.getString("last_crash_stacktrace", "No stack trace available.") ?: "No stack trace available."
        val message = intent.getStringExtra("EXTRA_MESSAGE")
            ?: prefs.getString("last_crash_message", "Unknown runtime exception") ?: "Unknown runtime exception"
        val threadName = intent.getStringExtra("EXTRA_THREAD")
            ?: prefs.getString("last_crash_thread", "main") ?: "main"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F141C)
                ) {
                    CrashDiagnosticScreen(
                        message = message,
                        threadName = threadName,
                        stackTrace = stackTrace,
                        onCopyLog = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Spendwell Crash Log", stackTrace)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Crash log copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onRestartApp = {
                            prefs.edit().clear().apply()
                            val restartIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(restartIntent)
                            finish()
                        },
                        onResetDatabaseAndRestart = {
                            try {
                                deleteDatabase(SpendwellDatabase.DATABASE_NAME)
                                deleteDatabase("spendwell_encrypted.db")
                            } catch (ignored: Throwable) {}
                            prefs.edit().clear().apply()
                            val restartIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(restartIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CrashDiagnosticScreen(
    message: String,
    threadName: String,
    stackTrace: String,
    onCopyLog: () -> Unit,
    onRestartApp: () -> Unit,
    onResetDatabaseAndRestart: () -> Unit
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Error Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2D1518))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Spendwell Diagnostic Reporter",
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A runtime exception occurred on thread [$threadName]. Details below:",
                    color = Color(0xFFE2A0A0),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable Log Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161F2B))
                .padding(12.dp)
        ) {
            Text(
                text = stackTrace,
                color = Color(0xFFB0C5E0),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp,
                modifier = Modifier
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Button(
            onClick = onCopyLog,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF159B70)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Copy Error Log", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onResetDatabaseAndRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset Data", color = Color(0xFFFF8B8B), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onRestartApp,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26384D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Restart App", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
