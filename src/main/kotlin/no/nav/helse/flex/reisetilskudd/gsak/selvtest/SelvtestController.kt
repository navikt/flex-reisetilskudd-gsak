package no.nav.helse.flex.reisetilskudd.gsak.selvtest

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

const val APPLICATION_LIVENESS = "Application is alive!"
const val APPLICATION_READY = "Application is ready!"

@RestController
@Unprotected
class SelvtestController(private val applicationState: ApplicationState) {

    @GetMapping("/internal/isAlive", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isAlive(): ResponseEntity<String> {
        return if (applicationState.isAlive()) {
            ResponseEntity.ok(APPLICATION_LIVENESS)
        } else {
            ResponseEntity("Noe er galt", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/internal/isReady", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): ResponseEntity<String> {
        return ResponseEntity.ok(APPLICATION_READY)
    }
}
