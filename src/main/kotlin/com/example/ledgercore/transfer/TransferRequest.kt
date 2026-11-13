package com.example.ledgercore.transfer

import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.IdempotencyKey
import com.example.ledgercore.domain.Money

data class TransferRequest(
    val sourceAccountId: AccountId,
    val destinationAccountId: AccountId,
    val money: Money,
    val idempotencyKey: IdempotencyKey,
)
