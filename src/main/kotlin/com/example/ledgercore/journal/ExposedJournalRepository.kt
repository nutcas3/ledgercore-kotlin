package com.example.ledgercore.journal

import com.example.ledgercore.account.AccountsTable
import com.example.ledgercore.common.Page
import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.EntryStatus
import com.example.ledgercore.domain.IdempotencyKey
import com.example.ledgercore.domain.JournalEntryId
import com.example.ledgercore.domain.Posting
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedJournalRepository(
    private val database: Database,
) : JournalRepository {

    override suspend fun existsByIdempotencyKey(key: IdempotencyKey): Boolean =
        newSuspendedTransaction(db = database) {
            JournalEntriesTable.selectAll()
                .where { JournalEntriesTable.idempotencyKey eq key.value }
                .count() > 0
        }

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): JournalEntryRecord? =
        newSuspendedTransaction(db = database) {
            JournalEntriesTable.selectAll()
                .where { JournalEntriesTable.idempotencyKey eq key.value }
                .singleOrNull()
                ?.toJournalEntryRecord()
        }

    override suspend fun insertEntry(
        id: JournalEntryId,
        idempotencyKey: IdempotencyKey,
        status: EntryStatus,
    ) {
        newSuspendedTransaction(db = database) {
            JournalEntriesTable.insert {
                it[JournalEntriesTable.id] = EntityID(id.value, JournalEntriesTable)
                it[JournalEntriesTable.idempotencyKey] = idempotencyKey.value
                it[JournalEntriesTable.status] = status
            }
        }
    }

    override suspend fun insertPostings(entryId: JournalEntryId, postings: List<Posting>) {
        newSuspendedTransaction(db = database) {
            PostingsTable.batchInsert(postings) { posting ->
                this[PostingsTable.entryId] = EntityID(entryId.value, JournalEntriesTable)
                this[PostingsTable.accountId] = EntityID(posting.accountId.value, AccountsTable)
                this[PostingsTable.direction] = posting.direction
                this[PostingsTable.amountMinor] = posting.amount.minorUnits
                this[PostingsTable.currency] = posting.amount.currency.value
            }
        }
    }

    override suspend fun updateStatus(id: JournalEntryId, status: EntryStatus) {
        newSuspendedTransaction(db = database) {
            JournalEntriesTable.update({ JournalEntriesTable.id eq id.value }) {
                it[JournalEntriesTable.status] = status
            }
        }
    }

    override suspend fun findPostingsByAccount(accountId: AccountId, page: Int, limit: Int): Page<PostingRecord> =
        newSuspendedTransaction(db = database) {
            val query = PostingsTable.selectAll().where { PostingsTable.accountId eq accountId.value }
            val total = query.count()
            val data = query
                .orderBy(PostingsTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .offset(((page - 1) * limit).toLong())
                .map { it.toPostingRecord() }
            Page(data = data, total = total, page = page, limit = limit)
        }

    private fun ResultRow.toJournalEntryRecord() = JournalEntryRecord(
        id = JournalEntryId(this[JournalEntriesTable.id].value),
        idempotencyKey = IdempotencyKey(this[JournalEntriesTable.idempotencyKey]),
        status = this[JournalEntriesTable.status],
        createdAt = this[JournalEntriesTable.createdAt],
    )

    private fun ResultRow.toPostingRecord() = PostingRecord(
        id = this[PostingsTable.id].value,
        entryId = JournalEntryId(this[PostingsTable.entryId].value),
        accountId = AccountId(this[PostingsTable.accountId].value),
        direction = this[PostingsTable.direction],
        amountMinor = this[PostingsTable.amountMinor],
        currency = this[PostingsTable.currency],
        createdAt = this[PostingsTable.createdAt],
    )
}
