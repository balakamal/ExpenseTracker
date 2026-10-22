# AppWidget & Glance State Contract

**Feature**: `001-local-expense-tracker`  
**Type**: Android Glance Home Screen Widget Contract

---

## 1. Widget Types & Layout Contracts

| Widget Size | Dimensions | Displayed Information | Supported Actions |
| :--- | :--- | :--- | :--- |
| **Small (2x2)** | 110dp x 110dp | Current Month Total Debit, Budget Progress Ring, Currency Symbol. | Tap: Open Current Month View. |
| **Medium (4x2)** | 250dp x 110dp | Month Total Debit, Remaining Budget, Progress Bar, Top 2 Spending Categories with color indicators. | Tap Left: Open Month Breakdown.<br>Tap Right: Quick Add Expense. |
| **Large (4x4)** | 250dp x 250dp | Full FinTech Card: Total Debit & Credit, Net Velocity, Category Distribution Pill, Last 3 Transactions with merchant names & amounts. | Tap Header: Open App.<br>Tap Transaction: Open Transaction Details. |

---

## 2. Glance Widget State Definition

```kotlin
data class ExpenseWidgetState(
    val monthLabel: String,         // "October 2026"
    val totalDebitFormatted: String, // "$1,248.50"
    val totalCreditFormatted: String, // "$3,800.00"
    val remainingBudgetFormatted: String, // "$751.50"
    val budgetProgressPercent: Int,  // 62 (for 62%)
    val isOverBudget: Boolean,       // true if progress > 100%
    val topCategories: List<WidgetCategoryItem>,
    val recentTransactions: List<WidgetTransactionItem>,
    val lastSyncTimestamp: Long
)

data class WidgetCategoryItem(
    val name: String,
    val amountFormatted: String,
    val colorHex: String
)

data class WidgetTransactionItem(
    val merchant: String,
    val amountFormatted: String,
    val type: TransactionType,
    val categoryIconName: String,
    val timeFormatted: String
)
```

---

## 3. Widget Update Trigger Contract

Widgets MUST be refreshed via local broadcast or Glance state update:

```kotlin
interface ExpenseWidgetSyncManager {
    /**
     * Called whenever a new transaction is inserted, updated, or deleted.
     * Triggers asynchronous GlanceAppWidget.update() across all active instances.
     */
    suspend fun notifyDataChanged(context: Context)
}
```

- **Broadcast Action**: `com.innovatex.expensetracker.ACTION_WIDGET_DATA_CHANGED`
- **Latency Requirement**: Widget display MUST update within 3 seconds of a database change (Constitution Principle I & SC-003).
- **Battery Protection**: Debounced to avoid multiple renders if multiple SMS messages arrive simultaneously.
