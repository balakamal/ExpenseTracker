package com.innovatex.spendwell.data.seeder

/**
 * Production environment: Spendwell strictly uses authentic local SQLite ledger
 * populated via on-device SMS parsing and local user entries.
 * Synthetic demo data seeding is intentionally disabled.
 */
object SampleFinancialDataSeeder {
    fun seedIfEmpty(database: Any) {
        // No-op for production integrity
    }
}
