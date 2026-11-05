package com.innovatex.spendwell.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Welcome")
    object PrivacyMode : Screen("privacy_mode", "Privacy Mode")
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.ReceiptLong)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.PieChart)
    object Budgets : Screen("budgets", "Budgets", Icons.Default.Savings)
    object Advisor : Screen("advisor", "AI Advisor", Icons.Default.AutoAwesome)

    object TransactionDetail : Screen("transaction_detail/{transactionId}", "Transaction Details") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }

    object ReviewCenter : Screen("review_center", "Review Center")
    object Accounts : Screen("accounts", "Accounts & Cards")
    object Recurring : Screen("recurring", "Recurring Payments")
    object Reconciliation : Screen("reconciliation", "Reconciliation")
    object AiModel : Screen("ai_model", "AI Model Management")
    object Permissions : Screen("permissions", "Permissions")
    object SavingsGoals : Screen("savings_goals", "Savings Goals")
    object Settings : Screen("settings", "Settings")

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Transactions,
                Analytics,
                Advisor,
                Budgets
            )
    }
}
