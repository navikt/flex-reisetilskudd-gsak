package no.nav.helse.flex.reisetilskudd.gsak.configuration

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate


@Configuration
@Profile("test")
class OverrideAadRestTemplateConfiguration {


    @Bean
    fun dokarkivRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
            restTemplateBuilder.build()

    @Bean
    fun flexFssProxyRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
            restTemplateBuilder.build()

}
