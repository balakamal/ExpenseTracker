package com.innovatex.spendwell.analytics

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.Transaction

data class ReconciliationResult(
    val matchedTransactions: List<Pair<Transaction, Transaction>>,
    val unmatchedStatementTransactions: List<Transaction>,
    val unmatchedLedgerTransactions: List<Transaction>,
    val statementTotal: Money,
    val ledgerTotal: Money,
    val netDiscrepancy: Money
)

object ReconciliationEngine {

    fun reconcile(
        ledgerTransactions: List<Transaction>,
        statementTransactions: List<Transaction>
    ): ReconciliationResult {
        val matched = mutableListOf<Pair<Transaction, Transaction>>()
        val remainingStatement = statementTransactions.toMutableList()
        val remainingLedger = ledgerTransactions.toMutableList()

        // Pass 1: Match by reference ID / UTR
        val statementWithRef = remainingStatement.filter { !it.referenceId.isNullOrBlank() }
        for (stTx in statementWithRef) {
            val match = remainingLedger.firstOrNull { it.referenceId == stTx.referenceId }
            if (match != null) {
                matched.add(Pair(match, stTx))
                remainingLedger.remove(match)
                remainingStatement.remove(stTx)
            }
        }

        // Pass 2: Match by exact amount and timestamp within 24 hours
        val dayMs = 24 * 60 * 60 * 1000L
        val statementIter = remainingStatement.iterator()
        while (statementIter.hasNext()) {
            val stTx = statementIter.next()
            val match = remainingLedger.firstOrNull {
                it.amount == stTx.amount &&
                        it.type == stTx.type &&
                        kotlin.math.abs(it.timestamp - stTx.timestamp) <= dayMs
            }
            if (match != null) {
                matched.add(Pair(match, stTx))
                remainingLedger.remove(match)
                statementIter.remove()
            }
        }

        var statementTotal = Money.ZERO
        statementTransactions.forEach { statementTotal += it.amount }

        var ledgerTotal = Money.ZERO
        ledgerTransactions.forEach { ledgerTotal += it.amount }

        val discrepancy = (statementTotal - ledgerTotal).abs()

        return ReconciliationResult(
            matchedTransactions = matched,
            unmatchedStatementTransactions = remainingStatement,
            unmatchedLedgerTransactions = remainingLedger,
            statementTotal = statementTotal,
            ledgerTotal = ledgerTotal,
            netDiscrepancy = discrepancy
        )
    }
}
