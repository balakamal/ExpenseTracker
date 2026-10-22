<!--
Sync Impact Report:
- Version change: [CONSTITUTION_VERSION] (unratified template) → 1.0.0
- List of modified principles:
  - [PRINCIPLE_1_NAME] → I. Dribbble-Grade UI/UX Craft & Motion Distinction (NON-NEGOTIABLE)
  - [PRINCIPLE_2_NAME] → II. Zero-Jank Rendering & Memory Optimization (60/120 FPS Discipline)
  - [PRINCIPLE_3_NAME] → III. Clean Architecture & Unidirectional Data Flow (UDF / MVI)
  - [PRINCIPLE_4_NAME] → IV. Seamless In-Place Updates & Data Migration Integrity (NON-NEGOTIABLE)
  - [PRINCIPLE_5_NAME] → V. Automated CI/CD & Hermetic GitHub Actions Pipeline
  - [Added Principle] → VI. Offline-First Single Source of Truth & Local Storage Discipline
- Added sections:
  - Technology Stack & Android Platform Constraints (formerly [SECTION_2_NAME])
  - Engineering Quality Gates & Release Verification (formerly [SECTION_3_NAME])
- Removed sections: None
- Templates requiring updates:
  - .specify/templates/plan-template.md: ✅ Verified aligned with Android mobile structure & quality gates
  - .specify/templates/spec-template.md: ✅ Verified aligned with UX acceptance scenarios & update continuity
  - .specify/templates/tasks-template.md: ✅ Verified aligned with UI polish, unit/instrumented testing, and CI tasks
- Follow-up TODOs: None (all placeholders concretely defined)
-->

# Apex Android Application Constitution

## Core Principles

### I. Dribbble-Grade UI/UX Craft & Motion Distinction (NON-NEGOTIABLE)
Every user-facing screen MUST be treated as an award-winning digital product crafted from scratch by a senior UI/UX designer. Generic boilerplate, vanilla Material-3 defaults, and uninspired layouts are STRICTLY FORBIDDEN.
- **Visual Design System**: Colors MUST be drawn from a curated, harmonious palette (e.g., deep obsidian canvas `#090B10`, elevated charcoal surfaces `#12151F`, forest emerald `#10B981`, champagne gold `#F59E0B`, royal cobalt `#3B82F6`, cashmere slate). Raw primary red/blue/green colors MUST NOT be used. Surface depth MUST be achieved using subtle 1px translucent border strokes (`rgba(255,255,255,0.08–0.14)`), soft ambient shadows, and squircles (`RoundedCornerShape(20.dp)` to `28.dp`).
- **Typography & Hierarchy**: Typography MUST use modern geometric or editorial sans-serif typefaces (e.g., Outfit, Inter, or Plus Jakarta Sans) loaded with explicit weights, optical tracking, and tight line heights to establish clear information hierarchy.
- **Physics-Based Motion**: All state transitions, item additions, card expansions, and modal sheets MUST use physics-based spring animations (`spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`). Linear and robotic easing curves are BANNED. Shared element transitions MUST be used between summary cards and detailed views.
- **Tactile Haptics & Micro-Interactions**: Interactive toggles, expense categorizations, swipe actions, and confirmations MUST trigger appropriate haptic feedback (`HapticFeedbackType.LongPress`, `TextHandleMove`, or custom vibration effects).
- **Edge-to-Edge & States**: The app MUST use Android 14/15 edge-to-edge layout (`enableEdgeToEdge()`) with systematic window insets padding. Zero blank screens: all loading states MUST use shimmering skeleton loaders tuned to the ambient surface color; empty and error states MUST feature custom illustrative vector art and contextual action copy.
*Rationale: In financial and daily tracking applications, aesthetic polish, micro-animations, and tactile feedback establish instant user trust, eliminate perceived latency, and drive long-term engagement.*

### II. Zero-Jank Rendering & Memory Optimization (60/120 FPS Discipline)
Visual splendor MUST NEVER compromise runtime performance. The application MUST deliver a continuous 60 FPS or 120 FPS (on high-refresh displays) experience with zero dropped frames.
- **Frame Budget**: Every frame MUST render within 16.6ms (60Hz) or 8.3ms (120Hz). Jank during scrolling, gesture drags, and sheet dismissals is treated as a critical defect.
- **Recomposition Hygiene**: Jetpack Compose code MUST minimize recomposition scopes. All model classes passed to composables MUST be annotated with `@Immutable` or `@Stable`. Lambdas capturing unstable references MUST be wrapped in `remember`. State calculations MUST use `derivedStateOf`. Layout and draw manipulations (e.g. rotation, alpha, translation) MUST be executed in the graphics layer (`Modifier.graphicsLayer { ... }`) to skip composition and layout passes completely.
- **Memory Leak Elimination**: The application MUST NOT leak Activities, Fragments, ViewModels, or Coroutine scopes. LeakCanary MUST be integrated into debug builds. No `Context` references outside of Lifecycle-aware components.
- **Bitmap & Resource Lifecycle**: Image loading via Coil MUST enforce memory caching, disk caching, explicit downsampling to display bounds, and Hardware Bitmaps (`Bitmap.Config.HARDWARE`). Heavy drawables must be vectorized or rendered via efficient Canvas operations.
- **Main Thread Isolation**: `Dispatchers.Main` MUST only handle UI bindings. All database queries, financial calculations, encryption, parsing, and I/O MUST explicitly execute on `Dispatchers.IO` or `Dispatchers.Default` using structured concurrency.
*Rationale: Stuttering animations and out-of-memory crashes ruin user perception and trigger OS background process termination. Performance optimization must be architectural, not an afterthought.*

### III. Clean Architecture & Unidirectional Data Flow (UDF / MVI)
The codebase MUST maintain strict separation of concerns through Clean Architecture and reactive Unidirectional Data Flow.
- **Layer Separation**:
  1. `presentation`: Composable screens, UI state definitions, and ViewModels. Depends only on `domain`.
  2. `domain`: Pure Kotlin entities and UseCases/Interactors. Zero Android SDK dependencies (`android.*` imports are FORBIDDEN in domain modules).
  3. `data`: Repository implementations, Room DAOs, DataStore sources, network clients, and data mappers. Depends on `domain`.
- **State Immutability**: ViewModels MUST expose a single, immutable `StateFlow<UiState>` representing complete screen state. UI events MUST be sent as distinct sealed interface actions (e.g., `ExpenseIntent`), and transient one-time side effects (e.g. navigation, toasts) MUST be dispatched via `Channel`/`SharedFlow`.
- **Dependency Injection**: Dependencies MUST be injected cleanly via Hilt or Koin. No service locators or global singletons.
*Rationale: Strict architecture guarantees that business logic remains 100% unit-testable in isolation from Android framework lifecycles and UI changes.*

### IV. Seamless In-Place Updates & Data Migration Integrity (NON-NEGOTIABLE)
New versions of the application MUST install directly over existing versions without requiring uninstallation, while preserving 100% of the user's data.
- **Permanent Application Identity**: The `applicationId` MUST remain permanent and immutable across all builds and branches (e.g., `com.innovatex.expensetracker`).
- **Consistent Cryptographic Signing**: Both debug and release APKs/AABs MUST be signed with consistent keystores. GitHub Actions workflows MUST use a dedicated Keystore stored securely in GitHub Secrets. Development debug builds MUST use a shared, version-controlled debug keystore so developer and CI builds can be cross-installed without `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- **Monotonic Version Code**: `versionCode` MUST strictly increment as a positive monotonic integer for every CI release build (tied to CI run number or Git tag sequence). `versionName` MUST strictly follow Semantic Versioning (`MAJOR.MINOR.PATCH`).
- **Zero-Loss Database Migrations**: Room database migrations MUST be fully automated or explicitly scripted (`Migration(from, to)`). Destructive migration fallbacks (`fallbackToDestructiveMigration()`) are STRICTLY FORBIDDEN in release builds. Every schema modification MUST include an automated migration unit test using `MigrationTestHelper`.
- **DataStore & State Continuity**: User preferences, currency settings, biometric locks, and category structures stored in DataStore MUST validate schema compatibility on launch and migrate gracefully without resetting defaults.
*Rationale: Forcing users to uninstall an app to upgrade destroys user trust and wipes out their recorded financial history.*

### V. Automated CI/CD & Hermetic GitHub Actions Pipeline
Every commit, branch, and pull request MUST be verified through a fully automated GitHub Actions pipeline.
- **Hermetic Environment**: Workflows MUST run on standard GitHub Actions Ubuntu runners (`ubuntu-latest`) using locked JDK toolchains (Temurin JDK 17 or 21) and Gradle Build Cache for speed and consistency.
- **Mandatory Quality Pipeline**: Every CI run MUST execute:
  1. `./gradlew lintDebug` (Android Lint with `abortOnError true`).
  2. `./gradlew ktlintCheck detekt` (Code formatting and static analysis).
  3. `./gradlew testDebugUnitTest` (Unit tests for domain UseCases, repositories, and ViewModels).
  4. `./gradlew assembleDebug` and `./gradlew assembleRelease` (Ensuring R8/ProGuard rules compile cleanly).
- **Automated Artifact Delivery**: The CI pipeline MUST sign and upload generated APKs to GitHub Actions Artifacts on every push to main/feature branches, allowing instantaneous installation and testing on physical Android devices.
- **Secret Management**: All keystore credentials, base64 keystores, and API tokens MUST be injected through GitHub Repository Secrets; no sensitive certificates or passwords may ever be committed to the repository.
*Rationale: Automated continuous integration eliminates the 'works on my machine' failure mode, guarantees build reproducibility, and ensures installable APKs are always ready for testing.*

### VI. Offline-First Single Source of Truth & Local Storage Discipline
The application MUST operate flawlessly with zero internet connection.
- **Local Database as Authority**: Room SQLite database is the ultimate authority for all transactions, accounts, categories, and analytics. UI screens MUST observe database state via reactive Kotlin `Flow`.
- **Atomic Operations**: All multi-table updates (e.g., deducting an account balance and recording an expense) MUST be executed within a Room `@Transaction` block to prevent orphaned or corrupted state.
- **Large Dataset Paging**: Transaction feeds, historical expense queries, and ledger logs MUST utilize Jetpack Paging 3 to stream data incrementally from Room into Compose `LazyColumn` components without memory ballooning.
*Rationale: Expense tracking is an offline-critical utility; users must be able to log transactions instantly in any environment without latency or connectivity blockers.*

## Technology Stack & Android Platform Constraints

- **Language & Runtime**: Kotlin 2.0+ with modern Kotlin compiler and Coroutines/Flow.
- **UI Framework**: 100% Jetpack Compose with Material 3 foundation, customized with Apex bespoke tokens and Canvas drawing. XML layout files are FORBIDDEN except for AppWidget `RemoteViews`.
- **Dependency Injection**: Hilt (AndroidX Hilt Navigation Compose) or Koin.
- **Local Storage**: Room Database with KSP (Kotlin Symbol Processing) and Jetpack DataStore (Preferences / Proto).
- **Image & Media**: Coil Compose with memory/disk caching and hardware bitmap rendering.
- **Asynchronous Execution**: Kotlin Coroutines with strict Structured Concurrency (`viewModelScope`, `Lifecycle.repeatOnLifecycle`).
- **Build System**: Gradle Version Catalog (`gradle/libs.versions.toml`), Android Gradle Plugin (AGP 8.x+), R8 minification and resource shrinking for release builds.
- **Testing Suite**: JUnit 5, MockK, Turbine (for testing Flow emissions), Robolectric, and AndroidX Compose UI Test.
- **Platform Compatibility**: `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 35` (Android 15), `compileSdk = 35`.

## Engineering Quality Gates & Release Verification

Before any feature pull request is merged or released, the following gates MUST pass:
1. **Constitution Compliance**: The implementation strictly adheres to all six core principles herein.
2. **Recomposition & Performance Audit**: The feature passes Compose Layout Inspector verification with zero unwanted recompositions during scroll interactions.
3. **Memory Leak Audit**: No memory leaks detected by LeakCanary or Android Studio Profiler during screen rotation and navigation cycles.
4. **Room Migration Validation**: Any schema change is verified with a `MigrationTestHelper` unit test proving data preservation across versions.
5. **Dribbble Design Fidelity**: Visuals, typography, dark/light contrast, and spring animations match designer-grade aesthetics with no default/unthemed components.
6. **In-Place Upgrade Test**: An update from the prior build APK to the current build APK succeeds via `adb install -r` without signature mismatch or app crash.
7. **CI/CD Build & Test Gate**: GitHub Actions workflow completes green across linting, static analysis, unit testing, and APK artifact generation.

## Governance

- **Supremacy**: This Constitution supersedes all informal guidelines, team discussions, and individual coding preferences. Any deviation MUST be documented, justified, and approved.
- **Amendment Procedure**: Any change to this Constitution requires a formal pull request detailing the rationale, impact assessment, and migration strategy for affected code and artifacts.
- **Versioning Policy**: Semantic Versioning rules apply:
  - MAJOR bump: Removing or redefining core principles, architectural paradigm shifts.
  - MINOR bump: Introducing new principles, expanding constraints or quality gates.
  - PATCH bump: Clarifications, wording refinements, typo corrections.
- **Compliance Enforcement**: Code reviews and automated CI checks MUST verify compliance with all principles before code is merged into the mainline repository.

**Version**: 1.0.0 | **Ratified**: 2026-10-22 | **Last Amended**: 2026-10-22
