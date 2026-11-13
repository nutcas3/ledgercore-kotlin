package com.example.ledgercore.transfer

import com.example.ledgercore.account.Account
import com.example.ledgercore.account.AccountRepository
import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.Direction
import com.example.ledgercore.domain.EntryStatus
import com.example.ledgercore.domain.JournalEntry
import com.example.ledgercore.domain.JournalEntryId
import com.example.ledgercore.domain.LedgerEntryStatus
import com.example.ledgercore.domain.LedgerResult
import com.example.ledgercore.domain.Posting
import com.example.ledgercore.journal.JournalRepository
import java.sql.Connection
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LedgerService(
    private val database: Database,
    private val accountRepository: AccountRepository,
    private val journalRepository: JournalRepository,
) {

    suspend fun executeTransfer(request: TransferRequest): LedgerResult {
        var attempts = 0
        while (true) {
            try {
                return attemptTransfer(request)
            } catch (e: ExposedSQLException) {
                when {
                    e.sqlState == UNIQUE_VIOLATION -> return LedgerResult.DuplicateRequest(
                        journalRepository.findByIdempotencyKey(request.idempotencyKey)?.id,
                    )
                    e.sqlState == SERIALIZATION_FAILURE && ++attempts < MAX_ATTEMPTS -> {
                        logger.warn("Serialization failure on transfer, retry {}/{}", attempts, MAX_ATTEMPTS)
                        delay(RETRY_BASE_DELAY_MS * attempts)
                    }
                    else -> throw e
                }
            }
        }
    }

    private suspend fun attemptTransfer(request: TransferRequest): LedgerResult =
        newSuspendedTransaction(
            db = database,
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
        ) {
            journalRepository.findByIdempotencyKey(request.idempotencyKey)?.let {
                return@newSuspendedTransaction LedgerResult.DuplicateRequest(it.id)
            }

            val (firstId, secondId) = listOf(request.sourceAccountId, request.destinationAccountId)
                .sortedBy { it.value.toString() }
            val locked = buildMap {
                put(firstId, accountRepository.findByIdForUpdate(firstId))
                put(secondId, accountRepository.findByIdForUpdate(secondId))
            }

            val source = locked.getValue(request.sourceAccountId)
                ?: return@newSuspendedTransaction LedgerResult.AccountNotFound(request.sourceAccountId)
            val destination = locked.getValue(request.destinationAccountId)
                ?: return@newSuspendedTransaction LedgerResult.AccountNotFound(request.destinationAccountId)

            if (!currenciesMatch(source, destination, request)) {
                return@newSuspendedTransaction LedgerResult.CurrencyMismatch
            }
            if (source.balanceMinor < request.money.minorUnits) {
                return@newSuspendedTransaction LedgerResult.InsufficientFunds
            }

            val entryId = JournalEntryId.random()
            journalRepository.insertEntry(entryId, request.idempotencyKey, EntryStatus.POSTED)

            val entry = JournalEntry(
                id = entryId,
                idempotencyKey = request.idempotencyKey,
                postings = listOf(
                    Posting(source.id, Direction.CREDIT, request.money),
                    Posting(destination.id, Direction.DEBIT, request.money),
                ),
                status = LedgerEntryStatus.Posted(Clock.System.now()),
            )
            journalRepository.insertPostings(entryId, entry.postings)

            accountRepository.updateBalance(source.id, source.balanceMinor - request.money.minorUnits)
            accountRepository.updateBalance(destination.id, destination.balanceMinor + request.money.minorUnits)

            LedgerResult.Success(entryId)
        }

    private fun currenciesMatch(source: Account, destination: Account, request: TransferRequest): Boolean =
        source.currency == request.money.currency && destination.currency == request.money.currency

    companion object {
        private val logger = LoggerFactory.getLogger(LedgerService::class.java)

        private const val UNIQUE_VIOLATION = "23505"
        private const val SERIALIZATION_FAILURE = "40001"
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 10L
    }
}
