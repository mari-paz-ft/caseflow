package com.caseflow.auth.interfaces.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource

@RestController
class AuthHealthController(
    private val dataSource: DataSource
) {
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> = try {
        dataSource.connection.use { connection -> check(connection.isValid(2)) }
        ResponseEntity.ok(mapOf("status" to "UP"))
    } catch (_: Exception) {
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("status" to "DOWN"))
    }
}
