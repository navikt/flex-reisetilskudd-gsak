package no.nav.helse.flex.reisetilskudd.gsak.integrationtest

import no.nav.helse.flex.reisetilskudd.gsak.KafkaContainerWithProps
import no.nav.helse.flex.reisetilskudd.gsak.PostgreSQLContainerWithProps
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.*
import no.nav.helse.flex.reisetilskudd.gsak.config.FLEX_APEN_REISETILSKUDD_TOPIC
import no.nav.helse.flex.reisetilskudd.gsak.database.InnsendingRepository
import no.nav.helse.flex.reisetilskudd.gsak.domain.*
import no.nav.helse.flex.reisetilskudd.gsak.domain.Tag
import no.nav.helse.flex.reisetilskudd.gsak.kafka.ReisetilskuddConsumer
import no.nav.helse.flex.reisetilskudd.gsak.objectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.ExpectedCount.*
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit

@ExperimentalUnsignedTypes
@SpringBootTest
@ExtendWith(SpringExtension::class)
@Testcontainers
@DirtiesContext
class InnsendingIntegrationTest {

    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainerWithProps()

        @Container
        val kafkaContainer = KafkaContainerWithProps()
    }

    @Autowired
    private lateinit var producer: KafkaProducer<String, String>

    @Autowired
    private lateinit var reisetilskuddConsumer: ReisetilskuddConsumer

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var simpleRestTemplate: RestTemplate

    @Autowired
    private lateinit var dokarkivRestTemplate: RestTemplate

    @Autowired
    private lateinit var oppgaveRestTemplate: RestTemplate

    @Autowired
    private lateinit var flexFssProxyRestTemplate: RestTemplate

    @Autowired
    private lateinit var flexBucketUploaderRestTemplate: RestTemplate

    private lateinit var flexFssProxyMockServer: MockRestServiceServer
    private lateinit var pdfGenMockServer: MockRestServiceServer
    private lateinit var dokarkivMockServer: MockRestServiceServer
    private lateinit var oppgaveMockServer: MockRestServiceServer
    private lateinit var flexBucketUploaderMockServer: MockRestServiceServer

    @BeforeEach
    fun init() {
        pdfGenMockServer = MockRestServiceServer.createServer(simpleRestTemplate)
        dokarkivMockServer = MockRestServiceServer.createServer(dokarkivRestTemplate)
        oppgaveMockServer = MockRestServiceServer.createServer(oppgaveRestTemplate)
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
                hentPerson = HentPerson(navn = listOf(Navn(fornavn = "For", mellomnavn = "Midt", etternavn = "Efter"))),
                hentIdenter = HentIdenter(listOf(PdlIdent(gruppe = AKTORID, "aktorid123")))
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
            blobId = "1234",
            datoForUtgift = LocalDate.of(2020, 12, 24),
            belop = 10000,
            typeUtgift = Utgiftstype.OFFENTLIG_TRANSPORT
        )

        val soknad = Reisetilskudd(
            status = ReisetilskuddStatus.SENDT,
            fnr = "12345600000",
            id = UUID.randomUUID().toString(),
            sporsmal = listOf(sporsmalMedKvittering(kvittering)),
            fom = LocalDate.of(2020, 3, 12),
            tom = LocalDate.of(2020, 3, 20),
            sendt = LocalDateTime.of(2020, 3, 20, 16, 0, 0).toInstant(ZoneOffset.UTC)
        )

        pdfGenMockServer.expect(
            once(),
            requestTo(URI("http://flex-reisetilskudd-pdfgen/api/v1/genpdf/reisetilskudd/reisetilskudd"))
        )
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.navn", `is`("For Midt Efter")))
            .andExpect(jsonPath("$.reisetilskuddId", `is`(soknad.id)))
            .andExpect(jsonPath("$.sendt", `is`("2020-03-20T16:00:00Z")))
            .andExpect(jsonPath("$.kvitteringer[0].b64data", `is`("3q2+7w==")))
            .andExpect(jsonPath("$.kvitteringer[0].typeUtgift", `is`("Offentlig transport")))
            .andExpect(jsonPath("$.sum", `is`(10000)))
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

        val oppgaveResponse = OppgaveResponse(id = 1234)
        oppgaveMockServer.expect(
            once(),
            requestTo(URI("http://oppgave/api/v1/oppgaver"))
        )
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.behandlingstema", `is`("ab0237")))
            .andExpect(jsonPath("$.beskrivelse", `is`("Søknad om reisetilskudd for perioden 12.03.2020 - 20.03.2020")))

            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(oppgaveResponse.serialisertTilString())

            )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.id, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingRepository.findInnsendingByReisetilskuddId(soknad.id)!!

        assertThat(innsending.reisetilskuddId, `is`(soknad.id))
        assertThat(innsending.fnr, `is`(soknad.fnr))
        assertThat(innsending.journalpostId, `is`(jpostresponse.journalpostId))
        assertThat(innsending.oppgaveId, `is`(1234))
        assertThat(Instant.now().toEpochMilli() - innsending.opprettet.toEpochMilli(), lessThan(2000))

        pdfGenMockServer.verify()
        flexBucketUploaderMockServer.verify()
        dokarkivMockServer.verify()
        oppgaveMockServer.verify()
    }

    @Test
    @DirtiesContext
    fun `SENDT søknad hvor oppgave opprettelse feiler lagres i databasen med journalpost men uten oppgave id`() {
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
                hentPerson = HentPerson(navn = listOf(Navn(fornavn = "For", mellomnavn = "Midt", etternavn = "Efter"))),
                hentIdenter = HentIdenter(listOf(PdlIdent(gruppe = AKTORID, "aktorid123")))
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
            blobId = "1234",
            datoForUtgift = LocalDate.of(2020, 12, 24),
            belop = 10000,
            typeUtgift = Utgiftstype.ANNET
        )

        val soknad = Reisetilskudd(
            status = ReisetilskuddStatus.SENDT,
            fnr = "12345600000",
            id = UUID.randomUUID().toString(),
            sporsmal = listOf(sporsmalMedKvittering(kvittering)),
            fom = LocalDate.of(2020, 3, 12),
            tom = LocalDate.of(2020, 3, 20),
            sendt = LocalDateTime.of(2020, 3, 20, 16, 0, 0).toInstant(ZoneOffset.UTC)
        )

        pdfGenMockServer.expect(
            once(),
            requestTo(URI("http://flex-reisetilskudd-pdfgen/api/v1/genpdf/reisetilskudd/reisetilskudd"))
        )
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.navn", `is`("For Midt Efter")))
            .andExpect(jsonPath("$.reisetilskuddId", `is`(soknad.id)))
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

        oppgaveMockServer.expect(
            manyTimes(),
            requestTo(URI("http://oppgave/api/v1/oppgaver"))
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.id, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until {
            innsendingRepository.findInnsendingByReisetilskuddId(soknad.id) != null
        }

        val innsending = innsendingRepository.findInnsendingByReisetilskuddId(soknad.id)!!

        assertThat(innsending.reisetilskuddId, `is`(soknad.id))
        assertThat(innsending.fnr, `is`(soknad.fnr))
        assertThat(innsending.journalpostId, `is`(jpostresponse.journalpostId))
        assertThat(innsending.oppgaveId, `is`(nullValue()))
        assertThat(Instant.now().toEpochMilli() - innsending.opprettet.toEpochMilli(), lessThan(2000))

        pdfGenMockServer.verify()
        flexBucketUploaderMockServer.verify()
        dokarkivMockServer.verify()
        oppgaveMockServer.verify()
    }

    @Test
    fun `SENDT søknad hvor oppgave opprettelse tidligere feilet prosesseres igjen og oppdateres i databasen`() {
        reisetilskuddConsumer.meldinger = 0

        val eksisterendeInnsendingUtenOppgaveId = innsendingRepository.save(
            Innsending(
                reisetilskuddId = UUID.randomUUID().toString(),
                opprettet = Instant.now(),
                journalpostId = "jpost12",
                fnr = "12345600000",
            )
        )

        val getPersonResponse = GetPersonResponse(
            errors = emptyList(),
            data = ResponseData(
                hentPerson = HentPerson(navn = listOf(Navn(fornavn = "For", mellomnavn = "Midt", etternavn = "Efter"))),
                hentIdenter = HentIdenter(listOf(PdlIdent(gruppe = AKTORID, "aktorid123")))
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
            blobId = "1234",
            datoForUtgift = LocalDate.of(2020, 12, 24),
            belop = 10000,
            typeUtgift = Utgiftstype.ANNET
        )

        val soknad = Reisetilskudd(
            status = ReisetilskuddStatus.SENDT,
            fnr = "12345600000",
            id = eksisterendeInnsendingUtenOppgaveId.reisetilskuddId,
            sporsmal = listOf(sporsmalMedKvittering(kvittering)),
            fom = LocalDate.of(2020, 3, 12),
            tom = LocalDate.of(2020, 3, 20),
            sendt = LocalDateTime.of(2020, 3, 20, 16, 0, 0).toInstant(ZoneOffset.UTC)
        )

        val oppgaveResponse = OppgaveResponse(id = 1234)
        oppgaveMockServer.expect(
            once(),
            requestTo(URI("http://oppgave/api/v1/oppgaver"))
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(oppgaveResponse.serialisertTilString())
            )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.id, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingRepository.findInnsendingByReisetilskuddId(soknad.id)!!

        assertThat(innsending.reisetilskuddId, `is`(soknad.id))
        assertThat(innsending.fnr, `is`(soknad.fnr))
        assertThat(innsending.journalpostId, `is`(eksisterendeInnsendingUtenOppgaveId.journalpostId))
        assertThat(innsending.oppgaveId, `is`(1234))
        assertThat(Instant.now().toEpochMilli() - innsending.opprettet.toEpochMilli(), lessThan(2000))

        dokarkivMockServer.verify()
        oppgaveMockServer.verify()
    }

    @Test
    fun `FREMTIDIG prosesseres ikke og lagres ikke `() {
        reisetilskuddConsumer.meldinger = 0

        val soknad = Reisetilskudd(
            status = ReisetilskuddStatus.FREMTIDIG,
            fnr = "12345600000",
            id = UUID.randomUUID().toString(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
        )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.id, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingRepository.findInnsendingByReisetilskuddId(soknad.id)

        assertThat(innsending, nullValue())
    }

    @Test
    fun `allerede SENDT søknad prosesseres ikke og lagres i databasen`() {
        reisetilskuddConsumer.meldinger = 0

        val eksisterendeInnsending = Innsending(
            reisetilskuddId = UUID.randomUUID().toString(),
            opprettet = Instant.now(),
            journalpostId = "jpost12",
            oppgaveId = 1,
            fnr = "12345600000",
        )

        innsendingRepository.save(eksisterendeInnsending)

        val soknad = Reisetilskudd(
            status = ReisetilskuddStatus.SENDT,
            fnr = eksisterendeInnsending.fnr,
            id = eksisterendeInnsending.reisetilskuddId,
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            sendt = LocalDateTime.of(2020, 3, 20, 16, 0, 0).toInstant(ZoneOffset.UTC)
        )

        producer.send(ProducerRecord(FLEX_APEN_REISETILSKUDD_TOPIC, soknad.id, soknad.serialisertTilString())).get()

        await().atMost(3, TimeUnit.SECONDS).until { reisetilskuddConsumer.meldinger == 1 }

        val innsending = innsendingRepository.findInnsendingByReisetilskuddId(soknad.id)!!

        assertThat(innsending, `is`(eksisterendeInnsending.copy(id = innsending.id)))
    }
}

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)

private fun sporsmalMedKvittering(kvittering: Kvittering) =
    Sporsmal(
        tag = Tag.KVITTERINGER,
        svartype = Svartype.KVITTERING,
        svar = listOf(Svar(kvittering = kvittering))
    )
