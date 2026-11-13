package com.example.ledgercore.domain

import kotlinx.datetime.Instant

enum class Direction {
    DEBIT,
    CREDIT,
}

enum class EntryStatus {
    PENDING,
    POSTED,
    FAILED,
}

sealed interface LedgerEntryStatus {
    data object Pending : LedgerEntryStatus

    data class Posted(val at: Instant) : LedgerEntryStatus

    data class Failed(val reason: String) : LedgerEntryStatus
}

data class Posting(
    val accountId: AccountId,
    val direction: Direction,
    val amount: Money,
)

data class JournalEntry(
    val id: JournalEntryId,
    val idempotencyKey: IdempotencyKey,
    val postings: List<Posting>,
    val status: LedgerEntryStatus,
) {
    init {
        require(postings.size >= 2) { "A journal entry requires at least two postings" }
        val netByCurrency = postings
            .groupBy { it.amount.currency }
            .mapValues { (_, currencyPostings) ->
                currencyPostings.fold(0L) { net, posting ->
                    when (posting.direction) {
                        Direction.DEBIT -> net + posting.amount.minorUnits
                        Direction.CREDIT -> net - posting.amount.minorUnits
                    }
                }
            }
        require(netByCurrency.values.all { it == 0L }) {
            "Journal entry does not balance per currency: $netByCurrency"
        }
    }
}
