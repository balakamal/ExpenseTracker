# Implementation Plan: Local Monthly Expense Tracker with SMS/Email Deduplication & Live Widgets

**Branch**: `001-local-expense-tracker` | **Date**: 2026-10-22 | **Spec**: [specs/001-local-expense-tracker/spec.md](spec.md)

**Input**: Feature specification from `specs/001-local-expense-tracker/spec.md`

## Summary

Build an elite, privacy-first Android expense tracking application that captures financial activity locally via SMS and Email notifications, performs two-stage temporal deduplication without cloud servers, classifies credit/debit transactions, categorizes expenses using local heuristics and merchant rules, and delivers live real-time updates to interactive Android home screen widgets. The UI is built entirely in Jetpack Compose with Material 3 Expressive and bespoke Dribbble-grade FinTech aesthetics (deep obsidian canvas, forest emerald, champagne gold, squircle cards, physics spring motion).

## Technical Context

**Language/Version**: Kotlin 2.0+ (JVM 17 target)

**Primary Dependencies**:
- UI Toolkit: Jetpack Compose (BOM 2024.10+), Material 3 Expressive, Navigation Compose
- Widgets: AndroidX Glance 1.1+ (GlanceAppWidget, GlanceModifier)
- Architecture & DI: AndroidX Lifecycle, ViewModel, Hilt (Dagger) 2.51+
- Persistence: AndroidX Room 2.6+ with KSP, Jetpack DataStore (Preferences)
- Concurrency & Async: Kotlin Coroutines 1.9+, StateFlow, SharedFlow
- Image/Icons: Coil 2.7+ (with hardware bitmap caching)

**Storage**: Local Room SQLite database (`expense_tracker.db`) with multi-table indexing and Paging 3 support; Jetpack DataStore for user preferences.

**Testing**: JUnit 5, MockK, Turbine (Flow verification), Robolectric, Room `MigrationTestHelper`, Compose UI Test.

**Target Platform**: Android OS (minSdk 26 / Android 8.0 Oreo, targetSdk 35 / Android 15, compileSdk 35).

**Project Type**: Native Android Mobile Application.

**Performance Goals**:
- 60/120 FPS jank-free rendering (<16.6ms / <8.3ms per frame budget) across all list scrolling and sheet transitions.
- Sub-500ms background notification parsing and deduplication.
- Sub-3-second live home screen widget updates upon incoming transaction events.

**Constraints**:
- 100% offline-first: Zero external cloud servers, zero telemetry or financial data transmitted off-device.
- Zero memory leaks (monitored by LeakCanary in debug).
- Seamless in-place APK updates (`adb install -r`) with guaranteed data preservation and zero crashes.

**Scale/Scope**: Support 10,000+ local transaction records, 5+ years of historical month partitions, 3 AppWidget sizes (2x2, 4x2, 4x4).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / Gate | Compliance Status | Evidence & Architecture Verification |
| :--- | :---: | :--- |
| **I. Dribbble-Grade UI/UX Craft** | **PASS** | Bespoke design system defined in `contracts/ui-state-contract.md`; dark obsidian `#090B10`, emerald `#10B981`, champagne gold `#F59E0B`; physics-based spring animations; skeleton shimmers. No generic boilerplate. |
| **II. Zero-Jank & Memory Discipline** | **PASS** | State reads deferred to graphics layer; `@Immutable` / `@Stable` models; `Dispatchers.IO` isolation for parsing/DB operations; LeakCanary integrated in debug. |
| **III. Clean Architecture & UDF** | **PASS** | Strict 3-layer architecture (`presentation`, `domain`, `data`); pure Kotlin domain layer free of `android.*` imports; immutable `StateFlow<MonthlyDashboardState>`. |
| **IV. Seamless In-Place Updates** | **PASS** | Permanent `applicationId = "com.innovatex.expensetracker"`; CI-managed monotonic `versionCode`; Room migrations verified via `MigrationTestHelper`; zero destructive fallback in release. |
| **V. Automated CI/CD Pipeline** | **PASS** | GitHub Actions workflow on Ubuntu runners with JDK 17, `lintDebug`, `ktlintCheck`, `detekt`, `testDebugUnitTest`, release assembly, and APK artifact uploads. |
| **VI. Offline-First Single Source of Truth** | **PASS** | Room SQLite database as local authority; all ingestion runs via event-driven local listeners (`BroadcastReceiver`, `NotificationListenerService`) with zero cloud dependencies. |

## Project Structure

### Documentation (this feature)

```text
specs/001-local-expense-tracker/
├── spec.md              # Feature specification with clarified requirements
├── plan.md              # This implementation plan
├── research.md          # Phase 0 research: M3E styling, deduplication, Glance widgets
├── data-model.md        # Phase 1 data models: Room entities, DAOs, state machines
├── quickstart.md        # Phase 1 developer guide, commands, and ADB test procedures
└── contracts/           # Phase 1 contracts
    ├── ingestion-parser-contract.md
    ├── widget-update-contract.md
    └── ui-state-contract.md
```

### Source Code (Android Application Structure)

```text
android/
├── .github/
│   └── workflows/
│       └── android-ci.yml             # GitHub Actions CI/CD workflow
├── app/
│   ├── build.gradle.kts               # App module build configuration
│   ├── proguard-rules.pro             # R8 shrinking & obfuscation rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    # App manifest with receiver & listener declarations
│       │   └── kotlin/com/innovatex/expensetracker/
│       │       ├── di/                # Hilt Dependency Injection modules
│       │       │   ├── DatabaseModule.kt
│       │       │   ├── RepositoryModule.kt
│       │       │   └── ParserModule.kt
│       │       ├── domain/            # Pure Kotlin Domain Layer (Zero Android imports)
│       │       │   ├── model/         # Transaction, Category, MonthlySummary, Rule
│       │       │   └── usecase/       # Parse, Deduplicate, GetSummary, UpdateCategory
│       │       ├── data/              # Data Layer (Room & DataStore implementations)
│       │       │   ├── local/
│       │       │   │   ├── dao/       # TransactionDao, CategoryDao, RuleDao
│       │       │   │   ├── entity/    # Room Entities
│       │       │   │   ├── migration/ # Explicit Room Migrations
│       │       │   │   └── ExpenseDatabase.kt
│       │       │   └── repository/    # Repository Implementations
│       │       ├── ingestion/         # Local On-Device Event Listeners
│       │       │   ├── sms/           # SmsBroadcastReceiver & Historical Scanner
│       │       │   ├── notification/  # EmailNotificationListenerService
│       │       │   └── parser/        # Deterministic Regex Parsing Engine
│       │       ├── presentation/      # Jetpack Compose UI (Material 3 Expressive)
│       │       │   ├── theme/         # ApexThemeTokens, Color, Type, Shape
│       │       │   ├── components/    # Squircles, ShimmerLoader, CategoryPills
│       │       │   ├── dashboard/     # MonthlyDashboardScreen, DashboardViewModel
│       │       │   └── transaction/   # TransactionDetailSheet, CategoryPicker
│       │       └── widget/            # AndroidX Glance AppWidgets
│       │           ├── ExpenseGlanceWidget.kt
│       │           └── ExpenseWidgetReceiver.kt
│       └── test/                      # Unit tests (Robolectric, MockK, Turbine)
├── gradle/
│   ├── libs.versions.toml             # Gradle Version Catalog
│   └── wrapper/
├── build.gradle.kts                   # Root build script
└── settings.gradle.kts                # Project settings
```

**Structure Decision**: Selected standard Clean Architecture Android single-app structure with distinct `domain`, `data`, `presentation`, `ingestion`, and `widget` packages. Domain package strictly contains zero Android SDK imports to allow instantaneous unit testing on the JVM without emulators.

## Complexity Tracking

*No constitution violations. Architecture is fully compliant with all 6 ratified core principles.*
