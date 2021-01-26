package no.nav.helse.flex.reisetilskudd.gsak.kafka

import no.nav.helse.flex.reisetilskudd.gsak.config.FLEX_APEN_REISETILSKUDD_TOPIC
import no.nav.helse.flex.reisetilskudd.gsak.innsending.InnsendingService
import no.nav.helse.flex.reisetilskudd.gsak.log
import no.nav.helse.flex.reisetilskudd.gsak.selvtest.ApplicationState
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.event.EventListener
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.event.ConsumerStoppedEvent
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ReisetilskuddConsumer(
    private val innsendingService: InnsendingService,
    private val applicationState: ApplicationState
) {

    private val log = log()

    // For å lettere vente i testene
    var meldinger = 0

    @KafkaListener(topics = [FLEX_APEN_REISETILSKUDD_TOPIC])
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {

        log.info("Behandler reisetilskuddsøknad ${cr.key()}")
        try {
            innsendingService.behandleReisetilskuddSoknad(cr.value())
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("Feil ved mottak av record med key: ${cr.key()} offset: ${cr.offset()} partition: ${cr.partition()}", e)
            throw e
        } finally {
            meldinger++
        }
    }

    @EventListener
    fun eventHandler(event: ConsumerStoppedEvent) {
        log.warn("Consumer stoppet grunnet ${event.reason}, restarter app")
        applicationState.iAmDead()
    }
}
