package com.example.ledgercore.journal

import com.example.ledgercore.common.Page
import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.EntryStatus
import com.example.ledgercore.domain.IdempotencyKey
import com.example.ledgercore.domain.JournalEntryId
import com.example.ledgercore.domain.Posting

interface JournalRepository {
    suspend fun existsByIdempotencyKey(key: IdempotencyKey): Boolean

    suspend fun findByIdempotencyKey(key: IdempotencyKey): JournalEntryRecord?

    suspend fun insertEntry(
        id: JournalEntryId,
        idempotencyKey: IdempotencyKey,
        status: EntryStatus,
    )

    suspend fun insertPostings(entryId: JournalEntryId, postings: List<Posting>)

    suspend fun updateStatus(id: JournalEntryId, status: EntryStatus)

    suspend fun findPostingsByAccount(accountId: AccountId, page: Int, limit: Int): Page<PostingRecord>
}
