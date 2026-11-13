package com.example.ledgercore.account

import com.example.ledgercore.common.Page
import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.CurrencyCode
import com.example.ledgercore.domain.Money
import com.example.ledgercore.journal.JournalRepository
import com.example.ledgercore.journal.PostingRecord
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CreateAccountRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO 4217 code")
    val currency: String,
    @field:Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "openingBalance must be a positive decimal string")
    val openingBalance: String? = null,
)

data class AccountResponse(
    val id: String,
    val currency: String,
    val balance: String,
    val balanceMinor: Long,
    val version: Int,
)

data class PostingResponse(
    val id: String,
    val entryId: String,
    val direction: String,
    val amount: String,
    val amountMinor: Long,
    val currency: String,
    val createdAt: String,
)

@RestController
@RequestMapping("/v1/accounts")
class AccountController(
    private val accountRepository: AccountRepository,
    private val journalRepository: JournalRepository,
) {

    @PostMapping
    suspend fun create(@Valid @RequestBody body: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val currency = CurrencyCode(body.currency)
        val openingMinor = body.openingBalance?.let { Money.parse(it, currency).minorUnits } ?: 0L
        val account = accountRepository.create(currency, openingMinor)
        return ResponseEntity.status(HttpStatus.CREATED).body(account.toResponse())
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: UUID): ResponseEntity<AccountResponse> {
        val account = accountRepository.findById(AccountId(id))
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(account.toResponse())
    }

    @GetMapping("/{id}/entries")
    suspend fun entries(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
    ): Page<PostingResponse> =
        journalRepository.findPostingsByAccount(AccountId(id), page, limit)
            .let { p -> p.copy(data = p.data.map { it.toResponse() }) }

    private fun Account.toResponse() = AccountResponse(
        id = id.value.toString(),
        currency = currency.value,
        balance = Money(balanceMinor, currency).toMajor().toPlainString(),
        balanceMinor = balanceMinor,
        version = version,
    )

    private fun PostingRecord.toResponse() = PostingResponse(
        id = id.toString(),
        entryId = entryId.value.toString(),
        direction = direction.name,
        amount = Money(amountMinor, CurrencyCode(currency)).toMajor().toPlainString(),
        amountMinor = amountMinor,
        currency = currency,
        createdAt = createdAt.toString(),
    )
}
