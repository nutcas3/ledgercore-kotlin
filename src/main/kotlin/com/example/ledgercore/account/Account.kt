package com.example.ledgercore.account

import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.CurrencyCode
import kotlinx.datetime.Instant

data class Account(
    val id: AccountId,
    val currency: CurrencyCode,
    val balanceMinor: Long,
    val version: Int,
    val createdAt: Instant,
)
