# Feature Specification: Local Monthly Expense Tracker with SMS/Email Deduplication & Live Widgets

**Feature Branch**: `001-local-expense-tracker`

**Created**: 2026-10-22

**Status**: Draft

**Input**: User description: "We are creating an android app to track the expenses happened per month everything locally either by tracking sms or email or both but if the same transaction shown in both the places taking only one classifying if it is credit or debit and also categioring those and showing that in our app as well as android home screen widgets in live."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Core Local SMS Expense Capture & Monthly Dashboard (Priority: P1)

A user receives financial transaction SMS messages from banks, cards, and payment gateways. The app parses these SMS messages strictly locally on the Android device, extracts amount and merchant, accurately classifies them as Credit (income) or Debit (expense), assigns them to standard expense categories, and displays a real-time monthly spending dashboard.

**Why this priority**: This constitutes the foundational Minimum Viable Product (MVP). Without accurate local parsing, classification, and monthly aggregation of incoming transactions, no other features (deduplication, widgets, or analytics) can function.

**Independent Test**: The user receives a bank debit SMS ("Acct XX1234 debited by USD 42.50 at Supermarket on 22-Oct"); the app captures it locally, records a $42.50 Debit under "Groceries", updates the current month's total debit and category progress, and lists the transaction in chronological order.

**Acceptance Scenarios**:

1. **Given** the app has SMS access enabled and a new financial SMS arrives, **When** the message text indicates a withdrawal, card swipe, or purchase, **Then** the app records a new Debit transaction with exact amount, merchant name, timestamp, and appropriate category.
2. **Given** a financial SMS arrives indicating a salary deposit, refund, or transfer received, **When** the message text contains credit keywords, **Then** the app records a new Credit transaction and adds it to the monthly income total.
3. **Given** a non-financial SMS (such as OTP, service alert, or personal message) arrives, **When** the parsing engine analyzes the message, **Then** it is completely ignored without creating any transaction record or consuming background battery.

---

### User Story 2 - Dual-Channel Ingestion & Intelligent Transaction Deduplication (Priority: P2)

A user enables both SMS and Email tracking. When a payment triggers both an SMS alert and an email confirmation from the bank or payment processor, the app detects that both notifications represent the identical transaction, records only a single transaction, and links both sources together.

**Why this priority**: Users frequently receive duplicate notifications for the same card swipe or digital transfer. Double-counting distorts monthly budgets and destroys trust in the expense tracker.

**Independent Test**: A bank transaction triggers an SMS alert and an email alert 45 seconds apart with the same amount ($18.75) and merchant ("Starbucks"). The app logs the transaction once, increments the monthly debit by exactly $18.75, and tags the transaction as verified via "SMS + Email".

**Acceptance Scenarios**:

1. **Given** tracking is enabled for both SMS and Email, **When** an SMS and an Email alert arrive within a 10-minute window with matching amount, currency, and merchant/account identifiers, **Then** the app creates only one transaction record with multi-source attribution ("SMS + Email") and increments monthly spend only once.
2. **Given** a user makes two intentional, identical back-to-back purchases (e.g. two separate $5.00 subway tickets 2 minutes apart), **When** each purchase produces unique reference IDs or distinct timestamps across separate SMS messages, **Then** the app correctly retains both transactions as distinct debits.
3. **Given** user preferences, **When** the user switches ingestion mode to "SMS Only" or "Email Only", **Then** the app halts ingestion on the disabled channel without disrupting existing recorded transactions.

---

### User Story 3 - Live Android Home Screen Widgets (Priority: P3)

The user places an interactive widget on their Android home screen to monitor their monthly spending budget in real-time without needing to launch the main application.

**Why this priority**: Glancing at current monthly spending directly from the home screen provides immediate financial awareness and reinforces habit tracking.

**Independent Test**: The user places a 4x2 Apex widget on their home screen showing a monthly expense total of $850. A new $50 expense is processed in the background; the widget updates in real-time to show $900 total and updates the progress bar toward the monthly budget ceiling.

**Acceptance Scenarios**:

1. **Given** an active home screen widget, **When** a new transaction is processed via SMS or Email, **Then** the widget reflects the updated monthly total, remaining budget, and latest transaction within 3 seconds.
2. **Given** the user taps on the home screen widget, **When** the tap occurs on the summary card or quick-action zone, **Then** the app opens directly to the detailed Monthly Expense breakdown.
3. **Given** a month boundary occurs (e.g. midnight transitioning from October 31 to November 1), **When** the widget refreshes, **Then** it automatically rolls over to the new month, resets the monthly expenditure to zero, and reflects the current month's budget.

---

### User Story 4 - Local Categorization Customization & Merchant Rule Learning (Priority: P4)

A user inspects auto-categorized expenses, corrects misclassified categories, and creates custom categorization rules that apply automatically to future transactions.

**Why this priority**: Automated heuristics may not understand every local merchant; user-driven correction ensures long-term categorization accuracy with zero manual upkeep.

**Independent Test**: A transaction from "Blue Bottle Coffee" is initially auto-assigned to "General". The user changes the category to "Food & Dining" and checks "Always apply for this merchant". Subsequent transactions from "Blue Bottle Coffee" automatically classify under "Food & Dining".

**Acceptance Scenarios**:

1. **Given** an auto-categorized transaction, **When** the user selects a different category from the category picker, **Then** the transaction immediately updates its category and recalculates the monthly category distribution.
2. **Given** a user edits a merchant category with rule saving enabled, **When** a future transaction is parsed from that merchant, **Then** the learned local rule takes precedence over default heuristics.
3. **Given** the category manager screen, **When** the user adds a custom category with a bespoke name, icon, and monthly spending target, **Then** it becomes instantly available for categorization and widget filtering.

---

### User Story 5 - Historical Month Exploration & Local Privacy Archive (Priority: P5)

A user browses historical spending across previous months, reviews seasonal spending patterns, and exports or backs up their private database locally.

**Why this priority**: Long-term personal finance requires retrospective reviews, tax preparation summaries, and complete ownership of private financial records without cloud lock-in.

**Independent Test**: The user navigates to the previous month's tab, views the complete locked archive of all transactions, category totals, and net balance, and can export a local CSV summary.

**Acceptance Scenarios**:

1. **Given** past months with recorded transactions, **When** the user slides the month selector to a previous month, **Then** the screen displays the historical totals and transactions for that month without recalculating or altering current month numbers.
2. **Given** the local backup settings, **When** the user selects "Export Data", **Then** the app generates a standard CSV/JSON export file saved directly to local device storage without sending any telemetry or data outside the device.

---

### Edge Cases

- **Identical Back-to-Back Purchases vs. Duplicate Alerts**: How does the system differentiate two distinct transactions of identical amounts at the same merchant from a single transaction reported by both SMS and Email? The engine uses transaction reference/UTR numbers when available, and checks timestamp deltas against message metadata.
- **Reversals, Refunds, and Failed Transactions**: When a bank sends a "Debit reversed" or "Transaction failed" notification, the system must recognize reversal keywords, pair them with the original debit, and either nullify the original debit or record an offsetting credit.
- **Foreign Currency Transactions**: When an SMS or email quotes an international purchase (e.g., "Spent EUR 30.00, equivalent to USD 32.50"), the app must parse both currencies and accurately store the charged billing amount in the user's primary currency.
- **Split Messages & Multipart SMS**: When a bank splits a long transaction receipt across multiple consecutive SMS segments, the ingestion service must buffer segments or parse each independently without creating partial duplicate transactions.
- **Device Reboot & Background Battery Saver**: When the device reboots or enters deep battery optimization, the app must ensure local listeners resume tracking immediately upon wakeup without dropping financial notifications.
- **In-Place App Upgrades**: When a new app version installs over an older version, all existing monthly transactions, merchant rules, and widget state must be preserved without data loss or crashes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST perform 100% of transaction processing, deduplication, categorization, and storage locally on the user's device with zero external cloud transmission.
- **FR-002**: The system MUST support SMS transaction parsing, extracting transaction amount, currency, timestamp, merchant/payee name, and account identifier.
- **FR-003**: The system MUST classify every parsed transaction as either `Credit` (income, refund, deposit) or `Debit` (expense, fee, payment).
- **FR-004**: The system MUST support Email transaction capture via an On-Device Notification Listener Service that intercepts incoming transaction alert notifications posted by email clients (such as Gmail, Outlook, or system mail apps) without requiring email credentials or external account sign-in.
- **FR-005**: The system MUST provide user toggles to enable or disable SMS tracking, Email tracking, or both independently.
- **FR-006**: When both SMS and Email tracking are active, the system MUST deduplicate identical transactions received from both sources within a defined temporal window (default 10 minutes) and store a single unified record.
- **FR-007**: The system MUST assign every transaction to an expense category using local keyword heuristics, with support for standard categories (Food & Dining, Groceries, Shopping, Transport, Utilities, Entertainment, Health, Transfer, Income).
- **FR-008**: The system MUST allow users to manually reassign a transaction's category and optionally create a persistent local rule for future transactions from that merchant.
- **FR-009**: The system MUST aggregate and display total debits, total credits, net balance, and category-by-category breakdowns grouped by calendar month.
- **FR-010**: The system MUST provide live Android home screen widgets displaying current month expenses, remaining budget progress, and latest transactions.
- **FR-011**: Home screen widgets MUST update automatically within 3 seconds of a transaction being parsed or modified.
- **FR-012**: The system MUST perform an initial historical scan of the past 30 days of SMS messages upon permission grant, immediately populating the current month's expense ledger and dashboard with recent financial activity.
- **FR-013**: The system MUST allow users to manually create, edit, or delete transactions (for cash or untracked transactions).
- **FR-014**: The system MUST ignore non-financial messages (e.g., OTPs, promotional spam, two-factor authentication codes) without recording them.

### Key Entities

- **Transaction**:
  - `id`: Unique identifier (local UUID).
  - `amount`: Monetary value (positive decimal).
  - `currency`: ISO-4217 currency code (e.g., USD, EUR, INR).
  - `type`: Classification (`Debit` | `Credit`).
  - `timestamp`: Date and time when the transaction occurred.
  - `merchant`: Cleaned merchant or payee name.
  - `categoryId`: Reference to assigned `Category`.
  - `sources`: Array of source channels (`SMS`, `Email`, `Manual`).
  - `referenceId`: Bank transaction reference or UTR number if present.
  - `accountLastDigits`: Last 4 digits of card or bank account.
  - `notes`: User-editable text.
  - `isDeduplicated`: Boolean flag indicating multi-source deduplication.

- **Category**:
  - `id`: Unique identifier.
  - `name`: Category label (e.g., "Food & Dining").
  - `icon`: Bespoke vector icon identifier.
  - `colorToken`: Design system color hex or token name.
  - `monthlyBudgetLimit`: Optional target spending cap for the category.
  - `isSystemDefault`: Boolean flag.

- **MerchantRule**:
  - `id`: Unique identifier.
  - `merchantPattern`: Regex or substring pattern for merchant matching.
  - `targetCategoryId`: Category to automatically assign.
  - `createdAt`: Timestamp.

- **MonthlySummary**:
  - `monthYear`: Year and month key (e.g., "2026-10").
  - `totalDebit`: Sum of all debit transactions in the month.
  - `totalCredit`: Sum of all credit transactions in the month.
  - `netBalance`: `totalCredit - totalDebit`.
  - `categoryTotals`: Map of `categoryId` to total spent.
  - `budgetLimit`: User-defined overall monthly spending ceiling.

- **WidgetState**:
  - `widgetId`: Android AppWidget ID.
  - `layoutSize`: Small (2x2), Medium (4x2), or Large (4x4).
  - `monthKey`: Associated month.
  - `lastRefreshed`: Timestamp of last sync.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of all data processing, deduplication, categorization, and persistence remains on the local device with zero external network data transmission.
- **SC-002**: In dual-channel mode, duplicate transactions generated across SMS and Email within a 10-minute window are detected and merged with 99%+ accuracy.
- **SC-003**: The live home screen widget updates its displayed monthly total and progress metrics within 3 seconds of a transaction being parsed.
- **SC-004**: Automated parsing of incoming notifications completes in under 500ms on device without causing any UI jank or main thread blocking.
- **SC-005**: 95%+ of standard financial debit/credit alert messages are correctly classified into Credit vs. Debit without manual user intervention.
- **SC-006**: Users can reassign an expense category or establish a merchant rule in 3 taps or fewer.
- **SC-007**: App updates can be installed over existing versions with 100% retention of historical monthly records, categories, and widget settings.

## Assumptions

- **Operating System Baseline**: The application targets Android 8.0+ (API 26+) through Android 15 (API 35), leveraging modern Android notification listeners, SMS broadcast receivers, and Glance/AppWidget APIs.
- **Strict Privacy Stance**: No account registration, cloud sync, or external backend server is required; all financial insights belong entirely to the user on their device.
- **Deduplication Window**: A default temporal window of 10 minutes between matching SMS and Email events is sufficient to capture dual alerts from identical bank transactions while preventing collision with separate purchases.
- **Non-Financial Filtering**: A robust heuristics engine filters out OTPs, promotional coupons, balance check alerts without transactions, and service notifications before parsing.
- **Currency Handling**: The default currency is determined by the device's locale, with user override options in settings. Multi-currency transactions convert to the base currency when conversion rates are supplied in the alert text, or log the primary billing amount.
