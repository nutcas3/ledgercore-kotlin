package com.example.ledgercore.account

import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.CurrencyCode
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedAccountRepository(
    private val database: Database,
) : AccountRepository {

    override suspend fun create(currency: CurrencyCode, openingBalanceMinor: Long): Account =
        newSuspendedTransaction(db = database) {
            val newId = AccountsTable.insertAndGetId {
                it[AccountsTable.currency] = currency.value
                it[balanceMinor] = openingBalanceMinor
            }
            AccountsTable.selectAll()
                .where { AccountsTable.id eq newId }
                .single()
                .toAccount()
        }

    override suspend fun findById(id: AccountId): Account? =
        newSuspendedTransaction(db = database) {
            AccountsTable.selectAll()
                .where { AccountsTable.id eq id.value }
                .singleOrNull()
                ?.toAccount()
        }

    override suspend fun findByIdForUpdate(id: AccountId): Account? =
        newSuspendedTransaction(db = database) {
            AccountsTable.selectAll()
                .where { AccountsTable.id eq id.value }
                .forUpdate()
                .singleOrNull()
                ?.toAccount()
        }

    override suspend fun updateBalance(id: AccountId, newBalanceMinor: Long): Boolean =
        newSuspendedTransaction(db = database) {
            AccountsTable.update({ AccountsTable.id eq id.value }) {
                it[balanceMinor] = newBalanceMinor
                it[version] = SqlExpressionBuilder.run { AccountsTable.version + 1 }
            } > 0
        }

    private fun ResultRow.toAccount() = Account(
        id = AccountId(this[AccountsTable.id].value),
        currency = CurrencyCode(this[AccountsTable.currency]),
        balanceMinor = this[AccountsTable.balanceMinor],
        version = this[AccountsTable.version],
        createdAt = this[AccountsTable.createdAt],
    )
}
