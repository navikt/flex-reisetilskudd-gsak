package no.nav.helse.flex.reisetilskudd.gsak.integrationtest

import no.nav.helse.flex.reisetilskudd.gsak.config.FLEX_APEN_REISETILSKUDD_TOPIC
import no.nav.helse.flex.reisetilskudd.gsak.database.InnsendingDao
import no.nav.helse.flex.reisetilskudd.gsak.domain.Innsending
import no.nav.helse.flex.reisetilskudd.gsak.domain.Reisetilskudd
import no.nav.helse.flex.reisetilskudd.gsak.domain.ReisetilskuddStatus
import no.nav.helse.flex.reisetilskudd.gsak.kafka.ReisetilskuddConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit


@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = [FLEX_APEN_REISETILSKUDD_TOPIC])
@RunWith(SpringRunner::class)
class InnsendingIntegrationTest {


    @Autowired
    private lateinit var producer: KafkaProducer<String, String>

    @Autowired
    private lateinit var reisetilskuddConsumer: ReisetilskuddConsumer

    @Autowired
    private lateinit var innsendingDao: InnsendingDao

    @Test
    fun `SENDT søknad prosesseres og lagres i databasen`() {
        reisetilskuddConsumer.meldinger = 0

        val soknad = Reisetilskudd(
                status = ReisetilskuddStatus.SENDT,
                fnr = "12345600000",
                reisetilskuddId = UUID.randomUUID().toString()
        )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.reisetilskuddId, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingDao.hentInnsending(soknad.reisetilskuddId)!!

        assertThat(innsending.reisetilskuddId, `is`(soknad.reisetilskuddId))
        assertThat(innsending.fnr, `is`(soknad.fnr))
        assertThat(innsending.journalpostId, `is`("TODO"))
        assertThat(innsending.saksId, `is`("TODO"))
        assertThat(Instant.now().toEpochMilli() - innsending.opprettet.toEpochMilli(), lessThan(2000))
    }

    @Test
    fun `FREMTIDIG prosesseres ikke og lagres ikke `() {
        reisetilskuddConsumer.meldinger = 0

        val soknad = Reisetilskudd(
                status = ReisetilskuddStatus.FREMTIDIG,
                fnr = "12345600000",
                reisetilskuddId = UUID.randomUUID().toString()
        )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.reisetilskuddId, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingDao.hentInnsending(soknad.reisetilskuddId)

        assertThat(innsending, nullValue())
    }


    @Test
    fun `allerede SENDT søknad prosesseres ikke og lagres i databasen`() {
        reisetilskuddConsumer.meldinger = 0

        val eksisterendeInnsending = Innsending(
                reisetilskuddId = UUID.randomUUID().toString(),
                opprettet = Instant.now(),
                journalpostId = "jpost12",
                saksId = "sakem",
                fnr = "12345600000",
        )

        innsendingDao.lagreInnsending(eksisterendeInnsending)

        val soknad = Reisetilskudd(
                status = ReisetilskuddStatus.SENDT,
                fnr = eksisterendeInnsending.fnr,
                reisetilskuddId = eksisterendeInnsending.reisetilskuddId,
        )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.reisetilskuddId, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingDao.hentInnsending(soknad.reisetilskuddId)!!

        assertThat(innsending, `is`(eksisterendeInnsending))
    }
}
