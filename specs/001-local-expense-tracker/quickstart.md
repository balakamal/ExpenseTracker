# Quickstart: Apex Expense Tracker Android

**Feature**: `001-local-expense-tracker`  
**Date**: 2026-10-22

---

## 1. Prerequisites

- **Java Development Kit**: JDK 17 (Eclipse Temurin 17 or Amazon Corretto 17)
- **Android SDK**: Build Tools 35.0.0, Platform SDK 35 (Android 15)
- **Gradle**: 8.7+ (configured via Gradle wrapper)
- **IDE**: Android Studio Ladybug (2024.2.1+) or command line tools

---

## 2. Project Directory Structure

```text
android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/innovatex/expensetracker/
│       │       ├── di/                    # Hilt modules
│       │       ├── domain/                # Pure Kotlin UseCases & Models
│       │       │   ├── model/
│       │       │   └── usecase/
│       │       ├── data/                  # Repositories & Room DAOs
│       │       │   ├── local/
│       │       │   │   ├── dao/
│       │       │   │   ├── entity/
│       │       │   │   └── ExpenseDatabase.kt
│       │       │   └── repository/
│       │       ├── ingestion/             # SMS Receiver & Notification Listener
│       │       │   ├── sms/
│       │       │   ├── notification/
│       │       │   └── parser/
│       │       ├── presentation/          # Compose UI & ViewModels
│       │       │   ├── theme/             # Dribbble FinTech Theme Tokens
│       │       │   ├── components/        # Squircles, Shimmers, Cards
│       │       │   └── dashboard/
│       │       └── widget/                # Glance AppWidget implementations
│       │           ├── ExpenseGlanceWidget.kt
│       │           └── ExpenseWidgetReceiver.kt
│       └── test/                          # Unit tests (JUnit 5, MockK, Turbine)
├── gradle/
│   └── libs.versions.toml                 # Version Catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 3. Build & Test Commands

### 3.1 Local Compilation
```bash
# Clean and assemble debug APK
./gradlew clean assembleDebug

# Run unit tests across domain and data layers
./gradlew testDebugUnitTest

# Run static analysis and lint checks
./gradlew lintDebug ktlintCheck detekt
```

### 3.2 Installing on a Connected Android Device / Emulator
```bash
# In-place installation preserving database data
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. GitHub Actions CI/CD Verification

The GitHub Actions workflow (`.github/workflows/android-ci.yml`) executes on every commit and pull request:
1. Validates code formatting and checks for lint errors.
2. Runs all unit tests with code coverage verification.
3. Builds signed Debug & Release APKs.
4. Uploads the generated `.apk` artifacts to the GitHub Actions run summary for direct download and mobile installation.

---

## 5. Testing Ingestion Locally via ADB

Simulate incoming bank SMS notifications directly on an emulator:
```bash
# Simulate a bank debit SMS
adb emu sms send "+18005551234" "Acct XX4321 debited for USD 58.20 at Whole Foods on 22-Oct. Avl bal: USD 1,420.00"

# Verify that the transaction appears in the app and the home screen widget updates within 3 seconds
```
