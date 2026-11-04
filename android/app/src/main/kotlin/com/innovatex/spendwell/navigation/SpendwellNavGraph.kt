package com.innovatex.spendwell.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.features.accounts.AccountsScreen
import com.innovatex.spendwell.features.aiadvisor.AiAdvisorScreen
import com.innovatex.spendwell.features.aimodel.AiModelManagementScreen
import com.innovatex.spendwell.features.analytics.AnalyticsScreen
import com.innovatex.spendwell.features.budgets.BudgetManagementScreen
import com.innovatex.spendwell.features.dashboard.DashboardScreen
import com.innovatex.spendwell.features.onboarding.OnboardingScreen
import com.innovatex.spendwell.features.permissions.PermissionManagementScreen
import com.innovatex.spendwell.features.privacymode.PrivacyModeScreen
import com.innovatex.spendwell.features.reconciliation.ReconciliationScreen
import com.innovatex.spendwell.features.recurring.RecurringScreen
import com.innovatex.spendwell.features.reviewcenter.ReviewCenterScreen
import com.innovatex.spendwell.features.savingsgoals.SavingsGoalsScreen
import com.innovatex.spendwell.features.settings.SettingsScreen
import com.innovatex.spendwell.features.transactiondetail.TransactionDetailScreen
import com.innovatex.spendwell.features.transactions.TransactionFeedScreen

@Composable
fun SpendwellApp(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    aiAdvisorEngine: SpendwellAiAdvisorEngine,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Analytics.route,
        Screen.Budgets.route,
        Screen.Advisor.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SpendwellBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Screen.PrivacyMode.route)
                    }
                )
            }

            composable(Screen.PrivacyMode.route) {
                PrivacyModeScreen(
                    onContinue = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToTransactionDetail = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                    onNavigateToReviewCenter = { navController.navigate(Screen.ReviewCenter.route) },
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToReconciliation = { navController.navigate(Screen.Reconciliation.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionFeedScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToDetail = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) }
                )
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getString("transactionId") ?: ""
                TransactionDetailScreen(
                    transactionId = txId,
                    transactionRepository = transactionRepository,
                    categoryRepository = categoryRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Budgets.route) {
                BudgetManagementScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Advisor.route) {
                AiAdvisorScreen(engine = aiAdvisorEngine)
            }

            composable(Screen.ReviewCenter.route) {
                ReviewCenterScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Recurring.route) {
                RecurringScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Reconciliation.route) {
                ReconciliationScreen(viewModel = hiltViewModel())
            }

            composable(Screen.AiModel.route) {
                AiModelManagementScreen()
            }

            composable(Screen.Permissions.route) {
                PermissionManagementScreen()
            }

            composable(Screen.SavingsGoals.route) {
                SavingsGoalsScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToModelManagement = { navController.navigate(Screen.AiModel.route) },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
                )
            }
        }
    }
}

@Composable
private fun SpendwellBottomBar(
    currentRoute: String?,
    onNavigateToDestination: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(PillShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screen.bottomNavItems.filterNotNull().forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val activeBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(activeBg)
                            .clickable { onNavigateToDestination(screen) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        screen.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
