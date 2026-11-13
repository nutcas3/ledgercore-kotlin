package com.example.ledgercore.account

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object AccountsTable : UUIDTable("accounts") {
    val currency = char("currency", 3)
    val balanceMinor = long("balance_minor").default(0)
    val version = integer("version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
}
