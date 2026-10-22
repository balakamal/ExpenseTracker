# Phase 0 Research: Local Expense Tracker Architecture & Technology Decisions

**Feature**: `001-local-expense-tracker`  
**Date**: 2026-10-22  
**Target Platform**: Android (minSdk 26, targetSdk 35)

---

## 1. UI/UX Paradigm: Dribbble-Grade Material 3 Expressive

### Decision
Adopt **Material 3 Expressive (M3E)** with custom bespoke tokens inspired by Dribbble modern FinTech dashboards (as prototyped in `apex_finance_dribbble_crafted_android_home_screen_widgets.html`).

### Rationale
- Standard Material 3 templates often feel generic, flat, and clinical. Financial apps demand trust, tactile feedback, and visual prestige.
- Material 3 Expressive introduces dynamic shapes (squircles with varying curvature), expressive spring physics, expanded color roles with nuanced container tones, and rich typography scales.
- Visual tokens:
  - **Canvas & Surfaces**: Deep obsidian `#090B10`, elevated card `#12151F`, translucent glass pill highlights `rgba(255, 255, 255, 0.05)`.
  - **Accents**: Forest Emerald (`#10B981`) for positive balance / credit, Champagne Gold (`#F59E0B`) for warnings / alerts, Royal Cobalt (`#3B82F6`) for primary actions / analytics, Muted Rosewood (`#F43F5E`) for debits.
  - **Micro-Borders**: 1px subtle strokes (`rgba(255, 255, 255, 0.08)`) provide physical depth and eliminate blurriness.
  - **Motion**: Compose Physics-based `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)` for card expansions, button presses, and sheet dismissals.
  - **Loading States**: Shimmering skeleton loaders with matching dark-slate gradient passes instead of circular progress spinners.

### Alternatives Evaluated
- *Vanilla Material 3*: Rejected because defaults create a cookie-cutter Android utility aesthetic rather than an elite, crafted FinTech experience.
- *Custom Skia/Canvas engine*: Overkill for a native app; Jetpack Compose with custom `Modifier.graphicsLayer` and `Modifier.drawWithContent` provides 60/120 FPS native performance without custom C++ bridges.

---

## 2. Ingestion & Privacy Architecture: On-Device Notification Listener vs Direct Sync

### Decision
Implement a hybrid on-device capture pipeline:
1. **SMS Ingestion**: `BroadcastReceiver` for incoming SMS (`Telephony.Sms.Intents.SMS_RECEIVED_ACTION`) combined with a one-time `ContentResolver` historical scan (past 30 days).
2. **Email Ingestion**: Android `NotificationListenerService` (`NotificationListenerService`) filtering notifications specifically posted by email packages (`com.google.android.gm`, `com.microsoft.office.outlook`, etc.).

### Rationale
- **100% Privacy Guarantee**: Reading email notifications locally requires zero cloud servers, zero external API keys, and zero user account passwords or OAuth scopes. The data never leaves the handset.
- **Battery & Memory Efficiency**: `NotificationListenerService` is event-driven by the Android framework. The OS wakes the listener only when a notification arrives, consuming virtually 0% idle battery, unlike polling IMAP/POP3 background sync workers.
- **Immediate Live Reaction**: Notifications arrive in real-time as transactions happen, enabling instantaneous home screen widget updates.

### Alternatives Evaluated
- *Direct Gmail OAuth / IMAP Sync*: Requires storing OAuth refresh tokens on device, requires internet connectivity to sync, consumes heavy background battery polling inboxes, and raises privacy alarms with end-users.
- *Cloud Webhook Relay*: Strictly forbidden by Constitution Principle VI (Offline-First Single Source of Truth & Local Privacy).

---

## 3. Transaction Deduplication Algorithm

### Decision
Implement a deterministic **Two-Stage Sliding-Window Deduplication Engine** in the domain layer (`DeduplicateTransactionUseCase`):

```
Incoming Event (SMS or Email)
       │
       ▼
Extract Core Triad: [Amount, Timestamp, Currency]
       │
       ▼
Query Room for Unsettled Records within [Timestamp - 10min, Timestamp + 10min]
       │
       ├─► MATCH FOUND:
       │     - If Account Last 4 digits match OR Reference ID matches OR Merchant similarity > 0.8:
       │         => MERGE into existing record:
       │            sources = sources + [newSource]
       │            isDeduplicated = true
       │            update transaction in Room (DO NOT create new debit)
       │
       └─► NO MATCH:
             => INSERT as new Transaction (sources = [newSource])
```

### Rationale
- Banks typically dispatch SMS and email receipts simultaneously; network latencies between cellular SMS gateways and push notification servers rarely exceed 3 to 5 minutes. A 10-minute sliding window reliably captures cross-channel pairs.
- By matching exact monetary amount, currency, and secondary corroborating metadata (reference ID or last-4 account digits), false positive collisions between two intentional distinct purchases (e.g. two $5 coffees bought 15 minutes apart) are avoided.

### Alternatives Evaluated
- *Exact Timestamp Matching*: Fails because SMS delivery time and email notification arrival time can differ by 15–90 seconds.
- *Strict Reference ID Matching Alone*: Insufficient because many bank SMS alerts abbreviate or omit full reference IDs while email receipts include full invoice numbers.

---

## 4. Live Android Home Screen Widgets: Jetpack Glance vs Traditional RemoteViews

### Decision
Use **Jetpack Glance (AndroidX Glance 1.1+)** built on Compose runtime to build interactive, stateful AppWidgets.

### Rationale
- Jetpack Glance allows writing widget UI using declarative Compose-like syntax (`GlanceModifier`, `Text`, `Row`, `Column`, `Box`, `Button`) while generating optimized `RemoteViews` under the hood.
- Seamless state reactivity: `GlanceAppWidget.update(context, glanceId)` updates the widget instantly via local Room Flow collectors or WorkManager broadcasts.
- Supports modern Android 12+ rounded squircle corners, dynamic colors, and smooth launch animations directly into target app screens.

### Alternatives Evaluated
- *Legacy XML RemoteViews*: Extremely verbose, error-prone layout inflation, cumbersome state updates, and poor maintainability compared to Glance.

---

## 5. Local Storage & Database Migrations: Room with KSP

### Decision
Use **AndroidX Room 2.6+** with Kotlin Symbol Processing (KSP) and explicit migrations:
- Entities: `TransactionEntity`, `CategoryEntity`, `MerchantRuleEntity`, `MonthlySummaryEntity`.
- Foreign keys with `CASCADE` or `RESTRICT` depending on entity relationships.
- Room Auto-Migrations where applicable, supplemented with manual `Migration(from, to)` classes and automated `MigrationTestHelper` test cases.
- Paging 3 integration via `PagingSource` directly from Room DAOs.

### Rationale
- Enforces Constitution Principle IV (Seamless In-Place Updates & Data Migration Integrity): prevents data loss upon updating APK versions.
- Compile-time SQL validation prevents runtime query crashes.
- First-class Kotlin `Flow` support streams real-time database changes to ViewModels and Glance widgets.

---

## 6. GitHub Actions CI/CD Pipeline

### Decision
Standardize on a hermetic multi-stage workflow in `.github/workflows/android-ci.yml`:
- **Runner**: `ubuntu-latest`
- **JDK Toolchain**: Eclipse Temurin JDK 17
- **Gradle Caching**: `gradle/actions/setup-gradle@v4` with read/write caching of wrapper and build caches.
- **Stages**:
  1. Validation & Code Quality: `ktlintCheck`, `detekt`, `lintDebug` (`abortOnError true`).
  2. Unit Tests: `testDebugUnitTest` (JUnit 5, MockK, Turbine).
  3. Artifact Assembly: `assembleDebug` and `assembleRelease`.
  4. Release Signing: `r0adkll/sign-android-release@v1` using base64 keystore stored in GitHub Secrets.
  5. Artifact Upload: `actions/upload-artifact@v4` retaining signed APKs for 30 days.

### Rationale
- Guaranteed reproducibility; developers and testers receive installable APKs directly from workflow runs.
- Continuous signature consistency prevents `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
