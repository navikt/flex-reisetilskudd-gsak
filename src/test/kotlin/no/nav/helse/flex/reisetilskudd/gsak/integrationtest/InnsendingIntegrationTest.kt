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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import java.net.URI
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

    @Autowired
    private lateinit var simpleRestTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer


    @Before
    fun init() {
        mockServer = MockRestServiceServer.createServer(simpleRestTemplate)
    }


    @Test
    fun `SENDT søknad prosesseres og lagres i databasen`() {
        reisetilskuddConsumer.meldinger = 0

        mockServer.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(URI("http://flex-reisetilskudd-pdfgen/api/v1/genpdf/reisetilskudd/reisetilskudd")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_PDF)
                        .body("PDF bytes :)"))

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

        mockServer.verify()
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
