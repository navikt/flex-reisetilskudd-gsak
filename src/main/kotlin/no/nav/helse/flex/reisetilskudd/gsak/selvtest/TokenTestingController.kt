package no.nav.helse.flex.reisetilskudd.gsak.selvtest

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController



@RestController
@Unprotected
class TokingTestingController(val clientConfigurationProperties: ClientConfigurationProperties,
                              val oAuth2AccessTokenService: OAuth2AccessTokenService) {

    @GetMapping("/internal/token", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun tokenYo(): ResponseEntity<String> {

        val registrationName = "flex-fss-proxy-client-credentials"
        val clientProperties = clientConfigurationProperties.registration[registrationName]
                ?: throw RuntimeException("Fant ikke config for $registrationName")
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)

        return  ResponseEntity.ok(response.toString())
    }

}
