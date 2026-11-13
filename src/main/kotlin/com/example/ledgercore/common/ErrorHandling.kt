package com.example.ledgercore.common

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val error: String,
    val message: String,
)

@RestControllerAdvice
class LedgerExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleDomainValidation(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse("INVALID_TRANSACTION_STATE", ex.message ?: "Validation failed"),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                "VALIDATION_FAILED",
                ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" },
            ),
        )

    @ExceptionHandler(MissingRequestHeaderException::class, HttpMessageNotReadableException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse("BAD_REQUEST", ex.message ?: "Malformed request"),
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled error", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"),
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LedgerExceptionHandler::class.java)
    }
}
