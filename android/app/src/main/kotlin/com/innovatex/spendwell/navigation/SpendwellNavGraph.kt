package com.innovatex.spendwell.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
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
import kotlinx.coroutines.launch

@Composable
fun SpendwellApp(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    aiAdvisorEngine: SpendwellAiAdvisorEngine,
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController()
) {
    val isOnboardingCompleted by userPreferencesRepository.isOnboardingCompleted.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    // Show blank background while loading preference state to prevent flickering
    if (isOnboardingCompleted == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {}
        return
    }

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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SpendwellBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { screen ->
                        if (screen.route == Screen.Dashboard.route) {
                            navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                        } else {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingCompleted == true) Screen.Dashboard.route else Screen.Onboarding.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = { name, cycleDay ->
                        coroutineScope.launch {
                            if (name.isNotBlank()) {
                                userPreferencesRepository.setUserName(name)
                            }
                            userPreferencesRepository.setCycleStartDay(cycleDay)
                        }
                        navController.navigate(Screen.PrivacyMode.route)
                    }
                )
            }

            composable(Screen.PrivacyMode.route) {
                PrivacyModeScreen(
                    onContinue = { isPrivate ->
                        coroutineScope.launch {
                            userPreferencesRepository.setPrivateMode(isPrivate)
                            userPreferencesRepository.setOnboardingCompleted(true)
                        }
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
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionFeedScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToDetail = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                AnalyticsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Budgets.route) {
                BudgetManagementScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Advisor.route) {
                val currentUserName by userPreferencesRepository.userName.collectAsState(initial = "")
                AiAdvisorScreen(
                    engine = aiAdvisorEngine,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToModelManagement = { navController.navigate(Screen.AiModel.route) },
                    userName = currentUserName
                )
            }

            composable(Screen.ReviewCenter.route) {
                ReviewCenterScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Recurring.route) {
                RecurringScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Reconciliation.route) {
                ReconciliationScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AiModel.route) {
                AiModelManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Permissions.route) {
                PermissionManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SavingsGoals.route) {
                SavingsGoalsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToModelManagement = { navController.navigate(Screen.AiModel.route) },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateBack = { navController.popBackStack() }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = CardSurfaceWhite.copy(alpha = 0.95f),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val activeBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                val activeTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToDestination(screen) }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(activeBg)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        screen.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = activeTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = fontWeight,
                        color = activeTint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
