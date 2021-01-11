package no.nav.helse.flex.reisetilskudd.gsak.integrationtest

import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.GetPersonResponse
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.HentPerson
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.Navn
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.ResponseData
import no.nav.helse.flex.reisetilskudd.gsak.config.FLEX_APEN_REISETILSKUDD_TOPIC
import no.nav.helse.flex.reisetilskudd.gsak.database.InnsendingDao
import no.nav.helse.flex.reisetilskudd.gsak.domain.*
import no.nav.helse.flex.reisetilskudd.gsak.kafka.ReisetilskuddConsumer
import no.nav.helse.flex.reisetilskudd.gsak.objectMapper
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
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

@ExperimentalUnsignedTypes
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = [FLEX_APEN_REISETILSKUDD_TOPIC]
)
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

    @Autowired
    private lateinit var dokarkivRestTemplate: RestTemplate

    @Autowired
    private lateinit var flexFssProxyRestTemplate: RestTemplate

    @Autowired
    private lateinit var flexBucketUploaderRestTemplate: RestTemplate

    private lateinit var flexFssProxyMockServer: MockRestServiceServer
    private lateinit var pdfGenMockServer: MockRestServiceServer
    private lateinit var dokarkivMockServer: MockRestServiceServer
    private lateinit var flexBucketUploaderMockServer: MockRestServiceServer

    @Before
    fun init() {
        pdfGenMockServer = MockRestServiceServer.createServer(simpleRestTemplate)
        dokarkivMockServer = MockRestServiceServer.createServer(dokarkivRestTemplate)
        flexFssProxyMockServer = MockRestServiceServer.createServer(flexFssProxyRestTemplate)
        flexBucketUploaderMockServer = MockRestServiceServer.createServer(flexBucketUploaderRestTemplate)
    }

    @Test
    fun `SENDT søknad prosesseres og lagres i databasen`() {
        reisetilskuddConsumer.meldinger = 0

        val jpostresponse = JournalpostResponse(dokumenter = emptyList(), journalpostId = "w234", journalpostferdigstilt = true)

        dokarkivMockServer.expect(
            once(),
            requestTo(URI("http://dokarkiv/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jpostresponse.serialisertTilString())
            )

        val getPersonResponse = GetPersonResponse(
            errors = emptyList(),
            data = ResponseData(
                hentPerson = HentPerson(navn = listOf(Navn(fornavn = "For", mellomnavn = "Midt", etternavn = "Efter")))
            )
        )

        flexFssProxyMockServer.expect(
            once(),
            requestTo(URI("http://flex-fss-proxy/api/pdl/graphql"))
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(getPersonResponse.serialisertTilString())
            )

        val kvittering = Kvittering(
            kvitteringId = "abc",
            blobId = "1234",
            navn = "hva",
            datoForReise = LocalDate.of(2020, 12, 24),
            storrelse = 123L,
            belop = 10000,
            transportmiddel = Transportmiddel.EGEN_BIL
        )
        val soknad = Reisetilskudd(
            status = ReisetilskuddStatus.SENDT,
            fnr = "12345600000",
            reisetilskuddId = UUID.randomUUID().toString(),
            kvitteringer = listOf(kvittering)
        )

        pdfGenMockServer.expect(
            once(),
            requestTo(URI("http://flex-reisetilskudd-pdfgen/api/v1/genpdf/reisetilskudd/reisetilskudd"))
        )
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.navn", `is`("For Midt Efter")))
            .andExpect(jsonPath("$.reisetilskuddId", `is`(soknad.reisetilskuddId)))
            .andExpect(jsonPath("$.kvitteringer[0].kvitteringId", `is`(kvittering.kvitteringId)))
            .andExpect(jsonPath("$.kvitteringer[0].b64data", `is`("3q2+7w==")))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body("PDF bytes :)")
            )

        flexBucketUploaderMockServer.expect(
            once(),
            requestTo(URI("http://flex-bucket-uploader/maskin/kvittering/${kvittering.blobId}"))
        )
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(ubyteArrayOf(0xDEu, 0xADu, 0xBEu, 0xEFu).toByteArray())
            )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.reisetilskuddId, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingDao.hentInnsending(soknad.reisetilskuddId)!!

        assertThat(innsending.reisetilskuddId, `is`(soknad.reisetilskuddId))
        assertThat(innsending.fnr, `is`(soknad.fnr))
        assertThat(innsending.journalpostId, `is`(jpostresponse.journalpostId))
        assertThat(innsending.saksId, `is`("TODO"))
        assertThat(Instant.now().toEpochMilli() - innsending.opprettet.toEpochMilli(), lessThan(2000))

        pdfGenMockServer.verify()
        flexBucketUploaderMockServer.verify()
        dokarkivMockServer.verify()
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

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)
