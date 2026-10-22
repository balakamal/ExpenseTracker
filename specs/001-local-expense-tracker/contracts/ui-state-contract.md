# MVI UI State & Interaction Contract

**Feature**: `001-local-expense-tracker`  
**Type**: Unidirectional Data Flow (MVI) Contract

---

## 1. Dashboard MVI Contract

### 1.1 Intent Actions (User Events)
```kotlin
sealed interface DashboardIntent {
    data class SelectMonth(val monthKey: String) : DashboardIntent
    data class ToggleChannel(val channel: SourceChannel, val enabled: Boolean) : DashboardIntent
    data object TriggerHistoricalScan : DashboardIntent
    data class ReassignCategory(val transactionId: String, val newCategoryId: String, val createMerchantRule: Boolean) : DashboardIntent
    data class DeleteTransaction(val transactionId: String) : DashboardIntent
    data class SearchQueryChanged(val query: String) : DashboardIntent
    data class FilterByCategory(val categoryId: String?) : DashboardIntent
    data object RefreshData : DashboardIntent
}
```

### 1.2 Side Effects (Single-Event Dispatches)
```kotlin
sealed interface DashboardSideEffect {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : DashboardSideEffect
    data class NavigateToTransactionDetail(val transactionId: String) : DashboardSideEffect
    data object NavigateToSettings : DashboardSideEffect
    data class PromptNotificationPermission(val rationale: String) : DashboardSideEffect
    data class PromptSmsPermission(val rationale: String) : DashboardSideEffect
}
```

---

## 2. Dribbble-Grade Theme Tokens Contract

Every component MUST strictly draw its colors, radii, and elevations from the centralized design tokens defined below (matching `apex_finance_dribbble_crafted_android_home_screen_widgets.html`):

```kotlin
object ApexThemeTokens {
    // Canvas & Surfaces
    val CanvasBg = Color(0xFF090B10)
    val SurfaceElevated = Color(0xFF12151F)
    val SurfaceCard = Color(0xE0121621) // 88% alpha
    val GlassPill = Color(0x0DFFFFFF) // 5% alpha white
    val GlassPillBorder = Color(0x14FFFFFF) // 8% alpha border

    // Accents
    val EmeraldPrimary = Color(0xFF10B981) // Income, positive balance, success
    val EmeraldTint = Color(0x1F10B981)    // 12% alpha
    val ChampagneGold = Color(0xFFF59E0B)  // Budget warning, secondary highlights
    val RoyalCobalt = Color(0xFF3B82F6)    // Actions, analytics, tabs
    val MutedRosewood = Color(0xFFF43F5E)  // Debits, expenses, destructive

    // Typography Colors
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)

    // Squircles & Radii
    val RadiusPill = RoundedCornerShape(9999.dp)
    val RadiusSm = RoundedCornerShape(14.dp)
    val RadiusCard = RoundedCornerShape(20.dp)
    val RadiusSheet = RoundedCornerShape(28.dp)

    // Spring Animations
    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}
```
