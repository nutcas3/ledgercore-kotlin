package com.example.ledgercore.transfer

import com.example.ledgercore.domain.AccountId
import com.example.ledgercore.domain.CurrencyCode
import com.example.ledgercore.domain.IdempotencyKey
import com.example.ledgercore.domain.LedgerResult
import com.example.ledgercore.domain.Money
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class TransferRequestDto(
    @field:NotBlank val sourceAccountId: String,
    @field:NotBlank val destinationAccountId: String,
    @field:NotBlank
    @field:Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "amount must be a positive decimal string")
    val amount: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO 4217 code")
    val currency: String,
)

data class TransferResponse(
    val entryId: String?,
    val status: String,
    val duplicate: Boolean,
)

@RestController
@RequestMapping("/v1")
class TransferController(
    private val ledgerService: LedgerService,
) {

    @PostMapping("/transfers")
    suspend fun transfer(
        @Valid @RequestBody body: TransferRequestDto,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
    ): ResponseEntity<TransferResponse> {
        val request = TransferRequest(
            sourceAccountId = AccountId.parse(body.sourceAccountId),
            destinationAccountId = AccountId.parse(body.destinationAccountId),
            money = Money.parse(body.amount, CurrencyCode(body.currency)),
            idempotencyKey = IdempotencyKey(idempotencyKey),
        )

        return when (val result = ledgerService.executeTransfer(request)) {
            is LedgerResult.Success -> ResponseEntity.ok(
                TransferResponse(result.entryId.value.toString(), "POSTED", duplicate = false),
            )
            is LedgerResult.DuplicateRequest -> when (val existing = result.existingEntryId) {
                null -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(TransferResponse(null, "DUPLICATE_IN_PROGRESS", duplicate = true))
                else -> ResponseEntity.ok(
                    TransferResponse(existing.value.toString(), "POSTED", duplicate = true),
                )
            }
            is LedgerResult.InsufficientFunds -> ResponseEntity.unprocessableEntity()
                .body(TransferResponse(null, "INSUFFICIENT_FUNDS", duplicate = false))
            is LedgerResult.CurrencyMismatch -> ResponseEntity.unprocessableEntity()
                .body(TransferResponse(null, "CURRENCY_MISMATCH", duplicate = false))
            is LedgerResult.AccountNotFound -> ResponseEntity.unprocessableEntity()
                .body(TransferResponse(null, "ACCOUNT_NOT_FOUND", duplicate = false))
        }
    }
}
