package com.example.ledgercore.common

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/v1/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")
}
