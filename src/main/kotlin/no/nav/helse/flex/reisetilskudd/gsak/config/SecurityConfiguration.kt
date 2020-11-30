package no.nav.helse.flex.reisetilskudd.gsak.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.util.*

@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
@Configuration
internal class OAuth2Configuration {

    @Bean
    fun downstreamResourceRestTemplate(restTemplateBuilder: RestTemplateBuilder,
                                       clientConfigurationProperties: ClientConfigurationProperties,
                                       oAuth2AccessTokenService: OAuth2AccessTokenService): RestTemplate {
        val registrationName = "flex-fss-proxy-client-credentials"
        val clientProperties = clientConfigurationProperties.registration[registrationName]
                ?: throw RuntimeException("Fant ikke config for $registrationName")
        return restTemplateBuilder
                .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
                .build()
    }

    private fun bearerTokenInterceptor(clientProperties: ClientProperties,
                                       oAuth2AccessTokenService: OAuth2AccessTokenService): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.accessToken)
            execution.execute(request, body)
        }
    }
}
