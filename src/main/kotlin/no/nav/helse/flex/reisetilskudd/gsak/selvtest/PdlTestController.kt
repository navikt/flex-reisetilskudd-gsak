package no.nav.helse.flex.reisetilskudd.gsak.selvtest

import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.PdlClient
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
class PdlTestController(val pdlClient: PdlClient) {

    @GetMapping("/pdl/{fnr}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun pdl(@PathVariable fnr: String): ResponseEntity<String> {
        return ResponseEntity.ok(pdlClient.hentPerson(fnr).toString())
    }
}
