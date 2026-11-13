package com.example.ledgercore.domain

sealed interface LedgerResult {
    data class Success(val entryId: JournalEntryId) : LedgerResult

    data class DuplicateRequest(val existingEntryId: JournalEntryId?) : LedgerResult

    data object InsufficientFunds : LedgerResult

    data object CurrencyMismatch : LedgerResult

    data class AccountNotFound(val accountId: AccountId) : LedgerResult
}
