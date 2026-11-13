package com.example.ledgercore.journal

import com.example.ledgercore.account.AccountsTable
import com.example.ledgercore.domain.Direction
import com.example.ledgercore.domain.EntryStatus
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object JournalEntriesTable : UUIDTable("journal_entries") {
    val idempotencyKey = varchar("idempotency_key", 128).uniqueIndex()
    val status = enumerationByName<EntryStatus>("status", 16)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
}

object PostingsTable : UUIDTable("postings") {
    val entryId = reference("entry_id", JournalEntriesTable.id)
    val accountId = reference("account_id", AccountsTable.id)
    val direction = enumerationByName<Direction>("direction", 6)
    val amountMinor = long("amount_minor")
    val currency = char("currency", 3)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
}
