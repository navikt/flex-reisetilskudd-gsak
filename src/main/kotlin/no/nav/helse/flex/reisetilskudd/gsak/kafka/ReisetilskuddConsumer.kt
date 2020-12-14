package no.nav.helse.flex.reisetilskudd.gsak.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import no.nav.helse.flex.reisetilskudd.gsak.log
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ReisetilskuddConsumer {

    val log = log()

    @KafkaListener(topics = ["flex.aapen-reisetilskudd"])
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val melding = cr.value()

        log.info("Mottok: $melding")
        acknowledgment.acknowledge()

    }
}


