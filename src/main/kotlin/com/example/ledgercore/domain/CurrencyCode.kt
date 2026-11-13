package com.example.ledgercore.domain

@JvmInline
value class CurrencyCode(val value: String) {
    init {
        require(value.matches(ISO_4217_FORMAT)) { "Currency code must be a 3-letter ISO 4217 code, got '$value'" }
    }

    val exponent: Int
        get() = EXPONENT_OVERRIDES[value] ?: DEFAULT_EXPONENT

    companion object {
        private val ISO_4217_FORMAT = Regex("^[A-Z]{3}$")
        private const val DEFAULT_EXPONENT = 2

        private val EXPONENT_OVERRIDES = mapOf(
            "BHD" to 3, "IQD" to 3, "JOD" to 3, "KWD" to 3, "LYD" to 3,
            "OMR" to 3, "TND" to 3,
            "BIF" to 0, "CLP" to 0, "DJF" to 0, "GNF" to 0, "ISK" to 0,
            "JPY" to 0, "KMF" to 0, "KRW" to 0, "PYG" to 0, "RWF" to 0,
            "UGX" to 0, "VND" to 0, "VUV" to 0, "XAF" to 0, "XOF" to 0,
            "XPF" to 0,
            "CLF" to 4, "UYW" to 4,
        )
    }
}
