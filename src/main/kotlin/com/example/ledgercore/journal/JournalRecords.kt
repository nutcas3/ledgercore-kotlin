package com.example.ledgercore.journal

import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.Direction
import com.example.ledgercore.domain.EntryStatus
import com.example.ledgercore.domain.IdempotencyKey
import com.example.ledgercore.domain.JournalEntryId
import java.util.UUID
import kotlinx.datetime.Instant

data class JournalEntryRecord(
    val id: JournalEntryId,
    val idempotencyKey: IdempotencyKey,
    val status: EntryStatus,
    val createdAt: Instant,
)

data class PostingRecord(
    val id: UUID,
    val entryId: JournalEntryId,
    val accountId: AccountId,
    val direction: Direction,
    val amountMinor: Long,
    val currency: String,
    val createdAt: Instant,
)
