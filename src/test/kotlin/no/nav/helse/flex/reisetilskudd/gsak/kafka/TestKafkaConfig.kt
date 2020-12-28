package no.nav.helse.flex.reisetilskudd.gsak.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestKafkaConfig {

    @Bean
    fun kafkaProducer(properties: KafkaProperties): KafkaProducer<String, String> {
        return KafkaProducer(properties.buildProducerProperties())
    }
}
