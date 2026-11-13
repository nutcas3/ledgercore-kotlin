package com.example.ledgercore.domain

import java.math.BigDecimal

data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode,
) {
    init {
        require(minorUnits >= 0) { "Monetary amounts cannot be negative" }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits - other.minorUnits)
    }

    operator fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    fun toMajor(): BigDecimal = BigDecimal(minorUnits).movePointLeft(currency.exponent)

    private fun requireSameCurrency(other: Money) =
        require(currency == other.currency) { "Currency mismatch: ${currency.value} vs ${other.currency.value}" }

    companion object {
        fun ofMinor(minorUnits: Long, currency: CurrencyCode) = Money(minorUnits, currency)

        fun ofMajor(amount: BigDecimal, currency: CurrencyCode): Money {
            val scaled = amount.movePointRight(currency.exponent)
            require(scaled.stripTrailingZeros().scale() <= 0) {
                "Amount $amount has more decimal places than ${currency.value} supports (exponent ${currency.exponent})"
            }
            return Money(scaled.longValueExact(), currency)
        }

        fun parse(amount: String, currency: CurrencyCode): Money =
            ofMajor(BigDecimal(amount), currency)
    }
}
