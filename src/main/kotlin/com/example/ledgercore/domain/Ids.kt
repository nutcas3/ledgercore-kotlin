package com.example.ledgercore.domain

import java.util.UUID

@JvmInline
value class AccountId(val value: UUID) {
    companion object {
        fun random(): AccountId = AccountId(UUID.randomUUID())
        fun parse(raw: String): AccountId = AccountId(UUID.fromString(raw))
    }
}

@JvmInline
value class JournalEntryId(val value: UUID) {
    companion object {
        fun random(): JournalEntryId = JournalEntryId(UUID.randomUUID())
    }
}

@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        require(value.isNotBlank() && value.length <= MAX_LENGTH) {
            "Idempotency key must be between 1 and $MAX_LENGTH characters"
        }
    }

    companion object {
        const val MAX_LENGTH = 128
    }
}
