package com.example.ledgercore.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "ledgercore")
data class LedgerCoreProperties(
    val hmacSecret: String,
)

@Configuration
@EnableConfigurationProperties(LedgerCoreProperties::class)
class LedgerCorePropertiesConfig
