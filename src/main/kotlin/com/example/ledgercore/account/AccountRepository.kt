package com.example.ledgercore.account

import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.CurrencyCode

interface AccountRepository {
    suspend fun create(currency: CurrencyCode, openingBalanceMinor: Long): Account

    suspend fun findById(id: AccountId): Account?

    suspend fun findByIdForUpdate(id: AccountId): Account?

    suspend fun updateBalance(id: AccountId, newBalanceMinor: Long): Boolean
}
