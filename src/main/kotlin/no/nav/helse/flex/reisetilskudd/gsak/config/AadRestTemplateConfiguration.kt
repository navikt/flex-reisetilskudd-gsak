package no.nav.helse.flex.reisetilskudd.gsak.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate

@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
@Configuration
@Profile("default")
class AadRestTemplateConfiguration {

    @Bean
    fun dokarkivRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): RestTemplate =
        downstreamRestTemplate(
            registrationName = "dokarkiv-client-credentials",
            restTemplateBuilder = restTemplateBuilder,
            clientConfigurationProperties = clientConfigurationProperties,
            oAuth2AccessTokenService = oAuth2AccessTokenService,
        )

    @Bean
    fun oppgaveRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): RestTemplate =
        downstreamRestTemplate(
            registrationName = "oppgave-client-credentials",
            restTemplateBuilder = restTemplateBuilder,
            clientConfigurationProperties = clientConfigurationProperties,
            oAuth2AccessTokenService = oAuth2AccessTokenService,
        )

    @Bean
    fun flexFssProxyRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): RestTemplate =
        downstreamRestTemplate(
            registrationName = "flex-fss-proxy-client-credentials",
            restTemplateBuilder = restTemplateBuilder,
            clientConfigurationProperties = clientConfigurationProperties,
            oAuth2AccessTokenService = oAuth2AccessTokenService,
        )

    @Bean
    fun flexBucketUploaderRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): RestTemplate =
        downstreamRestTemplate(
            registrationName = "flex-bucket-uploader-client-credentials",
            restTemplateBuilder = restTemplateBuilder,
            clientConfigurationProperties = clientConfigurationProperties,
            oAuth2AccessTokenService = oAuth2AccessTokenService,
        )

    private fun downstreamRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
        registrationName: String
    ): RestTemplate {
        val clientProperties = clientConfigurationProperties.registration[registrationName]
            ?: throw RuntimeException("Fant ikke config for $registrationName")
        return restTemplateBuilder
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.accessToken)
            execution.execute(request, body)
        }
    }
}
